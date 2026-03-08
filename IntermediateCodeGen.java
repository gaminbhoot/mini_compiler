import java.util.*;

/**
 * PHASE 4: INTERMEDIATE CODE GENERATION (Three-Address Code)
 *
 * Translates the AST into a platform-independent intermediate representation.
 * We use Three-Address Code (TAC): each instruction has at most 3 operands.
 *
 * TAC Instruction forms:
 *   t1 = a + b       (binary operation)
 *   t1 = -a          (unary operation)
 *   t1 = a           (copy)
 *   if t1 GOTO L1    (conditional jump)
 *   GOTO L1          (unconditional jump)
 *   LABEL L1         (label definition)
 *   PRINT t1         (output)
 *
 * Example for:  int x = 2 + 3 * 4;
 *   t0 = 3 * 4
 *   t1 = 2 + t0
 *   x = t1
 */
public class IntermediateCodeGen {

    private final List<String> code = new ArrayList<>();
    private int tempCount  = 0;
    private int labelCount = 0;

    private String newTemp()  { return "t" + tempCount++; }
    private String newLabel() { return "L" + labelCount++; }

    public List<String> generate(ASTNode node) {
        genNode(node);
        return code;
    }

    private String genNode(ASTNode node) {
        if (node == null) return "";

        if (node instanceof ASTNode.Program p) {
            for (ASTNode s : p.statements) genNode(s);
            return "";
        }

        if (node instanceof ASTNode.Block b) {
            for (ASTNode s : b.statements) genNode(s);
            return "";
        }

        if (node instanceof ASTNode.VarDecl vd) {
            if (vd.initializer != null) {
                String val = genNode(vd.initializer);
                emit(vd.name + " = " + val);
            }
            return vd.name;
        }

        if (node instanceof ASTNode.Assign a) {
            String val = genNode(a.value);
            emit(a.name + " = " + val);
            return a.name;
        }

        if (node instanceof ASTNode.BinaryExpr be) {
            String left  = genNode(be.left);
            String right = genNode(be.right);
            String temp  = newTemp();
            emit(temp + " = " + left + " " + be.op + " " + right);
            return temp;
        }

        if (node instanceof ASTNode.UnaryExpr ue) {
            String operand = genNode(ue.operand);
            String temp = newTemp();
            emit(temp + " = " + ue.op + operand);
            return temp;
        }

        if (node instanceof ASTNode.IfStmt ifNode) {
            String cond    = genNode(ifNode.condition);
            String labelTrue  = newLabel();
            String labelFalse = newLabel();
            String labelEnd   = newLabel();

            emit("if " + cond + " GOTO " + labelTrue);
            emit("GOTO " + labelFalse);
            emit("LABEL " + labelTrue + ":");
            genNode(ifNode.thenBranch);

            if (ifNode.elseBranch != null) {
                emit("GOTO " + labelEnd);
                emit("LABEL " + labelFalse + ":");
                genNode(ifNode.elseBranch);
                emit("LABEL " + labelEnd + ":");
            } else {
                emit("LABEL " + labelFalse + ":");
            }
            return "";
        }

        if (node instanceof ASTNode.WhileStmt ws) {
            String labelStart = newLabel();
            String labelEnd   = newLabel();

            emit("LABEL " + labelStart + ":");
            String cond = genNode(ws.condition);
            emit("if !" + cond + " GOTO " + labelEnd);
            genNode(ws.body);
            emit("GOTO " + labelStart);
            emit("LABEL " + labelEnd + ":");
            return "";
        }

        if (node instanceof ASTNode.PrintStmt ps) {
            String val = genNode(ps.expr);
            emit("PRINT " + val);
            return "";
        }

        if (node instanceof ASTNode.ReturnStmt rs) {
            if (rs.expr != null) {
                String val = genNode(rs.expr);
                emit("RETURN " + val);
            } else {
                emit("RETURN");
            }
            return "";
        }

        if (node instanceof ASTNode.NumberLit nl) return nl.value;
        if (node instanceof ASTNode.StringLit sl) return "\"" + sl.value + "\"";
        if (node instanceof ASTNode.BoolLit bl)   return bl.value ? "true" : "false";
        if (node instanceof ASTNode.VarRef vr)     return vr.name;

        return "";
    }

    private void emit(String instr) {
        code.add(instr);
    }
}
