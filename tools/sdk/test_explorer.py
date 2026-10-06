"""Integrity tests for reviewed source graphs; does not modify repository source."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import unittest

SPEC = importlib.util.spec_from_file_location('build_explorer', Path(__file__).with_name('build-explorer.py'))
builder = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(builder)


def file(path, source):
    return {'path': path, 'source': source, 'hash': hashlib.sha256(source.encode()).hexdigest(),
            'module': ':sdk:test', 'binary': False}


class ExplorerIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.files = [file('sdk/provider.kt', 'fun createAndroid() {\n    val client = createHttpClient()\n    build(client)\n}\n'),
                      file('sdk/runtime.kt', 'fun build(httpClient: HttpClient) {\n    use(httpClient)\n}\n')]
        contexts = ['tmdb-android']
        self.fragment = {'nodes': [
            {'id': 'factory', 'file': 'sdk/provider.kt', 'anchor': 'fun createAndroid()', 'symbol': 'Provider.createAndroid',
             'label': 'createAndroid', 'kind': 'callable', 'contextIds': contexts, 'journeys': ['construction'], 'column': 0},
            {'id': 'client', 'file': 'sdk/provider.kt', 'anchor': 'val client = createHttpClient()', 'symbol': 'Provider.createAndroid.client',
             'label': 'client', 'kind': 'value', 'contextIds': contexts, 'journeys': ['construction'], 'column': 1, 'ownerId': 'factory'},
            {'id': 'parameter', 'file': 'sdk/runtime.kt', 'anchor': 'httpClient: HttpClient', 'symbol': 'Runtime.build.httpClient',
             'label': 'httpClient', 'kind': 'parameter', 'contextIds': contexts, 'journeys': ['construction'], 'column': 2}],
            'edges': [{'id': 'pass', 'from': 'client', 'to': 'parameter', 'kind': 'passes', 'contextIds': contexts,
                       'argumentName': 'httpClient', 'argumentIndex': 1, 'evidence': {'file': 'sdk/provider.kt', 'anchor': 'build(client)'}}],
            'coverage': []}
        self.hashes = {f['path']: f['hash'] for f in self.files}

    def compile(self):
        return builder.compile_graph([self.fragment], self.files, self.hashes)

    def test_canonical_identity_and_argument_evidence(self):
        graph = self.compile()
        self.assertEqual(graph['nodes'][1]['id'], 'sdk/provider.kt#Provider.createAndroid.client')
        self.assertEqual(graph['nodes'][1]['ownerId'], graph['nodes'][0]['id'])
        self.assertEqual(graph['edges'][0]['traces'], ['value'])
        self.assertEqual(graph['edges'][0]['evidence']['startLine'], 3)
        self.assertEqual(self.fragment['nodes'][0]['id'], 'factory', 'Compilation does not mutate authored data')

    def test_duplicate_simple_names_stay_separate(self):
        node = copy.deepcopy(self.fragment['nodes'][0])
        node.update(id='other-factory', file='sdk/other.kt')
        self.fragment['nodes'].append(node)
        other = file('sdk/other.kt', 'fun createAndroid() {}')
        self.files.append(other)
        self.hashes[other['path']] = other['hash']
        ids = [n['id'] for n in self.compile()['nodes']]
        self.assertEqual(len(ids), len(set(ids)))

    def test_duplicate_scoped_identity_is_rejected(self):
        node = copy.deepcopy(self.fragment['nodes'][0])
        node['id'] = 'different-alias'
        self.fragment['nodes'].append(node)
        with self.assertRaisesRegex(ValueError, 'Duplicate scoped declaration'):
            self.compile()

    def test_missing_or_ambiguous_anchor_is_rejected(self):
        for anchor in ['missingFunction()', 'client']:
            with self.subTest(anchor=anchor):
                self.fragment['nodes'][1]['anchor'] = anchor
                with self.assertRaisesRegex(ValueError, 'Anchor must match once'):
                    self.compile()

    def test_whitespace_and_line_movement_resolve_after_review(self):
        moved = file('sdk/provider.kt', '\n\nfun createAndroid() {\n  val client   =   createHttpClient()\n  build(client)\n}\n')
        self.files[0] = moved
        with self.assertRaisesRegex(ValueError, 'Review required'):
            self.compile()
        self.hashes[moved['path']] = moved['hash']
        graph = self.compile()
        self.assertEqual(graph['nodes'][1]['startLine'], 4)
        self.assertEqual(graph['edges'][0]['evidence']['startLine'], 5)

    def test_new_wiring_in_mapped_file_requires_review(self):
        changed = file('sdk/provider.kt', self.files[0]['source'] + 'fun newWiring() = createSomethingElse()\n')
        self.files[0] = changed
        with self.assertRaisesRegex(ValueError, 'Review required'):
            self.compile()

    def test_cross_context_and_dangling_edges_are_rejected(self):
        self.fragment['edges'][0]['contextIds'] = ['tmdb-web']
        with self.assertRaisesRegex(ValueError, 'Context crosses'):
            self.compile()
        self.fragment['edges'][0]['contextIds'] = ['tmdb-android']
        self.fragment['edges'][0]['to'] = 'unknown'
        with self.assertRaisesRegex(ValueError, 'Dangling edge'):
            self.compile()

    def test_wrong_argument_binding_is_rejected(self):
        self.fragment['edges'][0]['argumentName'] = 'storage'
        with self.assertRaisesRegex(ValueError, 'Argument does not name target'):
            self.compile()
        self.fragment['edges'][0]['argumentName'] = 'httpClient'
        self.fragment['edges'][0]['argumentIndex'] = 0
        with self.assertRaisesRegex(ValueError, 'one-based'):
            self.compile()

    def test_cycles_are_valid_and_ownership_is_not_traversable(self):
        edge = copy.deepcopy(self.fragment['edges'][0])
        edge.update(id='cycle', **{'from': 'parameter', 'to': 'client', 'kind': 'aliases'})
        edge.pop('argumentName')
        edge.pop('argumentIndex')
        self.fragment['edges'].append(edge)
        edge = copy.deepcopy(edge)
        edge.update(id='ownership', kind='owns')
        self.fragment['edges'].append(edge)
        graph = self.compile()
        self.assertEqual(graph['edges'][1]['traces'], ['value'])
        self.assertEqual(graph['edges'][2]['traces'], [])

    def test_new_public_operation_needs_coverage(self):
        path = 'sdk/api/src/commonMain/kotlin/example/AuthService.kt'
        self.files.append(file(path, '/** fun fake() */\ninterface AuthService {\n suspend fun restoreSession(): Result\n}\n'))
        with self.assertRaisesRegex(ValueError, 'Public operation coverage differs'):
            self.compile()

    def test_coverage_cannot_point_at_unrelated_implementation(self):
        path = 'sdk/api/src/commonMain/kotlin/example/AuthService.kt'
        self.files.append(file(path, 'interface AuthService { fun restoreSession(): Result }'))
        self.fragment['coverage'].append({'file': path, 'operation': 'restoreSession', 'nodeId': 'factory', 'journey': 'construction'})
        with self.assertRaisesRegex(ValueError, 'Invalid operation coverage'):
            self.compile()

    def test_stale_hash_entries_are_rejected(self):
        self.hashes['removed.kt'] = 'abc'
        with self.assertRaisesRegex(ValueError, 'Unused reviewed sources'):
            self.compile()


class ReviewedSdkJourneysTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        fragments = [json.loads((builder.CONTENT / name).read_text(encoding='utf-8'))
                     for name in ('construction.json', 'services.json')]
        inventory, _ = builder.source_inventory()
        hashes = json.loads((builder.CONTENT / 'reviewed-sources.json').read_text(encoding='utf-8'))
        cls.graph = builder.compile_graph(fragments, inventory, hashes)
        cls.by_key = {node['key']: node for node in cls.graph['nodes']}
        cls.by_id = {node['id']: node for node in cls.graph['nodes']}
        cls.sources = {item['path']: item['source'] for item in inventory}

    def reachable(self, key, context, mode, upstream=False):
        start = self.by_key[key]['id']
        adjacency = {}
        for edge in self.graph['edges']:
            if context in edge['contextIds'] and mode in edge['traces']:
                source, target = (edge['to'], edge['from']) if upstream else (edge['from'], edge['to'])
                adjacency.setdefault(source, set()).add(target)
        reached = {start}
        pending = [start]
        while pending:
            for target in adjacency.get(pending.pop(), set()) - reached:
                reached.add(target)
                pending.append(target)
        return {self.by_id[id]['key'] for id in reached}

    def test_http_value_chain_uses_platform_origin(self):
        for platform, engine, other in [('android', 'external.okhttp.engine', 'external.js.engine'),
                                        ('web', 'external.js.engine', 'external.okhttp.engine')]:
            with self.subTest(platform=platform):
                reached = self.reachable('tmdb.api.httpClient', f'tmdb-{platform}', 'value', upstream=True)
                self.assertTrue({f'tmdb.{platform}.client', 'tmdb.factory.httpClient', 'tmdb.http.factory', engine} <= reached)
                self.assertNotIn(other, reached)

    def test_account_repositories_are_deferred_until_authentication(self):
        startup = self.reachable('tmdb.android.factory', 'tmdb-android', 'call')
        authenticated = self.reachable('runtime.auth.installAuthenticationState', 'tmdb-android', 'call')
        self.assertNotIn('tmdb.home.repository', startup)
        self.assertIn('tmdb.home.repository', authenticated)

    def test_runtime_configuration_traces_only_common_fields_in_every_context(self):
        cases = [
            ('tmdb', 'android', 'app.tmdb.config.common', 'app.tmdb.config'),
            ('tmdb', 'web', 'app.web.config.common', 'app.web.config'),
            ('clientb', 'android', 'app.clientb.config.common', 'app.clientb.config'),
            ('clientb', 'web', 'external.clientb.web.common', None),
        ]
        forbidden = {'tmdb.http.config', 'app.tmdb.connection', 'app.web.connection'}
        source_keys = {source for _, _, source, _ in cases}
        for provider, platform, source_key, application_config in cases:
            context = f'{provider}-{platform}'
            factory_field = f'{provider}.factory.config.common'
            platform_field = f'{provider}.{platform}.config.common'
            with self.subTest(context=context):
                reached = self.reachable('runtime.client.configuration', context, 'value', upstream=True)
                self.assertTrue({factory_field, platform_field, source_key} <= reached, reached)
                self.assertFalse(forbidden & reached, f'Common configuration inherited sibling connection values: {forbidden & reached}')
                self.assertFalse((source_keys - {source_key}) & reached, 'Configuration origin must respect provider and platform')
                self.assertNotIn(f'{provider}.factory.config', reached, 'A field trace must not widen to the whole shared-factory config')
                self.assertNotIn(f'{provider}.{platform}.config', reached, 'A field trace must not widen to the whole platform config')

                bindings = [edge for edge in self.graph['edges']
                            if edge['from'] == self.by_key[factory_field]['id']
                            and edge['to'] == self.by_key['runtime.client.configuration']['id']
                            and context in edge['contextIds'] and 'value' in edge['traces']]
                self.assertEqual(len(bindings), 1, 'Runtime configuration receives the selected provider\'s common field')
                self.assertEqual(bindings[0]['kind'], 'passes')
                self.assertEqual(bindings[0]['argumentName'], 'configuration')
                evidence = bindings[0]['evidence']
                excerpt = '\n'.join(self.sources[evidence['file']].splitlines()[evidence['startLine'] - 1:evidence['endLine']])
                self.assertRegex(excerpt, r'configuration\s*=\s*config\.common', 'The edge proves the actual constructor argument')

                if application_config:
                    source = self.by_key[source_key]
                    self.assertEqual(source['file'], self.by_key[application_config]['file'])
                    source_excerpt = '\n'.join(self.sources[source['file']].splitlines()[source['startLine'] - 1:source['endLine']])
                    self.assertIn('StreamCoreConfiguration', source_excerpt, 'The origin identifies actual application construction')

    def test_whole_tmdb_config_still_retains_connection_dependencies(self):
        for context, application_config, connection in [
            ('tmdb-android', 'app.tmdb.config', 'app.tmdb.connection'),
            ('tmdb-web', 'app.web.config', 'app.web.connection'),
        ]:
            with self.subTest(context=context):
                whole_config = self.reachable('tmdb.factory.config', context, 'value', upstream=True)
                self.assertTrue({application_config, connection} <= whole_config,
                                'Fixing common-field provenance must preserve whole-object dependencies')
                common_config = self.reachable('runtime.client.configuration', context, 'value', upstream=True)
                self.assertNotIn(connection, common_config)

    def test_clientb_web_common_configuration_stops_at_sdk_caller(self):
        boundary_key = 'external.clientb.web.common'
        boundary = self.by_key[boundary_key]
        self.assertTrue(boundary['external'])
        self.assertEqual(boundary['contextIds'], ['clientb-web'])
        reached = self.reachable('runtime.client.configuration', 'clientb-web', 'value', upstream=True)
        self.assertIn(boundary_key, reached)
        self.assertFalse(any(key.startswith('app.') for key in reached),
                         'An SDK-only browser factory must not invent current application wiring')
        self.assertEqual(self.reachable(boundary_key, 'clientb-web', 'value', upstream=True), {boundary_key},
                         'The supplied common configuration is the documented external origin')

    def test_library_receives_same_playback_instance(self):
        reached = self.reachable('runtime.client.playbackValue', 'tmdb-android', 'value')
        self.assertIn('runtime.client.playback', reached)
        self.assertIn('runtime.library.service', reached)
        self.assertIn('runtime.playback.recorder', self.reachable('runtime.playback.createProgressRecorder', 'tmdb-android', 'call'))

    def test_actual_application_paths_exclude_clientb_web(self):
        for node in self.graph['nodes']:
            if node['key'].startswith('app.'):
                self.assertNotIn('clientb-web', node['contextIds'], node['key'])
        self.assertEqual(len(self.graph['coverage']), 38)
        self.assertFalse(any(item.get('exclusion') for item in self.graph['coverage']))


if __name__ == '__main__':
    unittest.main()
