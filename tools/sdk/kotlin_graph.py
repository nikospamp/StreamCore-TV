"""Conservatively augment the reviewed SDK map with Kotlin PSI source relationships.

PSI supplies syntax and lexical ranges, not Kotlin compiler type resolution. This
module resolves only a unique compatible declaration and labels every remaining
call explicitly. Reviewed provider/interface bindings remain the authority for
dynamic dispatch. No Kotlin source is executed.
"""
from __future__ import annotations

import bisect
import copy
import hashlib
import re
from collections import defaultdict, deque


CALLABLE_KINDS = {'function', 'constructor', 'class', 'object', 'enum', 'lambda', 'accessor'}
TYPE_KINDS = {'class', 'interface', 'object', 'enum', 'typeAlias'}
VALUE_KINDS = {'parameter', 'property', 'local', 'destructuringEntry', 'enumEntry'}
EXCLUDED_KINDS = {'typeParameter', 'destructuring', 'initializer'}
EXPR_CHILD_KEYS = ('receiver', 'selector', 'callee', 'left', 'right', 'value', 'condition',
                   'thenBranch', 'elseBranch', 'subject', 'expression', 'body', 'finally', 'guard')


def _simple_type(value):
    if not value:
        return ''
    value = re.sub(r'\b(?:suspend|out|in)\s+', '', str(value)).strip().rstrip('?')
    if '->' in value:
        return 'Function'
    return value.split('<', 1)[0].rsplit('.', 1)[-1].strip()


def _name(value):
    if isinstance(value, dict):
        return value.get('name') or value.get('text', '').strip('`')
    return str(value or '').strip('`')


def _children(expr):
    """Yield structural children once even when exporter adds convenience fields."""
    seen = set()
    for key in EXPR_CHILD_KEYS:
        value = expr.get(key)
        if isinstance(value, dict) and (value.get('kind') or value.get('id')):
            identity = value.get('id') or id(value)
            if identity not in seen:
                seen.add(identity)
                yield value
    for key in ('children', 'entries', 'conditions', 'statements', 'parts', 'catches'):
        for value in expr.get(key, []) or []:
            if isinstance(value, dict):
                identity = value.get('id') or id(value)
                if not value.get('kind') and not value.get('id'):
                    yield from _children(value)
                elif identity not in seen:
                    seen.add(identity)
                    yield value
    for argument in expr.get('arguments', []) or []:
        value = argument.get('expression', argument.get('value')) if isinstance(argument, dict) else None
        if isinstance(value, dict):
            identity = value.get('id') or id(value)
            if identity not in seen:
                seen.add(identity)
                yield value


