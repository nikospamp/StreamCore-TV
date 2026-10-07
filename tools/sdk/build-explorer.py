"""Compile the Kotlin source index and reviewed SDK bindings into offline HTML.

python tools/sdk/build-explorer.py          # validate and generate
python tools/sdk/build-explorer.py --check  # validate without writing

The Kotlin PSI tree supplies declarations and conservative source relationships.
Reviewed bindings remain explicit; changed reviewed source is never auto-accepted.
"""
import argparse
import base64
import copy
import gzip
import hashlib
import json
from pathlib import Path
import re
import sys

from source_inventory import ROOT, git, source_inventory

CONTENT = ROOT / 'docs/tracked/sdk/explorer-content'
OUTPUT = ROOT / 'build/sdk-docs/streamcore-sdk-explorer.html'
CONTEXTS = [
    {'id': 'tmdb-android', 'provider': 'tmdb', 'platform': 'android', 'label': 'TMDB · Android app', 'currentAppWiring': True},
    {'id': 'tmdb-web', 'provider': 'tmdb', 'platform': 'web', 'label': 'TMDB · Browser app', 'currentAppWiring': True},
    {'id': 'clientb-android', 'provider': 'clientb', 'platform': 'android', 'label': 'ClientB · Android app', 'currentAppWiring': True},
    {'id': 'clientb-web', 'provider': 'clientb', 'platform': 'web', 'label': 'ClientB · Browser SDK entry point', 'currentAppWiring': False},
]
JOURNEYS = [
    {'id': 'construction', 'label': 'SDK construction', 'description': 'From application composition to SDK factories, dependencies, and shared services.'},
    {'id': 'auth', 'label': 'Authentication', 'description': 'Restore, sign in, create account providers, and sign out.'},
    {'id': 'profiles', 'label': 'Profiles & authorization', 'description': 'Profile selection, PIN authorization, avatars, and profile mutations.'},
    {'id': 'home', 'label': 'Home catalogue', 'description': 'Profile guards and provider-backed home collections.'},
    {'id': 'details', 'label': 'Content details', 'description': 'Details requests, policy checks, provider calls, and mapping.'},
    {'id': 'search', 'label': 'Search & history', 'description': 'Query handling, provider search, and local recent-search persistence.'},
    {'id': 'library', 'label': 'Library', 'description': 'Membership, observation, and continue watching through shared progress.'},
    {'id': 'playback', 'label': 'Playback & progress', 'description': 'Source resolution, captured authorization, recorders, and local progress.'},
    {'id': 'storage', 'label': 'Persistence', 'description': 'Backend/account/profile namespaces and platform storage.'},
    {'id': 'lifecycle', 'label': 'Lifetime & disposal', 'description': 'Owned sessions and resources, deferred creation, and cleanup.'},
]
TRACE_KINDS = {
    'calls': ['call'], 'constructs': ['call'], 'produces': ['value'],
    'passes': ['value'], 'aliases': ['value'], 'binds': ['call', 'value'],
    'captures': ['value'], 'deferred': ['call'], 'owns': [], 'closes': [],
}
NODE_KINDS = {'callable', 'parameter', 'value', 'property', 'contract', 'external'}


def resolve_anchor(reference, files):
    """Resolve an authored exact excerpt, allowing indentation/line-ending changes."""
    path = reference.get('file')
    if path not in files or files[path].get('binary'):
        raise ValueError(f'Missing source file: {path}')
    anchor = reference.get('anchor', '').strip()
    if not anchor:
        raise ValueError(f'Empty source anchor: {path}')
    pattern = r'\s+'.join(re.escape(part) for part in re.split(r'\s+', anchor))
    matches = list(re.finditer(pattern, files[path]['source']))
    if len(matches) != 1:
        raise ValueError(f'Anchor must match once ({len(matches)} matches) in {path}: {anchor[:140]}')
    match = matches[0]
    source = files[path]['source']
    return {'file': path, 'startLine': source.count('\n', 0, match.start()) + 1,
            'endLine': source.count('\n', 0, match.end() - 1) + 1}


