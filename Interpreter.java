import java.util.*;

/**
 * BONUS PHASE: INTERPRETER / EXECUTOR
 *
 * Walks the AST and actually executes the program, producing real output.
 * This simulates what the compiled program would do when run.
 */
public class Interpreter {

    private final Deque<Map<String, Object>> envStack = new ArrayDeque<>();
    private final List<String> output = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public List<String> run(ASTNode node) {
        envStack.push(new LinkedHashMap<>());
        try { exec(node); }
        catch (RuntimeException e) { errors.add("Runtime error: " + e.getMessage()); }
        return output;
    }

    private void exec(ASTNode node) {
        if (node instanceof ASTNode.Program p) {
            for (ASTNode s : p.statements) exec(s);
        } else if (node instanceof ASTNode.Block b) {
            envStack.push(new LinkedHashMap<>());
            for (ASTNode s : b.statements) exec(s);
            envStack.pop();
        } else if (node instanceof ASTNode.VarDecl vd) {
            Object val = vd.initializer != null ? eval(vd.initializer) : defaultValue(vd.type);
            envStack.peek().put(vd.name, val);
        } else if (node instanceof ASTNode.Assign a) {
            Object val = eval(a.value);
            setVar(a.name, val);
        } else if (node instanceof ASTNode.IfStmt ifNode) {
            Object cond = eval(ifNode.condition);
            if (isTruthy(cond)) exec(ifNode.thenBranch);
            else if (ifNode.elseBranch != null) exec(ifNode.elseBranch);
        } else if (node instanceof ASTNode.WhileStmt ws) {
            int iterations = 0;
            while (isTruthy(eval(ws.condition))) {
                exec(ws.body);
                if (++iterations > 10000) { errors.add("Infinite loop detected — stopped at 10000 iterations"); break; }
            }
        } else if (node instanceof ASTNode.PrintStmt ps) {
            Object val = eval(ps.expr);
            output.add(formatValue(val));
        } else if (node instanceof ASTNode.ReturnStmt rs) {
            // simplified: just evaluate
            if (rs.expr != null) eval(rs.expr);
        }
    }

    private Object eval(ASTNode node) {
        if (node instanceof ASTNode.NumberLit nl) {
            String v = nl.value;
            try { return v.contains(".") ? Double.parseDouble(v) : Integer.parseInt(v); }
            catch (NumberFormatException e) { return 0; }
        }
        if (node instanceof ASTNode.StringLit sl) return sl.value;
        if (node instanceof ASTNode.BoolLit bl)   return bl.value;
        if (node instanceof ASTNode.VarRef vr)     return getVar(vr.name);

        if (node instanceof ASTNode.UnaryExpr ue) {
            Object operand = eval(ue.operand);
            return switch (ue.op) {
                case "-" -> negate(operand);
                case "!" -> !isTruthy(operand);
                default  -> operand;
            };
        }

        if (node instanceof ASTNode.BinaryExpr be) {
            // Short-circuit for logical
            if (be.op.equals("&&")) return isTruthy(eval(be.left)) && isTruthy(eval(be.right));
            if (be.op.equals("||")) return isTruthy(eval(be.left)) || isTruthy(eval(be.right));

            Object left  = eval(be.left);
            Object right = eval(be.right);
            return applyOp(be.op, left, right);
        }
        return null;
    }

    private Object applyOp(String op, Object l, Object r) {
        // String concatenation
        if (op.equals("+") && (l instanceof String || r instanceof String))
            return formatValue(l) + formatValue(r);

        double lv = toDouble(l), rv = toDouble(r);
        return switch (op) {
            case "+"  -> isInt(l, r) ? (int)(lv + rv) : (lv + rv);
            case "-"  -> isInt(l, r) ? (int)(lv - rv) : (lv - rv);
            case "*"  -> isInt(l, r) ? (int)(lv * rv) : (lv * rv);
            case "/"  -> {
                if (rv == 0) { errors.add("Division by zero"); yield 0; }
                yield isInt(l, r) ? (int)(lv / rv) : (lv / rv);
            }
            case "%"  -> isInt(l, r) ? (int)(lv % rv) : (lv % rv);
            case "==" -> lv == rv || (l instanceof Boolean b && b.equals(r));
            case "!=" -> lv != rv;
            case "<"  -> lv < rv;
            case ">"  -> lv > rv;
            case "<=" -> lv <= rv;
            case ">=" -> lv >= rv;
            default   -> null;
        };
    }

    private Object negate(Object v) {
        if (v instanceof Integer i) return -i;
        if (v instanceof Double d)  return -d;
        return v;
    }

    private boolean isTruthy(Object v) {
        if (v == null)          return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Integer i) return i != 0;
        if (v instanceof Double d)  return d != 0;
        if (v instanceof String s)  return !s.isEmpty();
        return true;
    }

    private boolean isInt(Object l, Object r) {
        return l instanceof Integer && r instanceof Integer;
    }

    private double toDouble(Object v) {
        if (v instanceof Integer i) return i;
        if (v instanceof Double d)  return d;
        if (v instanceof Boolean b) return b ? 1 : 0;
        return 0;
    }

    private String formatValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Double d && d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf(d.intValue());
        return String.valueOf(v);
    }

    private Object getVar(String name) {
        for (Map<String, Object> env : envStack) {
            if (env.containsKey(name)) return env.get(name);
        }
        errors.add("Undefined variable: " + name);
        return 0;
    }

    private void setVar(String name, Object val) {
        for (Map<String, Object> env : envStack) {
            if (env.containsKey(name)) { env.put(name, val); return; }
        }
        errors.add("Assignment to undeclared variable: " + name);
    }

    private Object defaultValue(String type) {
        return switch (type) {
            case "int"    -> 0;
            case "float"  -> 0.0;
            case "string" -> "";
            case "bool"   -> false;
            default       -> null;
        };
    }

    public List<String> getErrors() { return errors; }
}
