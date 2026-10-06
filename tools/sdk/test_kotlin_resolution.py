"""Source-level safety fixtures for the PSI-backed explorer resolver."""
import re
import unittest

from kotlin_graph import augment_graph
from kotlin_psi import parse_sources


CONTEXTS = ['tmdb-android', 'tmdb-web', 'clientb-android', 'clientb-web']
JOURNEYS = ['construction', 'auth', 'profiles', 'home', 'details', 'search', 'library', 'playback', 'storage', 'lifecycle']
PREFIX = 'sdk/resolution-fixture/src/commonMain/kotlin/'


def build(sources):
    inventory = [{'path': path if path.startswith('sdk/') else PREFIX + path,
                  'source': source, 'module': ':sdk:resolution-fixture',
                  'sourceSet': 'androidMain' if '/androidMain/' in path else 'wasmJsMain' if '/wasmJsMain/' in path else 'commonMain',
                  'test': False, 'binary': False}
                 for path, source in sources.items()]
    parsed = parse_sources(inventory)
    errors = [(file['path'], file['errors']) for file in parsed['files'] if file['errors']]
    if errors:
        raise AssertionError(errors)
    graph = augment_graph({'nodes': [], 'edges': [], 'coverage': []}, inventory, parsed, CONTEXTS, JOURNEYS)
    declarations = {node['id']: node for file in parsed['files'] for node in file['declarations']}
    return graph, declarations


def normalized(value):
    return re.sub(r'\s+', ' ', value).strip()


def calls(graph, expression):
    return [node for node in graph['nodes']
            if node.get('expressionKind') == 'call' and node.get('expressionText') == normalized(expression)]


def declared(graph, declarations, name, kind=None, owner_name=None):
    ids = {identity for identity, node in declarations.items()
           if node['name'] == name and (kind is None or node['kind'] == kind)
           and (owner_name is None or declarations.get(node.get('ownerId'), {}).get('name') == owner_name)}
    return [node for node in graph['nodes'] if node.get('declarationId') in ids]


def identity_reachable(graph, source, target):
    seen = {source}
    pending = [source]
    while pending:
        current = pending.pop()
        for edge in graph['edges']:
            if edge['from'] == current and edge['kind'] in {'passes', 'aliases', 'returns'} and edge.get('upstream', True):
                if edge['to'] == target:
                    return True
                if edge['to'] not in seen:
                    seen.add(edge['to'])
                    pending.append(edge['to'])
    return False


