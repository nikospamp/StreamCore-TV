"""Contract fixtures for real Kotlin PSI extraction, independent of the SDK map."""
import json
from pathlib import Path
import unittest

from kotlin_psi import parse_sources


SOURCE = '''package fixture
import other.Provider as ProviderAlias
import helpers.*
// Keep exact source offsets after a supplementary character: 🎬
data class Config(val common: String, var enabled: Boolean = true)
class Owner(private val config: Config, initial: Int = 4) {
    constructor(config: Config): this(config, 9)
    private var count = initial
    private val deferred by lazy { config.common }
    private val callback: (description: String) -> String = { value -> value }
    var label: String
        get() = config.common
        set(value) { count = value.length }
    private fun consume(value: String): String = value
    private fun consume(value: Int): String = value.toString()
    fun route(value: Int): String {
        val local = config.common
        val call = invoke(first = local, *arrayOf(value), block = { text: String -> text })
        invoke(local) { text: String -> consume(text) }
        if (config.enabled) count += value
        when (val result = next(value)) {
            0 -> consume(result)
            else -> consume("${result}:${config.common}:$local")
        }
        when (val result = next(value + 1)) {
            else -> consume(result)
        }
        val closure = { local }
        val reference = ::Config
        for ((key, item) in entries) { consume(key); consume(item) }
        try { unknown() } catch (failure: Exception) { consume(failure.message ?: "") }
        return invoke(local) block@{ text -> return@block text }
    }
    class Nested(val nested: String)
}
private fun Config.extension(suffix: String = "!"): String = common + suffix
fun next(value: Int): Int = value
'''


def expressions(file):
    result = {}

    def collect(value):
        if isinstance(value, dict):
            if '#expr:' in value.get('id', ''):
                result[value['id']] = value
            for child in value.values():
                collect(child)
        elif isinstance(value, list):
            for child in value:
                collect(child)

    collect(file['expressions'])
    return result


class KotlinPsiTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.result = parse_sources([{'path': 'fixtures/Syntax.kt', 'source': SOURCE}])
        cls.file = cls.result['files'][0]
        cls.declarations = cls.file['declarations']
        cls.by_id = {node['id']: node for node in cls.declarations}
        cls.expressions = expressions(cls.file)

    def test_constructor_parameters_overloads_and_receiver_types(self):
        self.assertEqual([], self.file['errors'])
        owner = next(node for node in self.declarations if node['name'] == 'Owner' and node['kind'] == 'class')
        self.assertEqual(2, len(owner['constructorIds']))
        constructor = self.by_id[owner['primaryConstructorId']]
        self.assertTrue(constructor['primary'])
        self.assertEqual(owner['parameterIds'], constructor['parameterIds'])
        config, initial = [self.by_id[key] for key in constructor['parameterIds']]
        self.assertEqual(constructor['id'], config['ownerId'])
        self.assertTrue(config['propertyParameter'])
        self.assertFalse(initial['propertyParameter'])
        self.assertEqual('4', initial['defaultValue'])
        self.assertEqual('Config', config['typeName'])
        overloads = [node for node in self.declarations if node['name'] == 'consume']
        self.assertEqual(2, len({node['id'] for node in overloads}))
        self.assertTrue(all('private' in node['modifiers'] for node in overloads))
        extension = next(node for node in self.declarations if node['name'] == 'extension')
        self.assertEqual('Config', extension['receiverType'])
        self.assertEqual('String', extension['returnType'])

    def test_shadowed_locals_keep_distinct_when_scopes(self):
        locals = [node for node in self.declarations if node['name'] == 'result' and node['kind'] == 'local']
        self.assertEqual(2, len(locals))
        self.assertNotEqual(locals[0]['id'], locals[1]['id'])
        self.assertNotEqual(locals[0]['lexicalScopeStart'], locals[1]['lexicalScopeStart'])
        for local in locals:
            scope = SOURCE[local['lexicalScopeStart']:local['lexicalScopeEnd']]
            self.assertTrue(scope.startswith('when (val result ='))
        self.assertTrue(any(node['kind'] == 'destructuringEntry' and node['name'] == 'key' for node in self.declarations))
        catch = next(node for node in self.declarations if node['name'] == 'failure')
        self.assertTrue(SOURCE[catch['lexicalScopeStart']:catch['lexicalScopeEnd']].startswith('catch'))

    def test_argument_forms_preserve_single_trailing_lambda(self):
        calls = [node for node in self.expressions.values() if node['kind'] == 'call' and node['name'] == 'invoke']
        named = next(node for node in calls if node['arguments'][0]['name'] == 'first')
        self.assertEqual(['first', None, 'block'], [arg['name'] for arg in named['arguments']])
        self.assertTrue(named['arguments'][1]['spread'])
        trailing = next(node for node in calls if 'consume(text)' in node['text'])
        self.assertEqual(2, len(trailing['arguments']))
        self.assertTrue(trailing['arguments'][1]['trailingLambda'])
        self.assertEqual('lambda', trailing['arguments'][1]['expression']['kind'])
        declaration = self.by_id[trailing['arguments'][1]['expression']['declarationId']]
        self.assertEqual('text', self.by_id[declaration['parameterIds'][0]]['name'])

    def test_property_reads_templates_conditions_and_returns(self):
        kinds = {node['kind'] for node in self.expressions.values()}
        self.assertTrue({'qualified', 'stringTemplate', 'if', 'when', 'assignment', 'return', 'callableReference'} <= kinds)
        interpolation = [node for node in self.expressions.values() if node['kind'] == 'stringTemplate' and '$local' in node['text']]
        self.assertEqual(1, len(interpolation))
        self.assertEqual(3, len(interpolation[0]['children']))
        self.assertTrue(any(node.get('targetLabel') == 'block' for node in self.expressions.values()))
        deferred = next(node for node in self.declarations if node['name'] == 'deferred')
        self.assertIn(deferred['delegateId'], self.expressions)
        self.assertEqual(2, len([node for node in self.declarations if node['kind'] == 'accessor']))

    def test_every_expression_range_and_executable_root_is_exact(self):
        self.assertEqual('unicode-code-points', self.file['offsetEncoding'])
        for expression in self.expressions.values():
            self.assertEqual(expression['text'], SOURCE[expression['start']:expression['end']])
        for declaration in self.declarations:
            if 'nameStart' in declaration:
                self.assertTrue(SOURCE[declaration['nameStart']:declaration['nameEnd']])
            for role in ('body', 'initializer', 'defaultValue', 'delegate', 'delegation'):
                if role + 'Id' in declaration:
                    expression = self.expressions[declaration[role + 'Id']]
                    self.assertEqual(declaration['id'], expression['ownerId'])

    def test_implicit_lambda_does_not_invent_it_parameter(self):
        closure = next(node for node in self.declarations if node['name'] == 'closure')
        expression = self.expressions[closure['initializerId']]
        declaration = self.by_id[expression['declarationId']]
        self.assertFalse(declaration['hasParameterSpecification'])
        self.assertTrue(declaration['implicitParameterCandidate'])
        self.assertEqual([], declaration['parameterIds'])

    def test_function_type_labels_are_not_runtime_parameter_bindings(self):
        label = next(node for node in self.declarations if node['name'] == 'description')
        self.assertTrue(label['typeOnly'])
        self.assertEqual('(description: String) -> String', SOURCE[label['lexicalScopeStart']:label['lexicalScopeEnd']])
        actual = [node for node in self.declarations if node['name'] == 'value' and node['kind'] == 'parameter']
        self.assertTrue(actual)
        self.assertTrue(all(not node['typeOnly'] for node in actual))

    def test_cache_invalidates_exact_source_without_changing_lexical_ids(self):
        changed = '\n' + SOURCE
        parsed = parse_sources([{'path': 'fixtures/Syntax.kt', 'source': changed}])['files'][0]
        self.assertNotEqual(self.file['sourceHash'], parsed['sourceHash'])
        self.assertEqual([node['id'] for node in self.declarations], [node['id'] for node in parsed['declarations']])
        for before, after in zip(self.declarations, parsed['declarations']):
            self.assertEqual(before['start'] + 1, after['start'])
        repeated = parse_sources([{'path': 'fixtures/Syntax.kt', 'source': SOURCE}])
        self.assertEqual(self.result, repeated)

    def test_syntax_errors_are_explicit_and_tests_are_excluded(self):
        parsed = parse_sources([
            {'path': 'fixtures/Broken.kt', 'source': 'fun broken( {'},
            {'path': 'fixtures/Skipped.kt', 'source': 'fun ignored() {}', 'test': True},
        ])
        self.assertEqual(1, len(parsed['files']))
        self.assertTrue(parsed['files'][0]['errors'])


if __name__ == '__main__':
    unittest.main()