def validate_contexts(values, valid, label):
    if not isinstance(values, list) or not values or len(values) != len(set(values)) or not set(values) <= valid:
        raise ValueError(f'Invalid contexts for {label}: {values}')


def public_operations(files):
    """The deliberately narrow coverage gate indexes public service declarations."""
    result = set()
    for path, file in files.items():
        if not path.startswith('sdk/api/src/commonMain/'):
            continue
        name = Path(path).name
        if not (name.endswith('Service.kt') or name in {'StreamCoreClient.kt', 'PlaybackProgressRecorder.kt'}):
            continue
        source = re.sub(r'/\*.*?\*/|//[^\n]*', '', file['source'], flags=re.S)
        for operation in re.findall(r'\bfun\s+(\w+)\s*\(', source):
            result.add((path, operation))
    return result


def compile_graph(fragments, inventory, reviewed_hashes):
    files = {file['path']: file for file in inventory}
    nodes, edges, coverage = [], [], []
    for fragment in fragments:
        nodes.extend(copy.deepcopy(fragment.get('nodes', [])))
        edges.extend(copy.deepcopy(fragment.get('edges', [])))
        coverage.extend(copy.deepcopy(fragment.get('coverage', [])))
    contexts = {context['id'] for context in CONTEXTS}
    journeys = {journey['id'] for journey in JOURNEYS}
    aliases = {}
    canonical = set()
    mapped_files = set()
    for node in nodes:
        key = node['id']
        if key in aliases:
            raise ValueError(f'Duplicate node: {key}')
        if node.get('kind') not in NODE_KINDS:
            raise ValueError(f'Unknown node kind: {key}')
        validate_contexts(node.get('contextIds'), contexts, key)
        if not node.get('journeys') or not set(node['journeys']) <= journeys:
            raise ValueError(f'Unknown journey for {key}')
        if not isinstance(node.get('column'), int) or not 0 <= node['column'] <= 20:
            raise ValueError(f'Invalid layout column: {key}')
        if not node.get('symbol') or not node.get('label'):
            raise ValueError(f'Missing symbol/label: {key}')
        if node.get('external') or node['kind'] == 'external':
            node['external'] = True
            node['id'] = 'external:' + node['symbol']
        else:
            node.update(resolve_anchor(node, files))
            mapped_files.add(node['file'])
            node['module'] = files[node['file']]['module']
            node['id'] = node['file'] + '#' + node['symbol']
        if node['id'] in canonical:
            raise ValueError(f'Duplicate scoped declaration: {node["id"]}')
        canonical.add(node['id'])
        node['key'] = key
        aliases[key] = node
    for node in nodes:
        if node.get('ownerId'):
            owner = aliases.get(node['ownerId'])
            if not owner or not set(node['contextIds']) <= set(owner['contextIds']):
                raise ValueError(f'Missing/incompatible owner for {node["key"]}')
            node['ownerId'] = owner['id']
    edge_ids = set()
    for edge in edges:
        key = edge['id']
        if key in edge_ids:
            raise ValueError(f'Duplicate edge: {key}')
        edge_ids.add(key)
        if edge.get('kind') not in TRACE_KINDS:
            raise ValueError(f'Unknown edge kind: {key}')
        source, target = aliases.get(edge['from']), aliases.get(edge['to'])
        if not source or not target:
            raise ValueError(f'Dangling edge {key}: {edge["from"]} -> {edge["to"]}')
        validate_contexts(edge.get('contextIds'), contexts, key)
        if not set(edge['contextIds']) <= set(source['contextIds']) & set(target['contextIds']):
            raise ValueError(f'Context crosses provider/platform boundary: {key}')
        edge['evidence'] = resolve_anchor(edge.get('evidence', {}), files)
        mapped_files.add(edge['evidence']['file'])
        if 'argumentIndex' in edge and (type(edge['argumentIndex']) is not int or edge['argumentIndex'] < 1):
            raise ValueError(f'Argument positions are one-based: {key}')
        if edge.get('argumentName'):
            argument = edge['argumentName']
            if not re.fullmatch(r'[A-Za-z_]\w*', argument):
                raise ValueError(f'Invalid argument name: {key}')
            # The reviewed source excerpt supplies call-site semantics. Check the
            # destination declaration as well, without pretending to parse Kotlin.
            if edge['kind'] == 'passes' and target['kind'] in {'parameter', 'property'}:
                if not re.search(r'\b' + re.escape(argument) + r'\b', target.get('anchor', '')):
                    raise ValueError(f'Argument does not name target declaration: {key}')
        if 'argumentExpression' in edge:
            expression = edge['argumentExpression']
            evidence = edge['evidence']
            excerpt = '\n'.join(files[evidence['file']]['source'].splitlines()[evidence['startLine'] - 1:evidence['endLine']])
            expression_pattern = r'\s+'.join(re.escape(part) for part in str(expression).strip().split())
            assignment = r'\b' + re.escape(edge.get('argumentName', '')) + r'\s*=\s*' + expression_pattern + r'\s*(?=[,\n)]|$)'
            if not expression or not edge.get('argumentName') or not re.search(assignment, excerpt):
                raise ValueError(f'Argument expression lacks matching assignment evidence: {key}')
        edge['from'], edge['to'] = source['id'], target['id']
        edge.setdefault('traces', TRACE_KINDS[edge['kind']])
        if not isinstance(edge['traces'], list) or not set(edge['traces']) <= {'call', 'value'}:
            raise ValueError(f'Invalid trace policy: {key}')
    covered = set()
    for item in coverage:
        operation = (item['file'], item['operation'])
        if operation in covered:
            raise ValueError(f'Duplicate coverage: {operation}')
        covered.add(operation)
        mapped_files.add(item['file'])
        if item.get('exclusion'):
            if not isinstance(item['exclusion'], str) or len(item['exclusion'].strip()) < 12:
                raise ValueError(f'Explain coverage exclusion: {operation}')
        else:
            node = aliases.get(item.get('nodeId'))
            if not node or item.get('journey') not in node['journeys'] or node.get('file') != item['file']:
                raise ValueError(f'Invalid operation coverage: {operation}')
            if not re.search(r'\bfun\s+' + re.escape(item['operation']) + r'\s*\(', node.get('anchor', '')):
                raise ValueError(f'Coverage must reference the named operation declaration: {operation}')
            item['nodeId'] = node['id']
    expected = public_operations(files)
    if expected != covered:
        missing = sorted(expected - covered)
        extra = sorted(covered - expected)
        raise ValueError(f'Public operation coverage differs. Missing: {missing}; extra: {extra}')
    for path in sorted(mapped_files):
        if path not in files:
            raise ValueError(f'Missing mapped file: {path}')
        actual = files[path]['hash']
        if reviewed_hashes.get(path) != actual:
            raise ValueError(f'Review required: {path}\nSource changed or is not reviewed. Inspect its nodes/edges and update reviewed-sources.json only after review.\nCurrent SHA-256: {actual}')
    obsolete = set(reviewed_hashes) - mapped_files
    if obsolete:
        raise ValueError(f'Unused reviewed sources: {sorted(obsolete)}')
    for node in nodes:
        node.pop('anchor', None)
    return {'nodes': nodes, 'edges': edges, 'coverage': coverage}