class SourceGraph:
    def __init__(self, graph, inventory, parsed_ir, contexts, journeys):
        self.graph = copy.deepcopy(graph)
        self.graph.setdefault('nodes', [])
        self.graph.setdefault('edges', [])
        self.graph.setdefault('coverage', [])
        self.nodes = {node['id']: node for node in self.graph['nodes']}
        self.keys = {node['key']: node for node in self.graph['nodes']}
        self.inventory = {item['path']: item for item in inventory}
        self.contexts = [item['id'] if isinstance(item, dict) else item for item in contexts]
        self.journeys = [item['id'] if isinstance(item, dict) else item for item in journeys]
        self.files = {}
        self.declarations = {}
        self.formal_parameter_ids = set()
        self.declaration_file = {}
        self.expressions = {}
        self.expression_file = {}
        self.decl_node = {}
        self.node_decl = {}
        self.by_name = defaultdict(list)
        self.by_owner = defaultdict(list)
        self.positions = {}
        self.expr_values = {}
        self.processing = set()
        self.processed_declarations = set()
        self.results = {}
        self.returns = defaultdict(set)
        self.calls = []
        self.callback_calls = []
        self.implicit_parameters = {}
        self.aliases = set()
        self.incoming_aliases = defaultdict(set)
        self.alias_evidence = defaultdict(list)
        self.projections = {}
        self.projection_parts = {}
        self.projection_evidence = {}
        self.object_slots = defaultdict(dict)
        self.constructor_inputs = {}
        self.edge_keys = set()
        self.unresolved = []
        self.exclusions = []
        self.original_nodes = len(self.nodes)
        self.original_edges = len(self.graph['edges'])
        self.curated_by_file = defaultdict(list)
        for node in self.graph['nodes']:
            if node.get('file'):
                self.curated_by_file[node['file']].append(node)
        self._curated_call_edges = [edge for edge in self.graph['edges']
                                    if edge['kind'] in {'calls', 'constructs', 'binds'}]
        self._index(parsed_ir)

    def _index(self, parsed_ir):
        files = parsed_ir.get('files', []) if isinstance(parsed_ir, dict) else parsed_ir
        for source in files:
            path = source['path'].replace('\\', '/')
            if not path.startswith(('sdk/', 'app/src/', 'webApp/src/', 'feature/', 'playback/')):
                self.exclusions.append({'file': path, 'reason': 'Outside SDK production and current application composition; samples/build tooling are not live app origins'})
                continue
            if path not in self.inventory or self.inventory[path].get('binary'):
                self.exclusions.append({'file': path, 'reason': 'Not in the safe source inventory'})
                continue
            self.files[path] = source
            starts = [0]
            offset = 0
            for line in self.inventory[path].get('source', '').splitlines(keepends=True):
                offset += len(line) if source.get('offsetEncoding') == 'unicode-code-points' else len(line.encode('utf-16-le')) // 2
                starts.append(offset)
            self.positions[path] = starts
            for declaration in source.get('declarations', []):
                self.declarations[declaration['id']] = dict(declaration)
                self.declaration_file[declaration['id']] = path
                self.by_name[declaration.get('name', '')].append(declaration['id'])
                self.by_owner[declaration.get('ownerId')].append(declaration['id'])
            for root in source.get('expressions', []):
                self._index_expression(root, path)
        self.formal_parameter_ids = {
            parameter_id
            for declaration in self.declarations.values()
            if declaration.get('kind') in CALLABLE_KINDS | {'interface'}
            for parameter_id in declaration.get('parameterIds', [])
            if parameter_id in self.declarations and not self.declarations[parameter_id].get('typeOnly')
        }

    def _index_expression(self, expr, path):
        if not isinstance(expr, dict):
            return
        identity = expr.get('id')
        if identity:
            if identity in self.expressions:
                return
            self.expressions[identity] = expr
            self.expression_file[identity] = path
        for child in _children(expr):
            self._index_expression(child, path)

    def _evidence(self, path, item):
        if 'startLine' in item and 'start' not in item:
            return {'file': path, 'startLine': item['startLine'], 'endLine': item.get('endLine', item['startLine'])}
        starts = self.positions.get(path, [0])
        start = max(0, int(item.get('start', 0)))
        end = max(start + 1, int(item.get('end', start + 1)))
        return {'file': path, 'startLine': max(1, bisect.bisect_right(starts, start)),
                'endLine': max(1, bisect.bisect_right(starts, end - 1))}

    def _contexts(self, path):
        lower = path.lower()
        choices = set(self.contexts)
        if '/providers/tmdb/' in lower or lower.startswith('app/src/tmdb/'):
            choices &= {'tmdb-android', 'tmdb-web'}
        elif '/providers/clientb/' in lower or lower.startswith('app/src/clientb/'):
            choices &= {'clientb-android', 'clientb-web'}
        if '/androidmain/' in lower or lower.startswith('app/'):
            choices &= {item for item in self.contexts if item.endswith('-android')}
        if not lower.startswith('sdk/') and ('/src/main/' in lower or '/ui-tv/' in lower or '/ui-mobile/' in lower or '/ui-tablet/' in lower):
            choices &= {item for item in self.contexts if item.endswith('-android')}
        if '/wasmjsmain/' in lower:
            choices &= {item for item in self.contexts if item.endswith('-web')}
        if not lower.startswith('sdk/'):
            choices.discard('clientb-web')
        return [item for item in self.contexts if item in choices]

    def _journeys(self, path):
        lower = path.lower()
        result = set()
        for fragment, journey in [('auth', 'auth'), ('profile', 'profiles'), ('home', 'home'),
                                  ('detail', 'details'), ('search', 'search'), ('library', 'library'),
                                  ('playback', 'playback'), ('player', 'playback'), ('storage', 'storage')]:
            if fragment in lower:
                result.add(journey)
        if 'session' in lower:
            result.update(self.journeys)
        if 'configuration' in lower or '/network/' in lower or not result:
            result.add('construction')
        return [item for item in self.journeys if item in result] or self.journeys[:1]

    def _add_node(self, identity, key, path, item, kind, label, symbol, **extra):
        if identity in self.nodes:
            return identity
        node = {'id': identity, 'key': key, 'label': label, 'symbol': symbol, 'kind': kind,
                'contextIds': self._contexts(path), 'journeys': self._journeys(path),
                'column': 4, 'origin': 'source-index', 'resolution': 'resolved',
                'description': 'Indexed from Kotlin syntax; follow the source evidence for conditions and branches.',
                'module': self.inventory[path].get('module', ':sdk'), **self._evidence(path, item), **extra}
        if not node['contextIds']:
            self.exclusions.append({'file': path, 'symbol': symbol, 'reason': 'No active source context'})
        self.nodes[identity] = node
        self.keys[key] = node
        self.graph['nodes'].append(node)
        return identity

    def _edge(self, source, target, kind, path, evidence, *, contexts=None, identity=False, **extra):
        if not source or not target or source == target:
            return
        available = set(self.nodes[source]['contextIds']) & set(self.nodes[target]['contextIds'])
        if contexts is not None:
            available &= set(contexts)
        if not available:
            return
        normalized_contexts = tuple(item for item in self.contexts if item in available)
        resolved_evidence = self._evidence(path, evidence)
        key = (source, target, kind, normalized_contexts, extra.get('argumentName'),
               extra.get('upstream', True), extra.get('projection', False), path,
               evidence.get('start', resolved_evidence['startLine']), evidence.get('end', resolved_evidence['endLine']))
        if key in self.edge_keys:
            return
        self.edge_keys.add(key)
        digest = hashlib.sha256(repr(key).encode()).hexdigest()[:18]
        edge = {'id': 'source-edge-' + digest, 'from': source, 'to': target, 'kind': kind,
                'contextIds': list(normalized_contexts), 'traces': ['call'] if kind in {'calls', 'constructs'} else ['value'],
                'origin': 'source-index', 'evidence': resolved_evidence, **extra}
        self.graph['edges'].append(edge)
        if identity:
            self.aliases.add((source, target, normalized_contexts))
            self.incoming_aliases[target].add((source, normalized_contexts))
            self.alias_evidence[(source, target, normalized_contexts)].append((path, evidence))

    def _declaration_kind(self, declaration):
        kind = declaration.get('kind')
        if kind == 'interface' or kind == 'typeAlias':
            return 'contract'
        if kind in CALLABLE_KINDS:
            return 'callable'
        if kind == 'parameter':
            return 'parameter' if declaration['id'] in self.formal_parameter_ids else 'value'
        if kind == 'property':
            return 'property'
        return 'value'

    def _match_curated(self, declaration, path):
        name = declaration.get('name', '')
        kind = declaration.get('kind')
        evidence = self._evidence(path, declaration)
        header_end = declaration.get('bodyStart', declaration.get('end', 0))
        header_line = self._evidence(path, {'start': header_end, 'end': header_end + 1})['startLine']
        candidates = []
        for node in self.curated_by_file[path]:
            if node.get('origin') == 'source-index' or node.get('file') != path:
                continue
            compatible = node['kind'] in ({'callable', 'contract'} if kind in CALLABLE_KINDS | TYPE_KINDS
                                          else {'parameter', 'property', 'value'})
            if not compatible:
                continue
            symbol = str(node.get('symbol', '')).split('[', 1)[0]
            has_name = symbol.rsplit('.', 1)[-1] == name or node.get('label') == name
            if not has_name:
                continue
            if not (evidence['startLine'] <= node.get('startLine', 0) <= header_line):
                continue
            score = abs(node.get('startLine', 0) - evidence['startLine'])
            if kind not in CALLABLE_KINDS | TYPE_KINDS:
                score += abs(node.get('endLine', 0) - evidence['endLine'])
            candidates.append((score, node['id']))
        candidates.sort()
        if candidates and (len(candidates) == 1 or candidates[0][0] < candidates[1][0]):
            return candidates[0][1]
        return None

    def declaration_node(self, decl_id):
        if not decl_id or decl_id not in self.declarations:
            return None
        if decl_id in self.decl_node:
            return self.decl_node[decl_id]
        declaration = self.declarations[decl_id]
        kind = declaration.get('kind', '')
        if kind in EXCLUDED_KINDS:
            return None
        path = self.declaration_file[decl_id]
        owner = self.declarations.get(declaration.get('ownerId'))
        owner_node = self.declaration_node(declaration.get('ownerId'))
        if kind == 'constructor' and declaration.get('primary') and owner_node:
            self.decl_node[decl_id] = owner_node
            return owner_node
        name = declaration.get('name') or kind
        symbol = declaration.get('symbol') or name
        identity = self._match_curated(declaration, path)
        if identity in self.node_decl and self.node_decl[identity] != decl_id:
            identity = None
        if not identity:
            identity = 'source:' + decl_id
            if kind == 'parameter' and owner_node:
                key = self.nodes[owner_node]['key'] + '.param.' + name
            else:
                key = 'index.' + decl_id
            if key in self.keys:
                key += '.' + str(declaration.get('start', 0))
            identity = self._add_node(identity, key, path, declaration, self._declaration_kind(declaration),
                                      name, symbol)
        self.decl_node[decl_id] = identity
        self.node_decl[identity] = decl_id
        node = self.nodes[identity]
        node.update(declarationId=decl_id, declarationKind=kind)
        for field in ('typeName', 'returnType', 'receiverType', 'mutable', 'propertyParameter', 'hasDefaultValue'):
            if declaration.get(field) is not None:
                node[field] = declaration[field]
        if declaration.get('defaultValue'):
            node['defaultValue'] = declaration['defaultValue']
        if kind in TYPE_KINDS:
            node.setdefault('typeName', name)
            node.setdefault('returnType', name)
            node['isConstructor'] = kind in {'class', 'enum'}
        if kind == 'constructor':
            node['isConstructor'] = True
        if kind == 'parameter':
            node['isParameter'] = decl_id in self.formal_parameter_ids
            node['parameterRole'] = 'signature' if node['isParameter'] else 'type-label' if declaration.get('typeOnly') else 'local-binding'
            if not node['isParameter'] and node['kind'] == 'parameter':
                node['kind'] = 'value'
        if owner_node:
            node.setdefault('ownerId', owner_node)
            node.setdefault('ownerName', owner.get('name', '') if owner else '')
            node.setdefault('ownerKind', 'class' if owner and owner.get('kind') in TYPE_KINDS | {'constructor'} else 'function')
        if declaration.get('receiverType') and kind == 'function':
            receiver = identity + '/receiver'
            self._add_node(receiver, node['key'] + '.receiver', path, declaration, 'parameter',
                           'this', symbol + '.<receiver>', typeName=declaration['receiverType'],
                           ownerId=identity, ownerName=name, ownerKind='function', isReceiver=True)
            declaration['_receiverNode'] = receiver
        return identity

    def _owner_chain(self, owner_id):
        seen = set()
        while owner_id and owner_id not in seen:
            seen.add(owner_id)
            yield owner_id
            owner_id = self.declarations.get(owner_id, {}).get('ownerId')

    def _function_owner(self, owner_id, include_lambda=True):
        for identity in self._owner_chain(owner_id):
            kind = self.declarations[identity].get('kind')
            if kind in {'function', 'constructor', 'accessor'} or (include_lambda and kind == 'lambda'):
                return identity
            if kind in TYPE_KINDS:
                return identity
        return None

    def _reference(self, expr, path):
        name = _name(expr.get('name') or expr.get('text'))
        owner_id = expr.get('ownerId')
        offset = expr.get('start', 0)
        for owner in self._owner_chain(owner_id):
            owner_declaration = self.declarations[owner]
            if name == 'it' and owner in self.implicit_parameters:
                return self.implicit_parameters[owner]
            if name == 'this':
                if owner_declaration.get('_receiverNode'):
                    return owner_declaration['_receiverNode']
                if owner_declaration.get('kind') in TYPE_KINDS:
                    return self.declaration_node(owner)
            candidates = []
            scope_members = list(self.by_owner.get(owner, []))
            if owner_declaration.get('primaryConstructorId'):
                scope_members += self.by_owner.get(owner_declaration['primaryConstructorId'], [])
            for identity in scope_members:
                declaration = self.declarations[identity]
                if declaration.get('typeOnly'):
                    continue
                if identity == expr.get('ownerId') and declaration.get('kind') in {'local', 'property'}:
                    continue
                if declaration.get('name') != name or declaration.get('kind') not in VALUE_KINDS | TYPE_KINDS:
                    continue
                start = declaration.get('lexicalScopeStart', owner_declaration.get('start', 0))
                end = declaration.get('lexicalScopeEnd', owner_declaration.get('end', 10**12))
                if not start <= offset <= end:
                    continue
                if declaration.get('kind') in {'local', 'destructuringEntry'} and declaration.get('start', 0) > offset:
                    continue
                candidates.append((end - start, -declaration.get('start', 0), identity))
            if candidates:
                candidates.sort()
                target = candidates[0][2]
                self.process_declaration(target)
                return self.declaration_node(target)
        candidates = [identity for identity in self.by_name.get(name, [])
                      if self.declaration_file[identity] == path and not self.declarations[identity].get('ownerId')
                      and self.declarations[identity].get('kind') in VALUE_KINDS | TYPE_KINDS]
        if len(candidates) == 1:
            self.process_declaration(candidates[0])
            return self.declaration_node(candidates[0])
        type_declaration = self._type_declaration(name, path)
        if type_declaration:
            return self.declaration_node(type_declaration)
        # Receiver extensions use unqualified property reads, e.g. ProfileDto.toModel().
        for owner in self._owner_chain(owner_id):
            receiver = self.declarations[owner].get('_receiverNode')
            if receiver and self._property_declaration(self.nodes[receiver].get('typeName'), name, path):
                return self.project(receiver, name, path, expr)
        return self._expression_node(expr, path, 'reference', resolution='unresolved',
                                     description='Reference has no unique lexical binding in the indexed syntax. No source was guessed.')

    def _type_declaration(self, type_name, path):
        simple = _simple_type(type_name)
        aliases = [item['path'] for item in self.files[path].get('imports', []) if item.get('alias') == simple]
        if len(aliases) == 1:
            simple = aliases[0].rsplit('.', 1)[-1]
        candidates = [identity for identity in self.by_name.get(simple, [])
                      if self.declarations[identity].get('kind') in TYPE_KINDS
                      and self._visible_top_level(identity, path)
                      and (not aliases or self.files[self.declaration_file[identity]].get('package', '') + '.' + simple in aliases)]
        if len(candidates) <= 1:
            return candidates[0] if candidates else None
        package = self.files[path].get('package', '')
        same_package = [identity for identity in candidates
                        if self.files[self.declaration_file[identity]].get('package', '') == package]
        if len(same_package) == 1:
            return same_package[0]
        imports = self.files[path].get('imports', [])
        imported = []
        for identity in candidates:
            candidate_package = self.files[self.declaration_file[identity]].get('package', '')
            fqn = candidate_package + '.' + self.declarations[identity].get('name', '')
            if any(item.get('path') == fqn or item.get('allUnder') and item.get('path', '').rstrip('.*') == candidate_package for item in imports):
                imported.append(identity)
        return imported[0] if len(imported) == 1 else None

    def _visible_top_level(self, identity, path):
        declaration = self.declarations[identity]
        target_file = self.declaration_file[identity]
        if not set(self._contexts(path)) & set(self._contexts(target_file)):
            return False
        if target_file == path:
            return True
        if 'private' in declaration.get('modifiers', []):
            return False
        target_package = self.files[target_file].get('package', '')
        if target_package == self.files[path].get('package', ''):
            return True
        fqn = target_package + '.' + declaration.get('name', '')
        return any(item.get('path') == fqn or item.get('allUnder') and item.get('path', '').rstrip('.*') == target_package
                   for item in self.files[path].get('imports', []))

    def _property_declaration(self, type_name, member, path):
        owner = self._type_declaration(type_name, path)
        if not owner:
            return None
        candidates = [identity for identity in self.by_owner.get(owner, [])
                      if self.declarations[identity].get('name') == member
                      and self.declarations[identity].get('kind') in VALUE_KINDS]
        # Some exporters retain a distinct primary-constructor owner.
        for constructor in self.by_owner.get(owner, []):
            if self.declarations[constructor].get('kind') == 'constructor':
                candidates += [identity for identity in self.by_owner.get(constructor, [])
                               if self.declarations[identity].get('name') == member
                               and self.declarations[identity].get('propertyParameter')]
        return candidates[0] if len(candidates) == 1 else None

    def project(self, base, member, path, expr):
        if not base:
            return None
        parts = tuple(member.split('.'))
        key = (base, parts)
        if key in self.projections:
            return self.projections[key]
        if len(parts) > 1:
            parent = self.project(base, '.'.join(parts[:-1]), path, expr)
            return self.project(parent, parts[-1], path, expr)
        parent_node = self.nodes[base]
        existing_key = parent_node['key'] + '.' + member
        existing = self.keys.get(existing_key)
        declaration_id = self._property_declaration(parent_node.get('typeName') or parent_node.get('returnType'), member, path)
        declaration = self.declarations.get(declaration_id, {})
        if existing:
            identity = existing['id']
        else:
            identity = base + '/field:' + member
            identity = self._add_node(identity, parent_node['key'] + '.field.' + member, path, expr, 'property',
                                      parent_node['label'] + '.' + member, parent_node['symbol'] + '.' + member,
                                      ownerId=base, ownerName=parent_node['label'], ownerKind='value',
                                      typeName=declaration.get('typeName', ''), fieldPath=member,
                                      resolution='resolved' if declaration_id else 'unresolved',
                                      contextIds=parent_node['contextIds'], journeys=parent_node['journeys'],
                                      description='Field projection. Its origins follow this field across object arguments and assignments, not sibling fields.')
        if declaration.get('typeName'):
            self.nodes[identity]['typeName'] = declaration['typeName']
        self.projections[key] = identity
        self.projection_parts[identity] = (base, parts)
        self.projection_evidence[identity] = (path, expr)
        self._edge(base, identity, 'reads', path, expr, projection=True, upstream=False, downstream=True,
                   description='Reads this field of the value; upstream provenance remains specific to this field.')
        if declaration_id:
            self.nodes[identity]['fieldDeclarationId'] = declaration_id
        slot = self.object_slots.get(base, {}).get(member)
        if slot:
            self._edge(slot, identity, 'aliases', path, expr, identity=True,
                       description='This constructor argument initializes the selected field.')
        return identity

    def _expression_node(self, expr, path, label=None, **extra):
        expr_id = expr.get('id') or f'{path}#expr:{expr.get("start",0)}:{expr.get("end",0)}:{expr.get("kind")}'
        identity = 'source:' + expr_id
        if identity in self.nodes:
            return identity
        owner = self._function_owner(expr.get('ownerId'))
        owner_node = self.declaration_node(owner)
        text = re.sub(r'\s+', ' ', expr.get('text', '')).strip()
        label = label or text[:95] or expr.get('kind', 'expression')
        symbol = (self.nodes[owner_node]['symbol'] + '.' if owner_node else '') + f'{expr.get("kind", "expression")}@{expr.get("start",0)}'
        kwargs = {'expressionKind': expr.get('kind'), 'expressionText': text}
        if owner_node:
            kwargs.update(ownerId=owner_node, ownerName=self.nodes[owner_node]['label'], ownerKind='function')
        kwargs.update(extra)
        return self._add_node(identity, 'index.' + expr_id, path, expr, 'value', label, symbol, **kwargs)

    def _result(self, declaration_id):
        if declaration_id in self.results:
            return self.results[declaration_id]
        declaration = self.declarations[declaration_id]
        owner = self.declaration_node(declaration_id)
        if not owner:
            return None
        node = self.nodes[owner]
        path = self.declaration_file[declaration_id]
        identity = owner + '/result'
        self._add_node(identity, node['key'] + '.result', path, declaration, 'value', node['label'] + ' result',
                       node['symbol'] + '.<result>', ownerId=owner, ownerName=node['label'], ownerKind='function',
                       typeName=declaration.get('returnType') or (declaration.get('name') if declaration.get('kind') in TYPE_KINDS else ''),
                       description='Values returned by this declaration. Call-site results remain separately indexed.')
        self.results[declaration_id] = identity
        self._edge(owner, identity, 'produces', path, declaration,
                   description='This function or constructor produces its return value.')
        return identity

    def _parameters(self, declaration):
        ids = declaration.get('parameterIds')
        if ids is not None:
            return [self.declarations[identity] for identity in ids if identity in self.declarations]
        return [self.declarations[identity] for identity in self.by_owner.get(declaration['id'], [])
                if self.declarations[identity].get('kind') == 'parameter']

    def _argument_bindings(self, declaration, arguments):
        parameters = self._parameters(declaration)
        bound = {}
        positional = 0
        for argument in arguments:
            name = argument.get('name')
            if name:
                choices = [parameter for parameter in parameters if parameter.get('name') == name]
                if len(choices) != 1:
                    return None
                parameter = choices[0]
            elif argument.get('trailingLambda') and parameters:
                parameter = parameters[-1]
            else:
                while positional < len(parameters) and parameters[positional]['id'] in bound:
                    positional += 1
                if positional >= len(parameters):
                    return None
                parameter = parameters[positional]
                positional += 1
            if parameter['id'] in bound and 'vararg' not in parameter.get('modifiers', []):
                return None
            bound[parameter['id']] = argument
        for parameter in parameters:
            if parameter['id'] not in bound and not parameter.get('hasDefaultValue') and 'vararg' not in parameter.get('modifiers', []):
                return None
        return bound

    def _resolve_call(self, expr, path, receiver, arguments):
        name = _name(expr.get('name') or expr.get('callee'))
        name = name.split('.')[-1]
        alias_imports = [item['path'] for item in self.files[path].get('imports', []) if item.get('alias') == name]
        lookup_name = alias_imports[0].rsplit('.', 1)[-1] if len(alias_imports) == 1 else name
        candidates = [identity for identity in self.by_name.get(lookup_name, [])
                      if self.declarations[identity].get('kind') in CALLABLE_KINDS | {'interface'}
                      and not (self.declarations[identity].get('kind') == 'constructor' and self.declarations[identity].get('primary'))]
        compatible = []
        receiver_type = _simple_type(self.nodes.get(receiver, {}).get('typeName') or self.nodes.get(receiver, {}).get('returnType'))
        receiver_declaration = self._type_declaration(receiver_type, path) if receiver_type else None
        if receiver_declaration:
            receiver_type = self.declarations[receiver_declaration].get('name', receiver_type)
        for identity in candidates:
            declaration = self.declarations[identity]
            if self._argument_bindings(declaration, arguments) is None:
                continue
            target = self.declaration_node(identity)
            if not set(self.nodes[target]['contextIds']) & set(self._contexts(path)):
                continue
            owner = self.declarations.get(declaration.get('ownerId'), {})
            owner_chain = set(self._owner_chain(expr.get('ownerId')))
            if owner.get('kind') in {'function', 'lambda', 'accessor', 'local', 'property'} and owner.get('id') not in owner_chain:
                continue
            if not owner and not self._visible_top_level(identity, path):
                continue
            if alias_imports and self.files[self.declaration_file[identity]].get('package', '') + '.' + declaration.get('name', '') not in alias_imports:
                continue
            if 'private' in declaration.get('modifiers', []) and owner.get('kind') in TYPE_KINDS and owner.get('id') not in owner_chain:
                continue
            if receiver:
                expected = _simple_type(declaration.get('receiverType')) or (owner.get('name') if owner.get('kind') in TYPE_KINDS else '')
                if owner.get('kind') == 'object' and ('companion' in owner.get('modifiers', []) or owner.get('name') == 'Companion'):
                    expected = self.declarations.get(owner.get('ownerId'), {}).get('name', expected)
                if not receiver_type or expected != receiver_type:
                    continue
            elif owner.get('kind') in TYPE_KINDS and declaration.get('kind') not in TYPE_KINDS:
                if owner['id'] not in set(self._owner_chain(expr.get('ownerId'))):
                    continue
            compatible.append(identity)
        if len(compatible) > 1:
            same_file = [identity for identity in compatible if self.declaration_file[identity] == path]
            if len(same_file) == 1:
                compatible = same_file
            else:
                imported = []
                imports = self.files[path].get('imports', [])
                package = self.files[path].get('package', '')
                for identity in compatible:
                    candidate_package = self.files[self.declaration_file[identity]].get('package', '')
                    if candidate_package == package or any(item.get('path', '').endswith('.' + name) or item.get('allUnder') and item.get('path', '').rstrip('.*') == candidate_package for item in imports):
                        imported.append(identity)
                if len(imported) == 1:
                    compatible = imported
        if len(compatible) == 1:
            return compatible[0], []
        # A reviewed direct call can supply a known interface/receiver binding.
        caller = self.declaration_node(self._function_owner(expr.get('ownerId')))
        reviewed = {edge['to'] for edge in self._curated_call_edges if edge['from'] == caller
                    and set(edge['contextIds']) & set(self._contexts(path))}
        supplied = [identity for identity in candidates if self.decl_node.get(identity) in reviewed
                    and self._argument_bindings(self.declarations[identity], arguments) is not None]
        if len(supplied) == 1:
            return supplied[0], []
        return None, compatible

    def _call(self, expr, path, receiver=None):
        name = _name(expr.get('name') or expr.get('callee')) or 'call'
        if receiver is None and isinstance(expr.get('receiver'), dict):
            receiver = self.evaluate(expr['receiver'], path)
        arguments = []
        for argument in expr.get('arguments', []) or []:
            item = dict(argument)
            value_expr = item.get('expression', item.get('value'))
            if isinstance(value_expr, dict) and value_expr.get('kind') == 'lambda':
                item['_node'] = self.declaration_node(value_expr.get('declarationId'))
                item['_lambda'] = value_expr
            else:
                item['_node'] = self.evaluate(value_expr, path) if isinstance(value_expr, dict) else None
            arguments.append(item)
        declaration_id, ambiguous = self._resolve_call(expr, path, receiver, arguments)
        result = self._expression_node(expr, path, name + '(…) result')
        caller_id = self._function_owner(expr.get('ownerId'))
        caller = self.declaration_node(caller_id)
        if declaration_id:
            declaration = self.declarations[declaration_id]
            target = self.declaration_node(declaration_id)
            self.nodes[result]['resolution'] = 'resolved'
            self.nodes[result]['calleeId'] = target
            self.nodes[result]['typeName'] = declaration.get('returnType') or (declaration.get('name') if declaration.get('kind') in TYPE_KINDS else '')
            self._edge(caller, target, 'constructs' if declaration.get('kind') in TYPE_KINDS | {'constructor'} else 'calls', path, expr)
            bindings = self._argument_bindings(declaration, arguments) or {}
            actuals = {}
            for index, parameter in enumerate(self._parameters(declaration), 1):
                parameter_node = self.declaration_node(parameter['id'])
                argument = bindings.get(parameter['id'])
                if argument and argument.get('_lambda'):
                    parameter_type = parameter.get('typeName') or ''
                    type_match = re.search(r'\((.*?)\)\s*->', parameter_type)
                    if type_match:
                        types = [part.strip().split(':', 1)[-1].strip() for part in type_match.group(1).split(',') if part.strip()]
                        self._prepare_lambda(argument['_lambda'], types, path)
                    self.process_declaration(argument['_lambda'].get('declarationId'))
                actual = argument.get('_node') if argument else None
                if not actual and parameter.get('defaultValueId'):
                    actual = self.evaluate(self.expressions.get(parameter['defaultValueId']), self.declaration_file[parameter['id']])
                if actual:
                    actuals[parameter_node] = actual
                    self._edge(actual, parameter_node, 'passes', path, expr, identity=True,
                               argumentName=parameter.get('name'), argumentIndex=index,
                               argumentExpression=argument.get('expression', {}).get('text', '') if argument else parameter.get('defaultValue', ''),
                               condition='Default value when the argument is omitted.' if not argument else '')
                    if declaration.get('kind') in TYPE_KINDS | {'constructor'}:
                        if parameter.get('propertyParameter'):
                            self.object_slots[result][parameter.get('name')] = actual
                        self._edge(actual, result, 'derives', path, expr,
                                   downstream=False,
                                   description='Constructor input contributes to the complete object. Field projections retain separate inputs.')
            if receiver and declaration.get('_receiverNode'):
                actuals[declaration['_receiverNode']] = receiver
                self._edge(receiver, declaration['_receiverNode'], 'passes', path, expr, identity=True,
                           description='Extension receiver is supplied by this call.')
            callable_result = self._result(declaration_id)
            if declaration.get('kind') in TYPE_KINDS | {'constructor'}:
                self.constructor_inputs[result] = actuals
            self._edge(callable_result, result, 'returns', path, expr, upstream=False, downstream=True,
                       description='This call receives the declaration result; caller-specific inputs are traced separately.')
            self._edge(target, result, 'produces', path, expr,
                       description='This call creates the returned value.')
            self.calls.append({'declaration': declaration_id, 'result': result, 'actuals': actuals,
                               'receiver': receiver, 'path': path, 'expression': expr})
        else:
            resolution = 'unresolved'
            imports = self.files[path].get('imports', [])
            if not ambiguous and any(item.get('path', '').rsplit('.', 1)[-1] == name for item in imports):
                resolution = 'external'
            self.nodes[result].update(resolution=resolution, candidateSymbols=[self.declarations[identity].get('symbol') for identity in ambiguous],
                                      description='External or unresolved call. The graph records its actual receiver/arguments without inventing a Kotlin overload binding.')
            boundary = result + '/callee'
            self._add_node(boundary, self.nodes[result]['key'] + '.callee', path, expr, 'external',
                           ('Ambiguous ' if ambiguous else 'Unresolved ') + name, self.nodes[result]['symbol'] + '.callee',
                           external=True, resolution=resolution,
                           description='The compiler syntax index has no unique source declaration for this call.',
                           candidateSymbols=[self.declarations[identity].get('symbol') for identity in ambiguous])
            self._edge(caller, boundary, 'calls', path, expr)
            self._edge(boundary, result, 'returns', path, expr)
            self.unresolved.append({'file': path, 'name': name, 'start': expr.get('start'), 'resolution': resolution,
                                    'candidateCount': len(ambiguous)})
            lexical_callee = self._reference(expr.get('callee', {}), path) if isinstance(expr.get('callee'), dict) and not receiver else None
            if lexical_callee and '->' in (self.nodes[lexical_callee].get('typeName') or ''):
                self.callback_calls.append({'callback': lexical_callee, 'caller': caller, 'arguments': arguments,
                                            'result': result, 'path': path, 'expression': expr})
                self._edge(lexical_callee, result, 'captures', path, expr, upstream=False,
                           description='Invokes the callable supplied through this function-typed value.')
            receiver_type = self.nodes.get(receiver, {}).get('typeName') or ''
            collection = re.match(r'(?:Mutable)?(?:List|Set|Collection|Iterable|Array|Sequence)<(.+)>\??$', receiver_type)
            for argument in arguments:
                if not argument.get('_lambda'):
                    continue
                types = None
                if collection and name in {'map', 'mapNotNull', 'mapIndexed', 'mapIndexedNotNull', 'flatMap', 'filter', 'filterNot', 'any', 'none', 'first', 'firstOrNull', 'indexOfFirst', 'sortedBy', 'sortedByDescending'}:
                    types = (['Int'] if 'Indexed' in name else []) + [collection.group(1)]
                elif name == 'let' and receiver_type:
                    types = [receiver_type.rstrip('?')]
                if types is not None:
                    parameters = self._prepare_lambda(argument['_lambda'], types, path)
                    for parameter in parameters[-1:]:
                        self._edge(receiver, parameter, 'derives', path, expr,
                                   description='Kotlin collection/scope callback receives this receiver or an element from it.',
                                   resolution='external')
                self.process_declaration(argument['_lambda'].get('declarationId'))
            for actual in ([receiver] if receiver else []) + [item.get('_node') for item in arguments]:
                self._edge(actual, result, 'derives', path, expr, downstream=name != 'copy',
                           description='Visible input to this external/unresolved expression; no implementation semantics are inferred.')
            self._infer_external_result(name, result, receiver, arguments, path, expr)
        return result

    def _prepare_lambda(self, expression, types, path):
        declaration_id = expression.get('declarationId')
        declaration = self.declarations.get(declaration_id, {})
        parameters = self._parameters(declaration)
        if parameters and len(parameters) == len(types):
            result = []
            for parameter, type_name in zip(parameters, types):
                node = self.declaration_node(parameter['id'])
                if not parameter.get('typeName'):
                    parameter['typeName'] = type_name
                    self.nodes[node]['typeName'] = type_name
                    self.nodes[node]['typeInference'] = 'callback signature'
                result.append(node)
            return result
        if not parameters and len(types) == 1 and declaration.get('implicitParameterCandidate'):
            if declaration_id not in self.implicit_parameters:
                owner = self.declaration_node(declaration_id)
                node = self._add_node(owner + '/implicit:it', self.nodes[owner]['key'] + '.param.it', path, expression,
                                      'parameter', 'it', self.nodes[owner]['symbol'] + '.it', ownerId=owner,
                                      ownerName=self.nodes[owner]['label'], ownerKind='function', typeName=types[0],
                                      isImplicit=True, isParameter=True, parameterRole='inferred-lambda',
                                      typeInference='single-parameter callback signature')
                self.implicit_parameters[declaration_id] = node
            return [self.implicit_parameters[declaration_id]]
        return [self.declaration_node(parameter['id']) for parameter in parameters]

    def _infer_external_result(self, name, result, receiver, arguments, path, expr):
        """Small language-shape hints; unresolved calls stay explicitly unresolved."""
        source_type = self.nodes.get(receiver, {}).get('typeName', '')
        if name in {'listOf', 'mutableListOf', 'setOf', 'mutableSetOf', 'arrayOf'}:
            types = {self.nodes[item['_node']].get('typeName') for item in arguments if item.get('_node')}
            element_type = next(iter(types)) if len(types) == 1 and None not in types and '' not in types else 'Unknown'
            self.nodes[result]['typeName'] = ('Set' if 'Set' in name or 'set' in name else 'List') + '<' + element_type + '>'
        elif name in {'trim', 'lowercase', 'uppercase', 'toString', 'joinToString', 'padStart'}:
            self.nodes[result]['typeName'] = 'String'
        elif name in {'toInt', 'toIntOrNull', 'indexOfFirst'}:
            self.nodes[result]['typeName'] = 'Int'
        elif name in {'copy', 'also', 'apply', 'filter', 'filterNot', 'take', 'sortedWith', 'sortedByDescending'}:
            self.nodes[result]['typeName'] = source_type
            if name in {'copy', 'also', 'apply'} and receiver:
                self.aliases.add((receiver, result, tuple(self.nodes[result]['contextIds'])))
                self.incoming_aliases[result].add((receiver, tuple(self.nodes[result]['contextIds'])))
                self.alias_evidence[(receiver, result, tuple(self.nodes[result]['contextIds']))].append((path, expr))
            if name == 'copy':
                for argument in arguments:
                    if argument.get('name') and argument.get('_node'):
                        self.object_slots[result][argument['name']] = argument['_node']
        elif name in {'isBlank', 'isNotBlank', 'isEmpty', 'isNotEmpty', 'contains', 'startsWith', 'any', 'none'}:
            self.nodes[result]['typeName'] = 'Boolean'
        if name in {'map', 'mapNotNull', 'mapIndexedNotNull', 'flatMap', 'let', 'run', 'withLock', 'withContext'}:
            for argument in arguments:
                value = argument.get('expression', {})
                if value.get('kind') == 'lambda' and value.get('declarationId'):
                    returned = self._result(value['declarationId'])
                    self._edge(returned, result, 'returns', path, expr,
                               description='Lambda result contributes to this call result; the library call remains an external boundary.')
                    element_type = self.nodes[returned].get('typeName') or 'Unknown'
                    self.nodes[result]['typeName'] = 'List<' + element_type + '>' if name in {'map', 'mapNotNull', 'mapIndexedNotNull', 'flatMap'} else element_type

    def evaluate(self, expr, path=None, receiver=None):
        if not isinstance(expr, dict):
            return None
        identity = expr.get('id')
        path = path or self.expression_file.get(identity)
        if not path:
            return None
        cache_key = (identity or id(expr), receiver)
        if cache_key in self.expr_values:
            return self.expr_values[cache_key]
        if cache_key in self.processing:
            return None
        self.processing.add(cache_key)
        kind = expr.get('kind', 'other')
        value = None
        if kind in {'reference', 'this'}:
            if kind == 'this':
                expr = {**expr, 'name': 'this'}
            value = self.project(receiver, _name(expr.get('name') or expr.get('text')), path, expr) if receiver else self._reference(expr, path)
        elif kind == 'qualified':
            base = self.evaluate(expr.get('receiver'), path)
            selector = expr.get('selector')
            if isinstance(selector, dict):
                value = self.evaluate(selector, path, base)
            elif expr.get('name'):
                value = self.project(base, expr['name'], path, expr)
        elif kind == 'call':
            value = self._call(expr, path, receiver)
        elif kind == 'declaration':
            decl_id = expr.get('declarationId')
            self.process_declaration(decl_id)
            value = self.declaration_node(decl_id)
        elif kind == 'lambda':
            decl_id = expr.get('declarationId')
            self.process_declaration(decl_id)
            value = self.declaration_node(decl_id)
        elif kind == 'return':
            returned = self.evaluate(expr.get('value') or expr.get('expression'), path)
            owner = self._function_owner(expr.get('ownerId'), include_lambda=bool(expr.get('targetLabel')))
            if owner:
                value = self._result(owner)
                self._edge(returned, value, 'returns', path, expr, identity=True,
                           description='Returned by this function or labelled lambda.')
                if returned:
                    self.returns[owner].add(returned)
        elif kind == 'assignment':
            target = self.evaluate(expr.get('left'), path)
            actual = self.evaluate(expr.get('right'), path)
            self._edge(actual, target, 'aliases' if expr.get('operator') == '=' else 'derives', path, expr,
                       identity=expr.get('operator') == '=', description='Assignment updates this scoped declaration or selected field.')
            value = target
        elif kind == 'block':
            for child in expr.get('children', expr.get('statements', [])):
                value = self.evaluate(child, path)
        else:
            parts = [self.evaluate(child, path) for child in _children(expr)]
            value = self._expression_node(expr, path)
            text = expr.get('text', '').strip()
            if kind == 'literal':
                self.nodes[value]['typeName'] = 'String' if text.startswith('"') else 'Boolean' if text in {'true', 'false'} else 'Long' if re.fullmatch(r'[\d_]+L', text) else 'Int' if re.fullmatch(r'[\d_]+', text) else ''
            if kind in {'stringTemplate', 'stringtemplate', 'template'}:
                self.nodes[value]['typeName'] = 'String'
            for part in parts:
                control_text = (expr.get('condition') or expr.get('subject') or {}).get('text', '') if kind in {'if', 'when'} else ''
                self._edge(part, value, 'reads' if kind in {'if', 'when'} else 'derives', path, expr,
                           condition=control_text, controlDependency=kind in {'if', 'when'}, description='Source expression reads this input.' if kind in {'if', 'when'} else 'Source expression derives this value from its visible inputs.')
        self.expr_values[cache_key] = value
        self.processing.discard(cache_key)
        return value

    def process_declaration(self, identity):
        if identity not in self.declarations or identity in self.processed_declarations:
            return
        self.processed_declarations.add(identity)
        declaration = self.declarations[identity]
        node = self.declaration_node(identity)
        path = self.declaration_file[identity]
        if not node:
            return
        if declaration.get('kind') == 'property':
            for accessor_id in declaration.get('accessorIds', []):
                accessor = self.declarations.get(accessor_id, {})
                if accessor.get('name') not in {'get', '<get>', 'getter'} and 'get' not in accessor.get('name', '').lower():
                    continue
                self.process_declaration(accessor_id)
                self._edge(self._result(accessor_id), node, 'returns', path, accessor, identity=True,
                           description='The property getter supplies its value.')
        for role in ('initializer', 'defaultValue', 'body'):
            expr_id = declaration.get(role + 'Id')
            expr = self.expressions.get(expr_id)
            if not expr and isinstance(declaration.get(role), dict):
                expr = declaration[role]
            if not expr:
                continue
            value = self.evaluate(expr, path)
            if role in {'initializer', 'defaultValue'}:
                self._edge(value, node, 'aliases', path, expr, identity=True,
                           condition='Used when this argument is omitted.' if role == 'defaultValue' else '',
                           description='Initializer supplies this declaration.')
                if value and not self.nodes[node].get('typeName'):
                    self.nodes[node]['typeName'] = self.nodes[value].get('typeName', '')
            elif declaration.get('kind') == 'lambda' or expr.get('kind') != 'block':
                result = self._result(identity)
                self._edge(value, result, 'returns', path, expr, identity=True,
                           description='Expression-body or lambda result.')
                if value:
                    self.returns[identity].add(value)
                    if not self.nodes[result].get('typeName'):
                        self.nodes[result]['typeName'] = self.nodes[value].get('typeName', '')

    def _link_callbacks(self):
        for call in self.callback_calls:
            pending, seen = [call['callback']], set()
            while pending:
                current = pending.pop()
                if current in seen:
                    continue
                seen.add(current)
                declaration_id = self.node_decl.get(current)
                declaration = self.declarations.get(declaration_id, {})
                if declaration.get('kind') == 'lambda':
                    parameters = [self.declaration_node(parameter['id']) for parameter in self._parameters(declaration)]
                    if not parameters and declaration_id in self.implicit_parameters:
                        parameters = [self.implicit_parameters[declaration_id]]
                    if len(parameters) == len(call['arguments']):
                        self._edge(call['caller'], current, 'calls', call['path'], call['expression'],
                                   phase='deferred', description='Invokes a lambda passed to this function-typed parameter.')
                        for parameter, argument in zip(parameters, call['arguments']):
                            self._edge(argument.get('_node'), parameter, 'passes', call['path'], call['expression'], identity=True,
                                       description='Value supplied when the source callback is invoked.')
                        self._edge(self._result(declaration_id), call['result'], 'returns', call['path'], call['expression'],
                                   upstream=False, description='The passed lambda returns the callback result.')
                    continue
                pending.extend(origin for origin, _ in sorted(self.incoming_aliases.get(current, set())))

    def _propagate_fields(self):
        # New projections can discover projections on earlier argument/alias nodes.
        for base, slots in list(self.object_slots.items()):
            node = self.nodes[base]
            path = node.get('file')
            if path not in self.files:
                continue
            expr = {'startLine': node['startLine'], 'endLine': node['endLine']}
            for member in sorted(slots):
                self.project(base, member, path, expr)
        pending = deque(self.projections.items())
        seen = set()
        while pending:
            (base, parts), target = pending.popleft()
            marker = (base, parts)
            if marker in seen:
                continue
            seen.add(marker)
            node = self.nodes[target]
            path = node.get('file') or self.nodes[base].get('file')
            if path not in self.files:
                continue
            path, expr = self.projection_evidence[target]
            slot = self.object_slots.get(base, {}).get(parts[0])
            if slot:
                source = slot if len(parts) == 1 else self.project(slot, '.'.join(parts[1:]), path, expr)
                self._edge(source, target, 'aliases', path, expr, identity=True,
                           description='Field receives its matching constructor or copy argument.')
                continue
            for origin, contexts in sorted(self.incoming_aliases.get(base, set())):
                evidence_items = self.alias_evidence.get((origin, base, contexts), [(path, expr)])[:]
                for origin_path, origin_evidence in evidence_items:
                    source = self.project(origin, '.'.join(parts), origin_path, origin_evidence)
                    self._edge(source, target, 'aliases', origin_path, origin_evidence, contexts=contexts, identity=True,
                               description='The same field is forwarded across this object argument or assignment.')
                pending.append(((origin, parts), source))

    def _rewire_curated_arguments(self):
        for edge in self.graph['edges'][:self.original_edges]:
            if edge['kind'] != 'passes' or not edge.get('argumentName'):
                continue
            declaration_id = self.node_decl.get(edge['to'])
            if not declaration_id or self.nodes[edge['to']]['kind'] not in {'callable', 'contract'}:
                continue
            matches = [parameter for parameter in self._parameters(self.declarations[declaration_id])
                       if parameter.get('name') == edge['argumentName']]
            if len(matches) == 1:
                edge['originalTarget'] = edge['to']
                edge['to'] = self.declaration_node(matches[0]['id'])
                edge['resolution'] = 'resolved'
        # Reviewed value aliases are also relevant to field projection propagation.
        for edge in self.graph['edges'][:self.original_edges]:
            if edge['kind'] in {'passes', 'aliases'} and 'value' in edge.get('traces', []):
                if edge['kind'] == 'passes' and edge.get('argumentName') and self.nodes[edge['to']]['kind'] == 'value':
                    # A constructor input contributes to a field of the whole value;
                    # it is not an identity alias between that field and its object.
                    self.object_slots[edge['to']][edge['argumentName']] = edge['from']
                    continue
                self.incoming_aliases[edge['to']].add((edge['from'], tuple(edge['contextIds'])))
                self.alias_evidence[(edge['from'], edge['to'], tuple(edge['contextIds']))].append((edge['evidence']['file'], edge['evidence']))

    def _interface_bindings(self):
        for edge in self._curated_call_edges:
            if edge['kind'] != 'binds':
                continue
            source_decl = self.node_decl.get(edge['from'])
            target_decl = self.node_decl.get(edge['to'])
            if not source_decl or not target_decl:
                continue
            source_parameters = self._parameters(self.declarations[source_decl])
            target_parameters = self._parameters(self.declarations[target_decl])
            if len(source_parameters) != len(target_parameters):
                continue
            path = self.declaration_file[target_decl]
            for source, target in zip(source_parameters, target_parameters):
                if source.get('name') != target.get('name'):
                    continue
                self._edge(self.declaration_node(source['id']), self.declaration_node(target['id']), 'passes',
                           path, target, contexts=edge['contextIds'], identity=True,
                           argumentName=target['name'], description='Parameter forwarding follows a reviewed interface implementation binding.')
            self._edge(self._result(target_decl), self._result(source_decl), 'returns', path, self.declarations[target_decl],
                       contexts=edge['contextIds'], identity=True,
                       description='Concrete result satisfies the reviewed interface return contract.')

    def _instantiate_returns(self):
        """Substitute formal inputs at each call instead of merging sibling call arguments."""
        incoming = defaultdict(list)
        for edge in self.graph['edges']:
            if 'value' in edge.get('traces', []) and edge.get('upstream', True):
                incoming[edge['to']].append(edge['from'])
        for call in self.calls:
            result = self.results.get(call['declaration'])
            if not result:
                continue
            pending = [result]
            seen = set()
            while pending:
                current = pending.pop()
                if current in seen:
                    continue
                seen.add(current)
                if current in call['actuals']:
                    self._edge(call['actuals'][current], call['result'], 'derives', call['path'], call['expression'],
                               description='This call result depends on the corresponding actual argument.')
                    continue
                if current in self.projection_parts:
                    base, parts = self.projection_parts[current]
                    if base in call['actuals']:
                        actual = self.project(call['actuals'][base], '.'.join(parts), call['path'], call['expression'])
                        self._edge(actual, call['result'], 'derives', call['path'], call['expression'],
                                   description='This call result depends on the matching field of its actual argument.')
                        continue
                if current == self.decl_node.get(call['declaration']):
                    continue
                # Do not walk through a different invocation's parameter origins.
                if self.nodes[current].get('isParameter') and current not in call['actuals']:
                    self._edge(current, call['result'], 'captures', call['path'], call['expression'],
                               description='The method uses a captured owner/outer parameter.')
                    continue
                pending.extend(incoming.get(current, []))

    def _instantiate_properties(self):
        incoming = defaultdict(list)
        for edge in self.graph['edges']:
            if 'value' in edge.get('traces', []) and edge.get('upstream', True):
                incoming[edge['to']].append(edge['from'])
        for (base, parts), target in list(self.projections.items()):
            actuals = self.constructor_inputs.get(base)
            declaration_id = self.nodes[target].get('fieldDeclarationId')
            declaration = self.declarations.get(declaration_id, {})
            if not actuals or declaration.get('kind') != 'property' or parts[0] in self.object_slots.get(base, {}):
                continue
            property_node = self.declaration_node(declaration_id)
            path = self.declaration_file[declaration_id]
            evidence = self.expressions.get(declaration.get('initializerId'), declaration)
            pending, seen = [property_node], set()
            while pending:
                current = pending.pop()
                if current in seen:
                    continue
                seen.add(current)
                if current in actuals:
                    self._edge(actuals[current], target, 'derives', path, evidence,
                               description='The field initializer/getter uses this constructor argument; see its source transformation.')
                    continue
                current_node = self.nodes[current]
                if current_node.get('expressionKind') in {'literal', 'stringTemplate'} and not incoming.get(current):
                    self._edge(current, target, 'derives', path, evidence,
                               description='Constant contribution from the field initializer.')
                if current_node.get('kind') == 'external':
                    self._edge(current, target, 'returns', path, evidence,
                               description='The field initializer includes this explicit external/unresolved operation.')
                if current_node.get('isParameter'):
                    continue
                pending.extend(incoming.get(current, []))

    def run(self):
        for identity, declaration in self.declarations.items():
            if declaration.get('kind') in EXCLUDED_KINDS:
                self.exclusions.append({'file': self.declaration_file[identity], 'symbol': declaration.get('symbol'),
                                        'reason': 'Type-only or non-value container declaration: ' + declaration.get('kind', '')})
            else:
                self.declaration_node(identity)
        self._rewire_curated_arguments()
        for identity in self.declarations:
            self.process_declaration(identity)
        for path, source in self.files.items():
            for expr in source.get('expressions', []):
                self.evaluate(expr, path)
        self._interface_bindings()
        self._link_callbacks()
        self._propagate_fields()
        self._instantiate_properties()
        self._instantiate_returns()
        self._propagate_fields()
        parameter_ids = [identity for identity in self.declarations
                         if identity in self.formal_parameter_ids and self._contexts(self.declaration_file[identity])]
        inferred_parameter_ids = [identity for identity in self.implicit_parameters.values()
                                  if self.nodes[identity].get('contextIds')]
        missing_parameters = [identity for identity in parameter_ids if identity not in self.decl_node]
        if missing_parameters:
            raise ValueError('Unrepresented Kotlin formal parameters: ' + ', '.join(missing_parameters))
        for identity, declaration in self.declarations.items():
            node_id = self.decl_node.get(identity)
            if node_id and declaration.get('kind') in CALLABLE_KINDS | {'interface'}:
                self.nodes[node_id]['signatureParameterIds'] = [self.declaration_node(parameter['id']) for parameter in self._parameters(declaration)]
                if identity in self.implicit_parameters:
                    self.nodes[node_id]['signatureParameterIds'].append(self.implicit_parameters[identity])
        self.graph['nodes'] = [node for node in self.graph['nodes'] if node.get('contextIds')]
        live_ids = {node['id'] for node in self.graph['nodes']}
        self.graph['edges'] = [edge for edge in self.graph['edges'] if edge['from'] in live_ids and edge['to'] in live_ids]
        self.graph['index'] = {
            'schemaVersion': 1, 'sourceFiles': len(self.files), 'parsedDeclarations': len(self.declarations),
            'parsedCallables': sum(declaration.get('kind') in CALLABLE_KINDS for declaration in self.declarations.values()),
            'formalParameters': len(parameter_ids) + len(inferred_parameter_ids),
            'representedParameters': len(parameter_ids) - len(missing_parameters) + len(inferred_parameter_ids),
            'parameterCount': len(parameter_ids) + len(inferred_parameter_ids), 'missingParameters': missing_parameters,
            'declaredParameterCount': len(parameter_ids), 'inferredParameterCount': len(inferred_parameter_ids),
            'localParameterBindingCount': sum(declaration.get('kind') == 'parameter' and identity not in self.formal_parameter_ids
                                             and not declaration.get('typeOnly') for identity, declaration in self.declarations.items()),
            'callableCount': sum(declaration.get('kind') in CALLABLE_KINDS for declaration in self.declarations.values()),
            'unresolvedCount': len(self.unresolved), 'sourceFileCount': len(self.files),
            'generatedNodes': len(self.nodes) - self.original_nodes,
            'generatedEdges': len(self.graph['edges']) - self.original_edges,
            'resolvedCalls': len(self.calls), 'unresolvedCalls': len(self.unresolved),
            'fieldProjections': len(self.projections), 'exclusions': self.exclusions,
            'unresolved': self.unresolved,
            'limitations': ['Syntax-based, conservative resolution; no Kotlin compiler binding context or runtime execution.',
                            'External and ambiguous calls remain explicit boundaries.',
                            'Condition and mutation edges describe possible source dependencies, not an execution history.'],
        }
        return self.graph


def augment_graph(graph, inventory, parsed_ir, contexts, journeys):
    """Return a new graph enriched with every indexed declaration and formal parameter."""
    return SourceGraph(graph, inventory, parsed_ir, contexts, journeys).run()