class KotlinResolutionTest(unittest.TestCase):
    def assert_unresolved(self, graph, expression):
        found = calls(graph, expression)
        self.assertEqual(1, len(found), expression)
        self.assertNotEqual('resolved', found[0].get('resolution'), expression)
        self.assertNotIn('calleeId', found[0], expression)

    def test_unimported_and_private_foreign_declarations_do_not_bind(self):
        graph, _ = build({
            'a/Foreign.kt': '''package a
fun foreignFactory(): String = "a"
class ForeignType(val value: String)
private fun hiddenFactory(): String = "private"
private class HiddenType
''',
            'b/Caller.kt': '''package b
import a.hiddenFactory
import a.HiddenType
fun caller() {
    foreignFactory()
    ForeignType("x")
    hiddenFactory()
    HiddenType()
}
''',
        })
        for expression in ['foreignFactory()', 'ForeignType("x")', 'hiddenFactory()', 'HiddenType()']:
            self.assert_unresolved(graph, expression)

    def test_local_function_does_not_escape_its_lexical_owner(self):
        graph, _ = build({'Locals.kt': '''package fixture
fun owner(): String {
    fun localOnly(value: String): String = value
    return localOnly("inside")
}
fun outsider(): String = localOnly("outside")
'''})
        self.assertEqual('resolved', calls(graph, 'localOnly("inside")')[0]['resolution'])
        self.assert_unresolved(graph, 'localOnly("outside")')

    def test_alias_imports_resolve_call_and_receiver_type(self):
        graph, declarations = build({
            'a/Exported.kt': '''package a
fun exported(value: String): String = value
class ExportedType(val value: String)
''',
            'b/Imported.kt': '''package b
import a.exported as renamed
import a.ExportedType as RenamedType
fun caller(value: String): String = renamed(value)
fun constructor(value: String): RenamedType = RenamedType(value)
fun read(input: RenamedType): String = input.value
''',
        })
        for expression in ['renamed(value)', 'RenamedType(value)']:
            self.assertEqual('resolved', calls(graph, expression)[0]['resolution'])
        projections = [node for node in graph['nodes'] if node.get('fieldPath') == 'value' and node.get('label') == 'input.value']
        self.assertEqual(1, len(projections))
        self.assertEqual('resolved', projections[0]['resolution'])
        self.assertIn(projections[0].get('fieldDeclarationId'), declarations)

    def test_platform_and_provider_contexts_do_not_cross_bind(self):
        graph, _ = build({
            'sdk/providers/tmdb/src/androidMain/kotlin/OnlyAndroid.kt': '''package shared
fun onlyAndroid(): String = "android"
class OnlyAndroidType(val value: String)
''',
            'sdk/providers/clientB/src/wasmJsMain/kotlin/WebCaller.kt': '''package shared
fun caller() {
    onlyAndroid()
    OnlyAndroidType("web")
}
''',
        })
        self.assert_unresolved(graph, 'onlyAndroid()')
        self.assert_unresolved(graph, 'OnlyAndroidType("web")')

    def test_explicit_this_field_has_real_property_evidence(self):
        graph, declarations = build({'This.kt': '''package fixture
class Holder(val value: String) {
    fun read(): String = this.value
}
'''})
        value = declared(graph, declarations, 'value', 'parameter')[0]
        projections = [node for node in graph['nodes'] if node.get('fieldPath') == 'value']
        self.assertEqual(1, len(projections))
        self.assertEqual('resolved', projections[0]['resolution'])
        self.assertEqual(value['declarationId'], projections[0].get('fieldDeclarationId'))

    def test_catch_finally_and_condition_calls_remain_indexed(self):
        graph, _ = build({'Cleanup.kt': '''package fixture
fun work() {}
fun recover(message: String) {}
fun cleanup() {}
fun allowed(): Boolean = true
fun guarded() {
    try { if (allowed()) work() }
    catch (failure: Exception) { recover("failed") }
    finally { cleanup() }
}
'''})
        for expression in ['allowed()', 'work()', 'recover("failed")', 'cleanup()']:
            found = calls(graph, expression)
            self.assertEqual(1, len(found), expression)
            self.assertEqual('resolved', found[0]['resolution'], expression)

    def test_non_property_constructor_parameter_is_not_field_identity(self):
        graph, declarations = build({'Transform.kt': '''package fixture
class Adjusted(raw: Int) { val raw = raw + 1 }
fun caller(input: Int): Int {
    val adjusted = Adjusted(input)
    return adjusted.raw
}
'''})
        source = declared(graph, declarations, 'input', 'parameter')[0]
        projections = [node for node in graph['nodes'] if node.get('fieldPath') == 'raw' and node.get('label') == 'adjusted.raw']
        self.assertEqual(1, len(projections))
        self.assertFalse(identity_reachable(graph, source['id'], projections[0]['id']),
                         'Adjusted.raw is computed from raw + 1, not an alias of the constructor argument.')

    def test_shadowing_initializer_reads_outer_variable(self):
        graph, declarations = build({'Shadow.kt': '''package fixture
fun shadow(seed: Int): Int {
    val x = seed
    if (seed > 0) {
        val x = x + 1
        return x
    }
    return x
}
'''})
        locals = sorted(declared(graph, declarations, 'x', 'local'),
                        key=lambda node: declarations[node['declarationId']]['start'])
        self.assertEqual(2, len(locals))
        operation = next(node for node in graph['nodes'] if node.get('expressionText') == 'x + 1')
        inputs = {edge['from'] for edge in graph['edges'] if edge['to'] == operation['id']}
        self.assertIn(locals[0]['id'], inputs)
        self.assertNotIn(locals[1]['id'], inputs)

    def test_named_positional_default_and_ambiguous_arguments(self):
        graph, declarations = build({'Arguments.kt': '''package fixture
fun combine(first: String, second: String = "fallback"): String = first + second
fun gather(vararg values: Int): Int = 0
fun pick(value: Int): Int = value
fun pick(value: String): String = value
fun caller(input: String) {
    combine(second = "override", first = input)
    combine(input)
    combine(unknown = input)
    gather(1, 2, 3)
    pick(unknownValue)
}
'''})
        for expression in ['combine(second = "override", first = input)', 'combine(input)']:
            self.assertEqual('resolved', calls(graph, expression)[0]['resolution'])
        first = declared(graph, declarations, 'first', 'parameter')[0]
        second = declared(graph, declarations, 'second', 'parameter')[0]
        bindings = [edge for edge in graph['edges'] if edge['kind'] == 'passes']
        self.assertTrue(any(edge['to'] == first['id'] and edge.get('argumentName') == 'first'
                            and edge.get('argumentExpression') == 'input' for edge in bindings))
        self.assertTrue(any(edge['to'] == second['id'] and edge.get('argumentExpression') == '"override"' for edge in bindings))
        self.assertTrue(any(edge['to'] == second['id'] and edge.get('argumentExpression') == '"fallback"' for edge in bindings))
        self.assert_unresolved(graph, 'combine(unknown = input)')
        self.assert_unresolved(graph, 'pick(unknownValue)')
        vararg = calls(graph, 'gather(1, 2, 3)')[0]
        if vararg.get('resolution') == 'resolved':
            values = declared(graph, declarations, 'values', 'parameter')[0]
            actuals = {edge.get('argumentExpression') for edge in bindings if edge['to'] == values['id']}
            self.assertTrue({'1', '2', '3'} <= actuals, 'Resolved varargs must retain every argument, not just one.')
        else:
            self.assertNotIn('calleeId', vararg)


if __name__ == '__main__':
    unittest.main()
