import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

/** Offline syntax index. No compiler analysis, application execution, or dependency resolution. */
public final class KotlinSourceIndex {
    private final String path;
    private final String source;
    private final KtFile file;
    private final int[] sourceOffsets;
    private final List<Map<String, Object>> declarations = new ArrayList<>();
    private final List<Map<String, Object>> roots = new ArrayList<>();
    private final List<Map<String, Object>> errors = new ArrayList<>();
    private final IdentityHashMap<PsiElement, Map<String, Object>> declarationByPsi = new IdentityHashMap<>();
    private final IdentityHashMap<PsiElement, Map<String, Object>> expressionByPsi = new IdentityHashMap<>();
    private final Map<String, Integer> ordinals = new LinkedHashMap<>();
    private final List<PsiElement> declarationElements = new ArrayList<>();

    private KotlinSourceIndex(String path, String source, KtFile file) {
        this.path = path;
        this.source = source;
        this.file = file;
        this.sourceOffsets = new int[source.length() + 1];
        int points = 0;
        for (int offset = 0; offset < source.length();) {
            int width = Character.charCount(source.codePointAt(offset));
            points++;
            for (int i = 1; i <= width; i++) sourceOffsets[offset + i] = points;
            offset += width;
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected the base64 source manifest path.");
        }
        Disposable disposable = Disposer.newDisposable("streamcore-source-index");
        try {
            CompilerConfiguration configuration = new CompilerConfiguration();
            configuration.put(CommonConfigurationKeys.MODULE_NAME, "streamcore-source-index");
            KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForProduction(
                    disposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
            KtPsiFactory factory = new KtPsiFactory(environment.getProject(), false);
            List<Map<String, Object>> indexedFiles = new ArrayList<>();
            Base64.Decoder decoder = Base64.getDecoder();
            try (BufferedReader input = Files.newBufferedReader(Path.of(args[0]), StandardCharsets.UTF_8)) {
                String line;
                while ((line = input.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    String[] fields = line.split("\t", -1);
                    if (fields.length != 2) throw new IOException("Invalid source manifest row.");
                    String path = new String(decoder.decode(fields[0]), StandardCharsets.UTF_8);
                    String source = new String(decoder.decode(fields[1]), StandardCharsets.UTF_8);
                    KtFile parsed = factory.createFile(Path.of(path).getFileName().toString(), source);
                    indexedFiles.add(new KotlinSourceIndex(path, source, parsed).index());
                }
            }
            System.out.println(json(map("schemaVersion", 1, "compilerVersion", "2.3.21", "files", indexedFiles)));
        } finally {
            Disposer.dispose(disposable);
        }
    }

    private Map<String, Object> index() {
        collectDeclarations(file);
        // Every declaration is known before expressions refer to nested/local declarations.
        for (PsiElement element : declarationElements) populateDeclaration(element);
        List<Map<String, Object>> imports = new ArrayList<>();
        for (KtImportDirective directive : file.getImportDirectives()) {
            imports.add(map("path", directive.getImportedFqName() == null ? "" : directive.getImportedFqName().asString(),
                    "alias", directive.getAliasName(), "allUnder", directive.isAllUnder(),
                    "start", start(directive), "end", end(directive)));
        }
        return map("path", path, "package", file.getPackageFqName().asString(), "imports", imports,
                "declarations", declarations, "expressions", roots, "errors", errors,
                "length", sourceOffsets[source.length()], "offsetEncoding", "unicode-code-points");
    }

    private void collectDeclarations(PsiElement element) {
        if (element instanceof PsiErrorElement error) {
            errors.add(map("message", error.getErrorDescription(), "start", start(error), "end", end(error)));
        }
        String kind = declarationKind(element);
        if (kind != null) {
            Map<String, Object> parent = nearestDeclaration(element.getParent());
            String name = declarationName(element, kind);
            String segment = declarationSegment(element, kind, name);
            String ownerSymbol = parent == null ? "" : (String) parent.get("symbol");
            String symbolBase = ownerSymbol.isEmpty() ? segment : ownerSymbol + "/" + segment;
            int ordinal = ordinals.merge(symbolBase, 1, Integer::sum);
            String symbol = symbolBase + (ordinal == 1 ? "" : "~" + ordinal);
            Map<String, Object> node = map("id", path + "#" + symbol, "name", name, "kind", kind,
                    "symbol", symbol, "ownerId", parent == null ? null : parent.get("id"),
                    "start", start(element), "end", end(element), "parameterIds", new ArrayList<String>(),
                    "modifiers", modifiers(element), "typeName", null, "returnType", null);
            if (element instanceof KtNamedDeclaration named && named.getNameIdentifier() != null) {
                node.put("nameStart", start(named.getNameIdentifier()));
                node.put("nameEnd", end(named.getNameIdentifier()));
            }
            PsiElement scope = lexicalScope(element);
            node.put("lexicalScopeStart", start(scope));
            node.put("lexicalScopeEnd", end(scope));
            declarationByPsi.put(element, node);
            declarationElements.add(element);
            declarations.add(node);
        }
        for (PsiElement child : element.getChildren()) collectDeclarations(child);
    }

    private static String declarationKind(PsiElement element) {
        if (element instanceof KtEnumEntry) return "enumEntry";
        if (element instanceof KtObjectDeclaration) return "object";
        if (element instanceof KtClass klass) return klass.isInterface() ? "interface" : klass.isEnum() ? "enum" : "class";
        if (element instanceof KtConstructor<?>) return "constructor";
        if (element instanceof KtNamedFunction) return "function";
        if (element instanceof KtFunctionLiteral) return "lambda";
        if (element instanceof KtParameter) return "parameter";
        if (element instanceof KtProperty property) return property.isLocal() ? "local" : "property";
        if (element instanceof KtPropertyAccessor) return "accessor";
        if (element instanceof KtTypeAlias) return "typeAlias";
        if (element instanceof KtTypeParameter) return "typeParameter";
        if (element instanceof KtDestructuringDeclaration) return "destructuring";
        if (element instanceof KtDestructuringDeclarationEntry) return "destructuringEntry";
        if (element instanceof KtClassInitializer) return "initializer";
        return null;
    }

    private static String declarationName(PsiElement element, String kind) {
        if (element instanceof KtNamedDeclaration named && named.getName() != null) return named.getName();
        if (element instanceof KtPropertyAccessor accessor) return accessor.isGetter() ? "get" : "set";
        if (element instanceof KtPrimaryConstructor) return "<init>";
        if (element instanceof KtSecondaryConstructor) return "<init>";
        if (element instanceof KtObjectDeclaration object && object.isCompanion()) return "Companion";
        return "<" + kind + ">";
    }

    private static String declarationSegment(PsiElement element, String kind, String name) {
        List<KtParameter> parameters = parameters(element);
        if (element instanceof KtNamedFunction function) {
            String receiver = typeText(function.getReceiverTypeReference());
            return (receiver == null ? "" : receiver + ".") + name + signature(parameters);
        }
        if (element instanceof KtConstructor<?>) return name + signature(parameters);
        if (element instanceof KtPropertyAccessor) return name + signature(parameters);
        if (kind.equals("parameter")) return "parameter:" + name;
        if (kind.equals("typeParameter")) return "typeParameter:" + name;
        if (kind.equals("local") || kind.equals("property")) return kind + ":" + name;
        return name;
    }

    private static String signature(List<KtParameter> parameters) {
        List<String> types = new ArrayList<>();
        for (KtParameter parameter : parameters) {
            String type = typeText(parameter.getTypeReference());
            types.add((parameter.isVarArg() ? "vararg " : "") + (type == null ? "?" : type.replaceAll("\\s+", " ")));
        }
        return "(" + String.join(",", types) + ")";
    }

    private void populateDeclaration(PsiElement element) {
        Map<String, Object> node = declarationByPsi.get(element);
        List<String> parameterIds = new ArrayList<>();
        for (KtParameter parameter : parameters(element)) parameterIds.add(id(parameter));
        node.put("parameterIds", parameterIds);
        if (element instanceof KtClassOrObject klass) {
            List<String> constructors = new ArrayList<>();
            if (klass.getPrimaryConstructor() != null) constructors.add(id(klass.getPrimaryConstructor()));
            for (KtSecondaryConstructor constructor : klass.getSecondaryConstructors()) constructors.add(id(constructor));
            node.put("constructorIds", constructors);
            node.put("primaryConstructorId", klass.getPrimaryConstructor() == null ? null : id(klass.getPrimaryConstructor()));
            List<String> superTypes = new ArrayList<>();
            for (KtSuperTypeListEntry entry : klass.getSuperTypeListEntries()) {
                if (entry.getTypeReference() != null) superTypes.add(entry.getTypeReference().getText());
                if (entry instanceof KtSuperTypeCallEntry call) {
                    Map<String, Object> expression = expression(call, (String) node.get("id"));
                    roots.add(expression);
                }
            }
            node.put("superTypes", superTypes);
            node.put("companion", klass instanceof KtObjectDeclaration object && object.isCompanion());
        }
        if (element instanceof KtNamedFunction function) {
            node.put("returnType", typeText(function.getTypeReference()));
            node.put("receiverType", typeText(function.getReceiverTypeReference()));
            node.put("bodyStyle", function.hasBlockBody() ? "block" : "expression");
            attachRoot(node, "body", function.getBodyExpression());
        } else if (element instanceof KtConstructor<?> constructor) {
            node.put("primary", constructor instanceof KtPrimaryConstructor);
            attachRoot(node, "body", constructor.getBodyExpression());
            if (constructor instanceof KtSecondaryConstructor secondary) attachRoot(node, "delegation", secondary.getDelegationCall());
        } else if (element instanceof KtFunctionLiteral literal) {
            node.put("hasParameterSpecification", literal.hasParameterSpecification());
            node.put("implicitParameterCandidate", !literal.hasParameterSpecification());
            attachRoot(node, "body", literal.getBodyExpression());
        } else if (element instanceof KtParameter parameter) {
            node.put("typeName", typeText(parameter.getTypeReference()));
            node.put("typeOnly", isFunctionTypeParameter(parameter));
            node.put("propertyParameter", parameter.hasValOrVar());
            node.put("mutable", parameter.isMutable());
            node.put("vararg", parameter.isVarArg());
            node.put("hasDefaultValue", parameter.hasDefaultValue());
            node.put("defaultValue", text(parameter.getDefaultValue()));
            attachRoot(node, "defaultValue", parameter.getDefaultValue());
            KtDestructuringDeclaration destructuring = parameter.getDestructuringDeclaration();
            if (destructuring != null) node.put("destructuringId", id(destructuring));
        } else if (element instanceof KtProperty property) {
            node.put("typeName", typeText(property.getTypeReference()));
            node.put("receiverType", typeText(property.getReceiverTypeReference()));
            node.put("mutable", property.isVar());
            node.put("local", property.isLocal());
            node.put("initializer", text(property.getInitializer()));
            node.put("delegate", text(property.getDelegateExpression()));
            attachRoot(node, "initializer", property.getInitializer());
            attachRoot(node, "delegate", property.getDelegateExpression());
            List<String> accessors = new ArrayList<>();
            for (KtPropertyAccessor accessor : property.getAccessors()) accessors.add(id(accessor));
            node.put("accessorIds", accessors);
        } else if (element instanceof KtPropertyAccessor accessor) {
            node.put("returnType", typeText(accessor.getTypeReference()));
            attachRoot(node, "body", accessor.getBodyExpression());
        } else if (element instanceof KtTypeAlias alias) {
            node.put("typeName", typeText(alias.getTypeReference()));
        } else if (element instanceof KtTypeParameter parameter) {
            node.put("typeName", typeText(parameter.getExtendsBound()));
        } else if (element instanceof KtDestructuringDeclaration destructuring) {
            node.put("mutable", destructuring.isVar());
            List<String> entries = new ArrayList<>();
            for (KtDestructuringDeclarationEntry entry : destructuring.getEntries()) entries.add(id(entry));
            node.put("entryIds", entries);
            attachRoot(node, "initializer", destructuring.getInitializer());
        } else if (element instanceof KtDestructuringDeclarationEntry entry) {
            node.put("typeName", typeText(entry.getTypeReference()));
        } else if (element instanceof KtClassInitializer initializer) {
            attachRoot(node, "body", initializer.getBody());
        }
    }

    private void attachRoot(Map<String, Object> declaration, String role, PsiElement element) {
        if (element == null) return;
        Map<String, Object> root = expression(element, (String) declaration.get("id"));
        declaration.put(role + "Id", root.get("id"));
        declaration.put(role + "Start", start(element));
        declaration.put(role + "End", end(element));
        root.put("rootRole", role);
        roots.add(root);
    }

    private Map<String, Object> expression(PsiElement element, String ownerId) {
        if (element == null) return null;
        Map<String, Object> cached = expressionByPsi.get(element);
        if (cached != null) return cached;
        Map<String, Object> out = map("id", path + "#expr:" + start(element) + ":" + end(element) + ":" + element.getClass().getSimpleName(),
                "kind", "other", "text", element.getText(), "start", start(element), "end", end(element), "ownerId", ownerId);
        expressionByPsi.put(element, out);
        List<Map<String, Object>> children = new ArrayList<>();
        if (element instanceof KtLambdaExpression lambda) {
            out.put("kind", "lambda");
            Map<String, Object> declaration = declarationByPsi.get(lambda.getFunctionLiteral());
            out.put("declarationId", declaration.get("id"));
            out.put("parameterIds", parameterIds(lambda.getValueParameters()));
            out.put("bodyId", expressionId(lambda.getBodyExpression()));
            out.put("hasParameterSpecification", lambda.getFunctionLiteral().hasParameterSpecification());
        } else if (declarationByPsi.containsKey(element)) {
            out.put("kind", "declaration");
            out.put("declarationId", declarationByPsi.get(element).get("id"));
        } else if (element instanceof KtCallExpression call) {
            out.put("kind", "call");
            out.put("name", call.getCalleeExpression() == null ? "" : call.getCalleeExpression().getText());
            putExpression(out, "callee", call.getCalleeExpression(), ownerId);
            // Kotlin PSI's valueArguments already includes trailing lambda arguments.
            out.put("arguments", arguments(call.getValueArguments(), ownerId));
            List<String> types = new ArrayList<>();
            for (KtTypeProjection type : call.getTypeArguments()) types.add(type.getText());
            out.put("typeArguments", types);
        } else if (element instanceof KtQualifiedExpression qualified) {
            out.put("kind", "qualified");
            out.put("safe", element instanceof KtSafeQualifiedExpression);
            putExpression(out, "receiver", qualified.getReceiverExpression(), ownerId);
            putExpression(out, "selector", qualified.getSelectorExpression(), ownerId);
        } else if (element instanceof KtNameReferenceExpression reference) {
            out.put("kind", "reference");
            out.put("name", reference.getReferencedName());
        } else if (element instanceof KtStringTemplateExpression template) {
            out.put("kind", "stringTemplate");
            for (KtStringTemplateEntry entry : template.getEntries()) {
                if (entry instanceof KtStringTemplateEntryWithExpression interpolation) {
                    for (KtExpression embedded : interpolation.getExpressions()) children.add(expression(embedded, ownerId));
                }
            }
        } else if (element instanceof KtConstantExpression) {
            out.put("kind", "literal");
        } else if (element instanceof KtBinaryExpression binary) {
            String operator = binary.getOperationReference().getText();
            out.put("kind", List.of("=", "+=", "-=", "*=", "/=", "%=").contains(operator) ? "assignment" : "binary");
            out.put("operator", operator);
            putExpression(out, "left", binary.getLeft(), ownerId);
            putExpression(out, "right", binary.getRight(), ownerId);
        } else if (element instanceof KtUnaryExpression unary) {
            out.put("kind", "unary");
            out.put("operator", unary.getOperationReference().getText());
            putExpression(out, "value", unary.getBaseExpression(), ownerId);
        } else if (element instanceof KtReturnExpression returned) {
            out.put("kind", "return");
            out.put("targetLabel", returned.getLabelName());
            putExpression(out, "value", returned.getReturnedExpression(), ownerId);
        } else if (element instanceof KtBlockExpression block) {
            out.put("kind", "block");
            for (KtExpression statement : block.getStatements()) children.add(expression(statement, ownerId));
        } else if (element instanceof KtIfExpression conditional) {
            out.put("kind", "if");
            putExpression(out, "condition", conditional.getCondition(), ownerId);
            putExpression(out, "thenBranch", conditional.getThen(), ownerId);
            putExpression(out, "elseBranch", conditional.getElse(), ownerId);
        } else if (element instanceof KtWhenExpression selection) {
            out.put("kind", "when");
            if (selection.getSubjectVariable() != null) out.put("subjectDeclarationId", id(selection.getSubjectVariable()));
            putExpression(out, "subject", selection.getSubjectExpression(), ownerId);
            List<Map<String, Object>> entries = new ArrayList<>();
            for (KtWhenEntry entry : selection.getEntries()) {
                List<Map<String, Object>> conditions = new ArrayList<>();
                for (KtWhenCondition condition : entry.getConditions()) conditions.add(expression(condition, ownerId));
                entries.add(map("else", entry.isElse(), "conditions", conditions,
                        "body", expression(entry.getExpression(), ownerId), "guard", expression(entry.getGuard(), ownerId),
                        "start", start(entry), "end", end(entry)));
            }
            out.put("entries", entries);
        } else if (element instanceof KtForExpression loop) {
            out.put("kind", "for");
            out.put("parameterId", loop.getLoopParameter() == null ? null : id(loop.getLoopParameter()));
            out.put("destructuringId", loop.getDestructuringDeclaration() == null ? null : id(loop.getDestructuringDeclaration()));
            putExpression(out, "range", loop.getLoopRange(), ownerId);
            putExpression(out, "body", loop.getBody(), ownerId);
        } else if (element instanceof KtWhileExpression loop) {
            out.put("kind", "while");
            putExpression(out, "condition", loop.getCondition(), ownerId);
            putExpression(out, "body", loop.getBody(), ownerId);
        } else if (element instanceof KtDoWhileExpression loop) {
            out.put("kind", "doWhile");
            putExpression(out, "condition", loop.getCondition(), ownerId);
            putExpression(out, "body", loop.getBody(), ownerId);
        } else if (element instanceof KtTryExpression attempt) {
            out.put("kind", "try");
            putExpression(out, "body", attempt.getTryBlock(), ownerId);
            List<Map<String, Object>> catches = new ArrayList<>();
            for (KtCatchClause clause : attempt.getCatchClauses()) {
                catches.add(map("parameterId", clause.getCatchParameter() == null ? null : id(clause.getCatchParameter()),
                        "body", expression(clause.getCatchBody(), ownerId), "start", start(clause), "end", end(clause)));
            }
            out.put("catches", catches);
            if (attempt.getFinallyBlock() != null) putExpression(out, "finally", attempt.getFinallyBlock().getFinalExpression(), ownerId);
        } else if (element instanceof KtParenthesizedExpression parentheses) {
            out.put("kind", "parenthesized");
            putExpression(out, "value", parentheses.getExpression(), ownerId);
        } else if (element instanceof KtLabeledExpression labeled) {
            out.put("kind", "labeled");
            out.put("label", labeled.getLabelName());
            putExpression(out, "value", labeled.getBaseExpression(), ownerId);
        } else if (element instanceof KtThisExpression self) {
            out.put("kind", "this"); out.put("targetLabel", self.getLabelName());
        } else if (element instanceof KtSuperExpression parent) {
            out.put("kind", "super"); out.put("targetLabel", parent.getLabelName());
        } else if (element instanceof KtCallableReferenceExpression reference) {
            out.put("kind", "callableReference");
            out.put("name", reference.getCallableReference().getReferencedName());
            putExpression(out, "receiver", reference.getReceiverExpression(), ownerId);
        } else if (element instanceof KtObjectLiteralExpression object) {
            out.put("kind", "object"); out.put("declarationId", id(object.getObjectDeclaration()));
        } else if (element instanceof KtArrayAccessExpression access) {
            out.put("kind", "index");
            putExpression(out, "receiver", access.getArrayExpression(), ownerId);
            for (KtExpression index : access.getIndexExpressions()) children.add(expression(index, ownerId));
        } else if (element instanceof KtBinaryExpressionWithTypeRHS cast) {
            out.put("kind", "cast"); out.put("typeName", typeText(cast.getRight()));
            out.put("operator", cast.getOperationReference().getText());
            putExpression(out, "value", cast.getLeft(), ownerId);
        } else if (element instanceof KtIsExpression check) {
            out.put("kind", "is"); out.put("typeName", typeText(check.getTypeReference()));
            out.put("negated", check.isNegated());
            putExpression(out, "value", check.getLeftHandSide(), ownerId);
        } else if (element instanceof KtConstructorDelegationCall call) {
            out.put("kind", "call"); out.put("name", call.isCallToThis() ? "this" : "super");
            out.put("constructorDelegation", true); out.put("arguments", arguments(call.getValueArguments(), ownerId));
        } else if (element instanceof KtSuperTypeCallEntry call) {
            out.put("kind", "call"); out.put("name", typeText(call.getTypeReference()));
            out.put("superConstructor", true); out.put("arguments", arguments(call.getValueArguments(), ownerId));
        } else {
            // Unknown syntax still retains direct expression children and source evidence.
            collectExpressionChildren(element, ownerId, children);
        }
        out.put("children", children);
        return out;
    }

    private void collectExpressionChildren(PsiElement element, String ownerId, List<Map<String, Object>> out) {
        for (PsiElement child : element.getChildren()) {
            if (child instanceof KtExpression || declarationByPsi.containsKey(child)) out.add(expression(child, ownerId));
            else if (child instanceof KtElement && !(child instanceof KtTypeReference)) collectExpressionChildren(child, ownerId, out);
        }
    }

    private List<Map<String, Object>> arguments(List<? extends ValueArgument> arguments, String ownerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (ValueArgument argument : arguments) {
            PsiElement element = argument.asElement();
            result.add(map("name", argument.getArgumentName() == null ? null : argument.getArgumentName().getAsName().asString(),
                    "spread", argument.getSpreadElement() != null, "trailingLambda", argument instanceof KtLambdaArgument,
                    "expression", expression(argument.getArgumentExpression(), ownerId), "start", start(element), "end", end(element)));
        }
        return result;
    }

    private void putExpression(Map<String, Object> output, String key, PsiElement child, String ownerId) {
        output.put(key, expression(child, ownerId));
    }

    private String expressionId(PsiElement element) {
        return element == null ? null : path + "#expr:" + start(element) + ":" + end(element) + ":" + element.getClass().getSimpleName();
    }

    private List<String> parameterIds(List<KtParameter> parameters) {
        List<String> ids = new ArrayList<>();
        for (KtParameter parameter : parameters) ids.add(id(parameter));
        return ids;
    }

    private static List<KtParameter> parameters(PsiElement element) {
        if (element instanceof KtFunction function) return function.getValueParameters();
        if (element instanceof KtClassOrObject klass) return klass.getPrimaryConstructorParameters();
        if (element instanceof KtPropertyAccessor accessor) return accessor.getValueParameters();
        return List.of();
    }

    private String id(PsiElement element) {
        Map<String, Object> declaration = declarationByPsi.get(element);
        return declaration == null ? null : (String) declaration.get("id");
    }

    private Map<String, Object> nearestDeclaration(PsiElement element) {
        for (PsiElement current = element; current != null; current = current.getParent()) {
            Map<String, Object> declaration = declarationByPsi.get(current);
            if (declaration != null) return declaration;
        }
        return null;
    }

    private PsiElement lexicalScope(PsiElement element) {
        for (PsiElement parent = element.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof KtFunctionType) return parent;
            if (parent instanceof KtPrimaryConstructor) {
                for (PsiElement outer = parent.getParent(); outer != null; outer = outer.getParent()) {
                    if (outer instanceof KtClassOrObject) return outer;
                }
            }
            if (parent instanceof KtBlockExpression || parent instanceof KtWhenExpression || parent instanceof KtForExpression
                    || parent instanceof KtCatchClause || parent instanceof KtIfExpression || parent instanceof KtFunctionLiteral
                    || parent instanceof KtNamedFunction || parent instanceof KtSecondaryConstructor || parent instanceof KtClassOrObject
                    || parent instanceof KtPropertyAccessor || parent instanceof KtFile) return parent;
        }
        return file;
    }

    private static boolean isFunctionTypeParameter(KtParameter parameter) {
        for (PsiElement parent = parameter.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof KtFunctionType) return true;
            if (parent instanceof KtDeclaration || parent instanceof KtFile) return false;
        }
        return false;
    }

