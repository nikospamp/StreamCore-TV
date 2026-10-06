"""Behavioral fixtures for conservative Kotlin source/data-flow augmentation."""
import unittest

from kotlin_graph import augment_graph
from kotlin_psi import parse_sources


CONTEXTS = ['tmdb-android', 'tmdb-web', 'clientb-android', 'clientb-web']
JOURNEYS = ['construction', 'auth', 'profiles', 'home', 'details', 'search', 'library', 'playback', 'storage', 'lifecycle']
PATH = 'sdk/fixture/src/commonMain/kotlin/Fixture.kt'


def source(path, text):
    return {'path': path, 'source': text, 'module': ':sdk:fixture', 'binary': False}


class KotlinGraphTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.text = '''package fixture
data class Common(val backend: String, val namespace: String)
data class Config(val common: Common, val connection: String)
fun encode(value: String): String { return value.trim() }
fun storage(config: Common, prefix: String): String {
    val key = "$prefix:${encode(config.backend)}:${encode(config.namespace)}"
    if (config.backend.isBlank()) return prefix
    return key
}
fun make(config: Config): String { return storage(config.common, "fixed") }
fun app(): String { return make(Config(connection = "private-token", common = Common("backend-a", "partition"))) }
fun pair(first: String, second: String): String { return first + second }
fun named(): String { return pair(second = "second", first = "first") }
fun positional(): String { return pair("one", "two") }
fun consume(value: String): String { return value }
fun repeated(value: String) { consume(value); consume(value) }
fun overload(value: String): String { return value }
fun overload(value: String, suffix: String): String { return value + suffix }
fun arity(value: String): String { return overload(value, "tail") }
fun shadow(value: String): String {
    val text = value
    run { val text = text + "inner"; consume(text) }
    return text
}
fun recovery(value: String): String {
    try { return consume(value) }
    catch (problem: Exception) { consume(value); throw problem }
    finally { consume(value) }
}
fun guarded(config: String, context: String, authFileName: String): String {
    for (file in listOf(config)) { consume(file) }
    try { return consume(context + authFileName) }
    catch (problem: Exception) { consume(config); throw problem }
}
data class Item(val name: String)
fun withItem(block: (Item) -> String): String { return block(Item("fixture")) }
fun callback(query: String): String { return withItem { profile -> profile.name + query } }
fun mapped(items: List<Item>): List<String> { return items.map { item -> item.name.trim() } }
fun implicit(items: List<Item>): List<String> { return items.map { it.name } }
class Recorder(private val request: String, initialPosition: Long) {
    private var bucket = initialPosition / 10L
    fun update(position: Long): Long { bucket = position / 10L; return bucket }
}
class Settings(prefix: String) { val storageKey = prefix.trim() }
fun options(): String { return Settings(" configured ").storageKey }
// Astral character before a declaration must not move source ranges: 🎬
fun unicode(value: String): String { return consume(value) }
'''
        cls.inventory = [source(PATH, cls.text)]
        cls.ir = parse_sources(cls.inventory)
        storage_line = cls.text[:cls.text.index('fun storage(')].count('\n') + 1
        cls.curated = {'nodes': [{'id': PATH + '#storage', 'key': 'fixture.storage', 'file': PATH,
                                 'startLine': storage_line, 'endLine': storage_line, 'label': 'storage',
                                 'symbol': 'storage', 'kind': 'callable', 'contextIds': CONTEXTS,
                                 'journeys': ['storage'], 'column': 2}], 'edges': [], 'coverage': []}
        cls.graph = augment_graph(cls.curated, cls.inventory, cls.ir, CONTEXTS, JOURNEYS)
        cls.nodes = {node['id']: node for node in cls.graph['nodes']}
        cls.declarations = {declaration['id']: declaration for file in cls.ir['files'] for declaration in file['declarations']}

    def declaration(self, name, owner=None, kind=None):
        matches = []
        for declaration in self.declarations.values():
            if declaration.get('name') != name or kind and declaration.get('kind') != kind:
                continue
            parent = self.declarations.get(declaration.get('ownerId'), {})
            if owner and parent.get('name') != owner:
                continue
            node = next((node for node in self.graph['nodes'] if node.get('declarationId') == declaration['id']), None)
            if node:
                matches.append(node)
        self.assertEqual(len(matches), 1, (name, owner, kind, [node['key'] for node in matches]))
        return matches[0]

    def reachable(self, start, upstream=False, graph=None):
        graph = graph or self.graph
        edges = [edge for edge in graph['edges'] if 'value' in edge.get('traces', [])
                 and edge.get('upstream' if upstream else 'downstream', True)]
        reached, pending = {start}, [start]
        while pending:
            current = pending.pop()
            for edge in edges:
                before, after = (edge['to'], edge['from']) if upstream else (edge['from'], edge['to'])
                if before == current and after not in reached:
                    reached.add(after)
                    pending.append(after)
        return reached

    def test_every_formal_parameter_and_curated_identity_is_retained(self):
        self.assertEqual(self.graph['index']['missingParameters'], [])
        self.assertEqual(self.graph['index']['parameterCount'], self.graph['index']['representedParameters'])
        storage = next(node for node in self.graph['nodes'] if node['key'] == 'fixture.storage')
        self.assertEqual(storage['id'], PATH + '#storage')
        parameters = [self.nodes[identity] for identity in storage['signatureParameterIds']]
        self.assertEqual([node['label'] for node in parameters], ['config', 'prefix'])
        self.assertEqual(parameters[0]['key'], 'fixture.storage.param.config')
        self.assertEqual(parameters[0]['typeName'], 'Common')
        self.assertEqual(len(self.curated['nodes']), 1, 'Augmentation does not mutate reviewed input')

    def test_field_input_origin_does_not_inherit_sibling_credentials(self):
        parameter = self.declaration('config', owner='storage', kind='parameter')
        reached = self.reachable(parameter['id'], upstream=True)
        expressions = [self.nodes[identity].get('expressionText', '') for identity in reached]
        self.assertFalse(any(text == '"private-token"' for text in expressions), expressions)
        self.assertTrue(any('Common(' in text for text in expressions), expressions)
        projections = [node for node in self.graph['nodes'] if node.get('ownerId') == parameter['id'] and node.get('fieldPath')]
        self.assertEqual({node['fieldPath'] for node in projections}, {'backend', 'namespace'})
        downstream = self.reachable(parameter['id'])
        self.assertTrue({node['id'] for node in projections} <= downstream)
        self.assertIn(self.declaration('key', owner='storage', kind='local')['id'], downstream,
                      'String-template interpolations remain connected to their parameters')

    def test_named_and_positional_arguments_bind_the_correct_parameter(self):
        first = self.declaration('first', owner='pair', kind='parameter')
        second = self.declaration('second', owner='pair', kind='parameter')
        for target, expected in [(first, {'"first"', '"one"'}), (second, {'"second"', '"two"'})]:
            incoming = [self.nodes[edge['from']].get('expressionText') for edge in self.graph['edges']
                        if edge['to'] == target['id'] and edge['kind'] == 'passes']
            self.assertEqual(set(incoming), expected)

    def test_overloads_use_arity_and_repeated_calls_keep_each_evidence(self):
        arity = self.declaration('arity', kind='function')
        calls = [edge for edge in self.graph['edges'] if edge['from'] == arity['id'] and edge['kind'] == 'calls']
        targets = [self.nodes[edge['to']] for edge in calls]
        self.assertTrue(any(len(target.get('signatureParameterIds', [])) == 2 for target in targets))
        repeated = self.declaration('repeated', kind='function')
        consume = self.declaration('consume', kind='function')
        occurrences = [edge for edge in self.graph['edges'] if edge['from'] == repeated['id'] and edge['to'] == consume['id'] and edge['kind'] == 'calls']
        self.assertEqual(len(occurrences), 2, 'Two uses on the same source line retain two distinct occurrences')

    def test_catch_and_finally_are_indexed(self):
        recovery = self.declaration('recovery', kind='function')
        consume = self.declaration('consume', kind='function')
        occurrences = [edge for edge in self.graph['edges'] if edge['from'] == recovery['id'] and edge['to'] == consume['id'] and edge['kind'] == 'calls']
        self.assertEqual(len(occurrences), 3)
        self.assertEqual(len({edge['evidence']['startLine'] for edge in occurrences}), 3)

    def test_catch_and_loop_bindings_are_not_function_inputs(self):
        guarded = self.declaration('guarded', kind='function')
        inputs = [self.nodes[identity] for identity in guarded['signatureParameterIds']]
        self.assertEqual([node['label'] for node in inputs], ['config', 'context', 'authFileName'])
        self.assertEqual(len(inputs), 3)
        for name in ['problem', 'file']:
            binding = self.declaration(name, owner='guarded', kind='parameter')
            self.assertEqual(binding['kind'], 'value')
            self.assertEqual(binding['parameterRole'], 'local-binding')
            self.assertFalse(binding['isParameter'])
            self.assertNotIn(binding['id'], guarded['signatureParameterIds'])
            self.assertTrue(any(edge['from'] == binding['id'] for edge in self.graph['edges']),
                            'Local binding usages remain indexed')
        self.assertGreater(self.graph['index']['localParameterBindingCount'], 0)

    def test_constructor_properties_and_non_property_inputs_remain_distinct(self):
        request = self.declaration('request', kind='parameter')
        initial = self.declaration('initialPosition', kind='parameter')
        bucket = self.declaration('bucket', kind='property')
        self.assertTrue(request['propertyParameter'])
        self.assertFalse(initial['propertyParameter'])
        self.assertIn(bucket['id'], self.reachable(initial['id']))

    def test_source_callbacks_and_typed_collection_lambda_inputs_are_connected(self):
        profile = self.declaration('profile', kind='parameter')
        item = self.declaration('item', kind='parameter')
        self.assertEqual(profile['typeName'], 'Item')
        self.assertEqual(item['typeName'], 'Item')
        incoming = [edge for edge in self.graph['edges'] if edge['to'] == profile['id'] and edge['kind'] == 'passes']
        self.assertTrue(incoming, 'Actual block(Item(...)) invocation supplies the lambda argument')
        items = self.declaration('items', owner='mapped', kind='parameter')
        self.assertIn(item['id'], self.reachable(items['id']))
        implicit = [node for node in self.graph['nodes'] if node.get('isImplicit') and node['label'] == 'it']
        self.assertTrue(any(node['typeName'] == 'Item' for node in implicit))

    def test_derived_constructor_property_traces_its_initializer_argument(self):
        projections = [node for node in self.graph['nodes'] if node.get('fieldPath') == 'storageKey'
                       and 'call@' in node['symbol']]
        self.assertEqual(len(projections), 1)
        origins = self.reachable(projections[0]['id'], upstream=True)
        self.assertTrue(any(self.nodes[identity].get('expressionText') == '" configured "' for identity in origins))
        evidence = [edge for edge in self.graph['edges'] if edge['to'] == projections[0]['id'] and edge['kind'] == 'derives']
        self.assertTrue(any('prefix.trim()' in self.text.splitlines()[edge['evidence']['startLine'] - 1] for edge in evidence))

    def test_unicode_source_ranges_and_deterministic_output(self):
        declaration = self.declaration('unicode', kind='function')
        line = self.text.splitlines()[declaration['startLine'] - 1]
        # PSI can include a preceding comment in a declaration; its full range must contain the function.
        excerpt = '\n'.join(self.text.splitlines()[declaration['startLine'] - 1:declaration['endLine']])
        self.assertIn('fun unicode', excerpt, line)
        repeated = augment_graph(self.curated, self.inventory, self.ir, CONTEXTS, JOURNEYS)
        self.assertEqual(self.graph, repeated)

    def test_unimported_and_ambiguous_calls_are_not_guessed(self):
        files = [source('sdk/fixture/src/commonMain/kotlin/Hidden.kt', 'package hidden\nfun secret(value: String): String { return value }\n'),
                 source('sdk/fixture/src/commonMain/kotlin/Use.kt', '''package consumer
fun pick(value: String): String { return value }
fun pick(value: Int): Int { return value }
fun use(input: Unknown): Unknown { secret("private"); return pick(input) }
''')]
        result = augment_graph({'nodes': [], 'edges': [], 'coverage': []}, files, parse_sources(files), CONTEXTS, JOURNEYS)
        boundaries = [node for node in result['nodes'] if node.get('kind') == 'external']
        self.assertTrue(any('secret' in node['label'] and node['resolution'] == 'unresolved' for node in boundaries))
        self.assertTrue(any('pick' in node['label'] and len(node.get('candidateSymbols', [])) == 2 for node in boundaries))

    def test_sample_and_build_sources_do_not_become_current_application_origins(self):
        files = [source(PATH, 'package fixture\nfun input(value: String): String { return value }\n'),
                 source('samples/sdk-consumer/src/commonMain/kotlin/Sample.kt', 'package fixture\nfun sample(): String { return input("sample-only") }\n'),
                 source('build-logic/src/main/kotlin/Build.kt', 'package fixture\nfun buildOnly(): String { return input("build-only") }\n')]
        result = augment_graph({'nodes': [], 'edges': [], 'coverage': []}, files, parse_sources(files), CONTEXTS, JOURNEYS)
        self.assertTrue(all(node.get('contextIds') for node in result['nodes']))
        self.assertFalse(any(node.get('file', '').startswith(('samples/', 'build-logic/')) for node in result['nodes']))
        self.assertEqual(result['index']['sourceFileCount'], 1)


if __name__ == '__main__':
    unittest.main()
