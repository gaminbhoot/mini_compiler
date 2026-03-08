import java.util.*;

/**
 * PHASE 3: SEMANTIC ANALYSIS
 *
 * Checks that the program is MEANINGFUL, not just syntactically valid.
 * Responsibilities:
 *   - Type checking (can't add string + number)
 *   - Undeclared variable detection (using x before declaring it)
 *   - Re-declaration detection (declaring x twice in same scope)
 *   - Symbol Table construction (tracks all declared variables & their types)
 *
 * Example errors caught:
 *   int x = 5;
 *   int x = 10;   // ERROR: x already declared
 *   print(y);     // ERROR: y is not declared
 */
public class SemanticAnalyzer {

    /** Symbol Table: maps variable names → declared types */
    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    private final List<String> errors   = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> symbolLog = new ArrayList<>();

    public void analyze(ASTNode node) {
        pushScope(); // global scope
        visit(node);
        popScope();
    }

    // ─── Scope Management ──────────────────────────────────────────────────────

    private void pushScope() { scopeStack.push(new LinkedHashMap<>()); }

    private void popScope() {
        Map<String, String> scope = scopeStack.pop();
        if (!scope.isEmpty()) {
            scope.forEach((name, type) ->
                symbolLog.add("  [scope exit] " + type + " " + name)
            );
        }
    }

    private void declare(String name, String type, int line) {
        Map<String, String> current = scopeStack.peek();
        if (current.containsKey(name)) {
            errors.add("Line " + line + ": Variable '" + name + "' already declared in this scope");
        } else {
            current.put(name, type);
            symbolLog.add("  Declared: " + type + " " + name);
        }
    }

    private String lookup(String name) {
        for (Map<String, String> scope : scopeStack) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    // ─── Visitor ───────────────────────────────────────────────────────────────

    private String visit(ASTNode node) {
        if (node == null) return "void";

        if (node instanceof ASTNode.Program p) {
            for (ASTNode s : p.statements) visit(s);
            return "void";
        }

        if (node instanceof ASTNode.Block b) {
            pushScope();
            for (ASTNode s : b.statements) visit(s);
            popScope();
            return "void";
        }

        if (node instanceof ASTNode.VarDecl vd) {
            String initType = vd.initializer != null ? visit(vd.initializer) : vd.type;
            if (vd.initializer != null && !typesCompatible(vd.type, initType)) {
                errors.add("Type mismatch: cannot assign '" + initType + "' to '" + vd.type + "' variable '" + vd.name + "'");
            }
            declare(vd.name, vd.type, 0);
            return "void";
        }

        if (node instanceof ASTNode.Assign a) {
            String declaredType = lookup(a.name);
            if (declaredType == null) {
                errors.add("Variable '" + a.name + "' is not declared");
            } else {
                String valType = visit(a.value);
                if (!typesCompatible(declaredType, valType)) {
                    errors.add("Type mismatch: cannot assign '" + valType + "' to '" + declaredType + "' variable '" + a.name + "'");
                }
            }
            return "void";
        }

        if (node instanceof ASTNode.IfStmt ifNode) {
            String condType = visit(ifNode.condition);
            if (!condType.equals("bool") && !condType.equals("int") && !condType.equals("unknown")) {
                warnings.add("If condition should be boolean, got '" + condType + "'");
            }
            visit(ifNode.thenBranch);
            if (ifNode.elseBranch != null) visit(ifNode.elseBranch);
            return "void";
        }

        if (node instanceof ASTNode.WhileStmt ws) {
            visit(ws.condition);
            visit(ws.body);
            return "void";
        }

        if (node instanceof ASTNode.PrintStmt ps) {
            visit(ps.expr);
            return "void";
        }

        if (node instanceof ASTNode.ReturnStmt rs) {
            if (rs.expr != null) visit(rs.expr);
            return "void";
        }

        if (node instanceof ASTNode.BinaryExpr be) {
            String leftType  = visit(be.left);
            String rightType = visit(be.right);
            return inferBinaryType(be.op, leftType, rightType);
        }

        if (node instanceof ASTNode.UnaryExpr ue) {
            String opType = visit(ue.operand);
            if (ue.op.equals("!") && !opType.equals("bool") && !opType.equals("unknown"))
                warnings.add("'!' operator applied to non-boolean type '" + opType + "'");
            return ue.op.equals("!") ? "bool" : opType;
        }

        if (node instanceof ASTNode.NumberLit nl) {
            return nl.value.contains(".") ? "float" : "int";
        }

        if (node instanceof ASTNode.StringLit) { return "string"; }
        if (node instanceof ASTNode.BoolLit)   { return "bool"; }

        if (node instanceof ASTNode.VarRef vr) {
            String type = lookup(vr.name);
            if (type == null) {
                errors.add("Variable '" + vr.name + "' is not declared");
                return "unknown";
            }
            return type;
        }

        return "unknown";
    }

    private boolean typesCompatible(String declared, String actual) {
        if (declared.equals(actual)) return true;
        if (declared.equals("float") && actual.equals("int")) return true;
        if (actual.equals("unknown")) return true;
        return false;
    }

    private String inferBinaryType(String op, String left, String right) {
        Set<String> relational = Set.of("==", "!=", "<", ">", "<=", ">=", "&&", "||");
        if (relational.contains(op)) return "bool";
        if (left.equals("string") || right.equals("string")) {
            if (op.equals("+")) return "string";
            errors.add("Operator '" + op + "' cannot be applied to string type");
            return "unknown";
        }
        if (left.equals("float") || right.equals("float")) return "float";
        return "int";
    }

    // ─── Symbol Table Dump ─────────────────────────────────────────────────────

    public List<String> getSymbolLog()  { return symbolLog; }
    public List<String> getErrors()     { return errors; }
    public List<String> getWarnings()   { return warnings; }
}