    private static List<String> modifiers(PsiElement element) {
        List<String> modifiers = new ArrayList<>();
        if (element instanceof KtModifierListOwner owner) {
            for (KtModifierKeywordToken token : KtTokens.MODIFIER_KEYWORDS_ARRAY) {
                if (owner.hasModifier(token)) modifiers.add(token.getValue());
            }
        }
        return modifiers;
    }

    private static String typeText(KtTypeReference reference) { return reference == null ? null : reference.getText(); }
    private static String text(PsiElement element) { return element == null ? null : element.getText(); }
    private int start(PsiElement element) { return sourceOffsets[element.getTextRange().getStartOffset()]; }
    private int end(PsiElement element) { return sourceOffsets[element.getTextRange().getEndOffset()]; }

    private static Map<String, Object> map(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) map.put((String) entries[i], entries[i + 1]);
        return map;
    }

    private static String json(Object value) {
        StringBuilder output = new StringBuilder();
        appendJson(output, value);
        return output.toString();
    }

    private static void appendJson(StringBuilder output, Object value) {
        if (value == null) output.append("null");
        else if (value instanceof Number || value instanceof Boolean) output.append(value);
        else if (value instanceof Map<?, ?> values) {
            output.append('{'); boolean first = true;
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (!first) output.append(','); first = false;
                appendJson(output, entry.getKey().toString()); output.append(':'); appendJson(output, entry.getValue());
            }
            output.append('}');
        } else if (value instanceof Iterable<?> values) {
            output.append('['); boolean first = true;
            for (Object entry : values) { if (!first) output.append(','); first = false; appendJson(output, entry); }
            output.append(']');
        } else {
            output.append('"');
            for (char c : value.toString().toCharArray()) {
                switch (c) {
                    case '"' -> output.append("\\\"");
                    case '\\' -> output.append("\\\\");
                    case '\b' -> output.append("\\b");
                    case '\f' -> output.append("\\f");
                    case '\n' -> output.append("\\n");
                    case '\r' -> output.append("\\r");
                    case '\t' -> output.append("\\t");
                    default -> { if (c < 0x20) output.append(String.format("\\u%04x", (int) c)); else output.append(c); }
                }
            }
            output.append('"');
        }
    }
}