def build():
    fragment_paths = [CONTENT / 'construction.json', CONTENT / 'services.json']
    fragments = [json.loads(path.read_text(encoding='utf-8-sig')) for path in fragment_paths]
    reviewed = json.loads((CONTENT / 'reviewed-sources.json').read_text(encoding='utf-8-sig'))
    inventory, source_digest = source_inventory()
    graph = compile_graph(fragments, inventory, reviewed)
    from kotlin_psi import parse_sources
    from kotlin_graph import augment_graph
    parsed = parse_sources(inventory, root=ROOT)
    syntax_errors = [(file['path'], error) for file in parsed.get('files', []) for error in file.get('errors', [])]
    if syntax_errors:
        raise ValueError(f'Kotlin source index found syntax errors: {syntax_errors[:5]}')
    graph = augment_graph(graph, inventory, parsed, CONTEXTS, JOURNEYS)
    if graph.get('index', {}).get('missingParameters'):
        raise ValueError('Source index omitted formal parameters')
    identifiers = {node['id'] for node in graph['nodes']}
    if len(identifiers) != len(graph['nodes']):
        raise ValueError('Source index produced duplicate node identities')
    for node in graph['nodes']:
        if node.get('ownerId') and node['ownerId'] not in identifiers:
            raise ValueError(f'Source index has a missing owner: {node["id"]}')
    for edge in graph['edges']:
        if edge['from'] not in identifiers or edge['to'] not in identifiers:
            raise ValueError(f'Source index produced a dangling edge: {edge["id"]}')
    # Embed all source for browsing, including files outside the reviewed graph.
    data = {'schemaVersion': 1, 'contexts': CONTEXTS, 'journeys': JOURNEYS, **graph, 'files': inventory}
    digest = hashlib.sha256(source_digest.encode())
    digest.update((ROOT / 'tools/sdk/KotlinSourceIndex.java').read_bytes())
    for path in sorted(CONTENT.iterdir()):
        if path.is_file():
            digest.update(path.name.encode() + b'\0' + path.read_bytes())
    data['snapshot'] = {'commit': git('rev-parse', '--short', 'HEAD'),
                        'dirty': bool(git('status', '--porcelain', '--', 'sdk', 'app/src', 'webApp/src', 'feature', 'playback')),
                        'fingerprint': digest.hexdigest()[:16]}
    serialized = json.dumps(data, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    packed = base64.b64encode(gzip.compress(serialized, compresslevel=6, mtime=0)).decode('ascii')
    result = (CONTENT / 'shell.html').read_text(encoding='utf-8')
    for placeholder in ('/* EXPLORER_CSS */', '/* EXPLORER_JS */', '<!-- EXPLORER_DATA -->'):
        if result.count(placeholder) != 1:
            raise ValueError(f'HTML template must contain exactly one {placeholder}')
    result = result.replace('/* EXPLORER_CSS */', (CONTENT / 'explorer.css').read_text(encoding='utf-8'))
    scripts = '\n'.join((CONTENT / name).read_text(encoding='utf-8') for name in ('shapes.js', 'tree.js', 'explorer.js'))
    result = result.replace('/* EXPLORER_JS */', scripts)
    result = result.replace('<!-- EXPLORER_DATA -->', packed)
    return result, data


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='Validate sources and check the generated file without writing')
    args = parser.parse_args()
    try:
        result, data = build()
        if args.check:
            if not OUTPUT.exists() or OUTPUT.read_text(encoding='utf-8') != result:
                raise ValueError('Explorer snapshot is stale. Run python tools/sdk/build-explorer.py')
        else:
            OUTPUT.parent.mkdir(parents=True, exist_ok=True)
            OUTPUT.write_text(result, encoding='utf-8', newline='\n')
        print(f'{"Checked" if args.check else "Built"} SDK explorer: {len(data["nodes"])} nodes, {len(data["edges"])} relationships, '
              f'{len(data["coverage"])} public operations, {len(data["files"])} source entries; {data["snapshot"]["fingerprint"]}')
    except (ValueError, KeyError, OSError, RuntimeError, json.JSONDecodeError) as error:
        print(f'SDK explorer: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
