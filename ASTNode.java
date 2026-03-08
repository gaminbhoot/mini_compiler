import java.util.List;

/**
 * AST (Abstract Syntax Tree) Node definitions.
 * Each node represents a construct in the mini language.
 */
public abstract class ASTNode {
    public abstract String toTree(String indent);

    // ─── Statements ────────────────────────────────────────────────────────────

    public static class Program extends ASTNode {
        public final List<ASTNode> statements;
        public Program(List<ASTNode> statements) { this.statements = statements; }

        @Override public String toTree(String indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("Program\n");
            for (ASTNode s : statements) sb.append(s.toTree(indent + "  ├─ "));
            return sb.toString();
        }
    }

    public static class VarDecl extends ASTNode {
        public final String type, name;
        public final ASTNode initializer;
        public VarDecl(String type, String name, ASTNode initializer) {
            this.type = type; this.name = name; this.initializer = initializer;
        }
        @Override public String toTree(String indent) {
            String s = indent + "VarDecl [" + type + " " + name + "]\n";
            if (initializer != null) s += initializer.toTree(indent + "  └─ ");
            return s;
        }
    }

    public static class Assign extends ASTNode {
        public final String name;
        public final ASTNode value;
        public Assign(String name, ASTNode value) { this.name = name; this.value = value; }
        @Override public String toTree(String indent) {
            return indent + "Assign [" + name + "]\n" + value.toTree(indent + "  └─ ");
        }
    }

    public static class IfStmt extends ASTNode {
        public final ASTNode condition;
        public final ASTNode thenBranch, elseBranch;
        public IfStmt(ASTNode condition, ASTNode thenBranch, ASTNode elseBranch) {
            this.condition = condition; this.thenBranch = thenBranch; this.elseBranch = elseBranch;
        }
        @Override public String toTree(String indent) {
            String s = indent + "IfStmt\n"
                + condition.toTree(indent + "  ├─ cond: ")
                + thenBranch.toTree(indent + "  ├─ then: ");
            if (elseBranch != null) s += elseBranch.toTree(indent + "  └─ else: ");
            return s;
        }
    }

    public static class WhileStmt extends ASTNode {
        public final ASTNode condition, body;
        public WhileStmt(ASTNode condition, ASTNode body) {
            this.condition = condition; this.body = body;
        }
        @Override public String toTree(String indent) {
            return indent + "WhileStmt\n"
                + condition.toTree(indent + "  ├─ cond: ")
                + body.toTree(indent + "  └─ body: ");
        }
    }

    public static class Block extends ASTNode {
        public final List<ASTNode> statements;
        public Block(List<ASTNode> statements) { this.statements = statements; }
        @Override public String toTree(String indent) {
            StringBuilder sb = new StringBuilder();
            sb.append(indent).append("Block\n");
            for (ASTNode s : statements) sb.append(s.toTree(indent + "  ├─ "));
            return sb.toString();
        }
    }

    public static class PrintStmt extends ASTNode {
        public final ASTNode expr;
        public PrintStmt(ASTNode expr) { this.expr = expr; }
        @Override public String toTree(String indent) {
            return indent + "PrintStmt\n" + expr.toTree(indent + "  └─ ");
        }
    }

    public static class ReturnStmt extends ASTNode {
        public final ASTNode expr;
        public ReturnStmt(ASTNode expr) { this.expr = expr; }
        @Override public String toTree(String indent) {
            return indent + "ReturnStmt\n" + (expr != null ? expr.toTree(indent + "  └─ ") : "");
        }
    }

    // ─── Expressions ───────────────────────────────────────────────────────────

    public static class BinaryExpr extends ASTNode {
        public final ASTNode left, right;
        public final String op;
        public BinaryExpr(ASTNode left, String op, ASTNode right) {
            this.left = left; this.op = op; this.right = right;
        }
        @Override public String toTree(String indent) {
            return indent + "BinaryExpr [" + op + "]\n"
                + left.toTree(indent + "  ├─ ")
                + right.toTree(indent + "  └─ ");
        }
    }

    public static class UnaryExpr extends ASTNode {
        public final ASTNode operand;
        public final String op;
        public UnaryExpr(String op, ASTNode operand) { this.op = op; this.operand = operand; }
        @Override public String toTree(String indent) {
            return indent + "UnaryExpr [" + op + "]\n" + operand.toTree(indent + "  └─ ");
        }
    }

    public static class NumberLit extends ASTNode {
        public final String value;
        public NumberLit(String value) { this.value = value; }
        @Override public String toTree(String indent) { return indent + "Number [" + value + "]\n"; }
    }

    public static class StringLit extends ASTNode {
        public final String value;
        public StringLit(String value) { this.value = value; }
        @Override public String toTree(String indent) { return indent + "String [\"" + value + "\"]\n"; }
    }

    public static class BoolLit extends ASTNode {
        public final boolean value;
        public BoolLit(boolean value) { this.value = value; }
        @Override public String toTree(String indent) { return indent + "Bool [" + value + "]\n"; }
    }

    public static class VarRef extends ASTNode {
        public final String name;
        public VarRef(String name) { this.name = name; }
        @Override public String toTree(String indent) { return indent + "VarRef [" + name + "]\n"; }
    }
}
