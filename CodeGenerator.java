import java.util.*;

/**
 * PHASE 5: CODE GENERATION (Target Code)
 *
 * Translates Three-Address Code (TAC) into pseudo-assembly / target machine code.
 * This simulates what a real compiler would generate for a CPU.
 *
 * We generate a simplified assembly-like output with:
 *   MOV  reg, value    — load value into register
 *   ADD  r1, r2, r3    — r1 = r2 + r3
 *   SUB  r1, r2, r3    — r1 = r2 - r3
 *   MUL  r1, r2, r3    — r1 = r2 * r3
 *   DIV  r1, r2, r3    — r1 = r2 / r3
 *   CMP  r1, r2        — compare for conditional jumps
 *   JMP  label         — unconditional jump
 *   JEQ  label         — jump if equal
 *   JNE  label         — jump if not equal
 *   CALL print         — call print routine
 *   RET                — return
 */
public class CodeGenerator {

    private final List<String> assembly = new ArrayList<>();
    private final Map<String, String> varToReg = new LinkedHashMap<>();
    private int regCount = 0;

    private String getReg(String varName) {
        return varToReg.computeIfAbsent(varName, k -> "R" + regCount++);
    }

    public List<String> generate(List<String> tacCode) {
        assembly.add(";; ─── Generated Assembly ───────────────────────────────");
        assembly.add(".section .text");
        assembly.add(".global _start");
        assembly.add("_start:");

        for (String instr : tacCode) {
            assembly.add("");
            assembly.add(";; TAC: " + instr);
            translateInstruction(instr.trim());
        }

        assembly.add("");
        assembly.add(";; ─── Program End ──────────────────────────────────────");
        assembly.add("    MOV  R0, #0");
        assembly.add("    SYS  exit");
        return assembly;
    }

    private void translateInstruction(String instr) {
        // LABEL
        if (instr.startsWith("LABEL ")) {
            String label = instr.substring(6).replace(":", "").trim();
            assembly.add(label + ":");
            return;
        }
        // GOTO
        if (instr.startsWith("GOTO ")) {
            String label = instr.substring(5).trim();
            assembly.add("    JMP  " + label);
            return;
        }
        // PRINT
        if (instr.startsWith("PRINT ")) {
            String val = instr.substring(6).trim();
            String reg = isLiteral(val) ? loadLiteral(val) : getReg(val);
            if (isLiteral(val)) assembly.add("    MOV  " + reg + ", " + val);
            assembly.add("    MOV  R_ARG, " + reg);
            assembly.add("    CALL print");
            return;
        }
        // RETURN
        if (instr.equals("RETURN")) {
            assembly.add("    RET");
            return;
        }
        if (instr.startsWith("RETURN ")) {
            String val = instr.substring(7).trim();
            String reg = isLiteral(val) ? loadLiteral(val) : getReg(val);
            if (isLiteral(val)) assembly.add("    MOV  " + reg + ", " + val);
            assembly.add("    MOV  R0, " + reg);
            assembly.add("    RET");
            return;
        }
        // CONDITIONAL: if [!]cond GOTO label
        if (instr.startsWith("if ")) {
            String rest = instr.substring(3).trim();
            boolean negate = rest.startsWith("!");
            if (negate) rest = rest.substring(1);
            String[] parts = rest.split(" GOTO ");
            String cond  = parts[0].trim();
            String label = parts[1].trim();
            String reg = isLiteral(cond) ? loadLiteral(cond) : getReg(cond);
            if (isLiteral(cond)) assembly.add("    MOV  " + reg + ", " + cond);
            assembly.add("    CMP  " + reg + ", #0");
            assembly.add("    " + (negate ? "JEQ" : "JNE") + "  " + label);
            return;
        }
        // ASSIGNMENT: dest = ...
        if (instr.contains(" = ")) {
            String[] sides = instr.split(" = ", 2);
            String dest = sides[0].trim();
            String rhs  = sides[1].trim();
            String destReg = getReg(dest);

            // Binary operation: a OP b
            String[] ops = {" \\+ ", " - ", " \\* ", " / ", " % ", " == ", " != ", " < ", " > ", " <= ", " >= ", " && ", " \\|\\| "};
            String[] opNames = {"+", "-", "*", "/", "%", "==", "!=", "<", ">", "<=", ">=", "&&", "||"};
            boolean handled = false;
            for (int i = 0; i < ops.length; i++) {
                String[] operands = rhs.split(ops[i]);
                if (operands.length == 2) {
                    String lStr = operands[0].trim();
                    String rStr = operands[1].trim();
                    String lReg = isLiteral(lStr) ? "R_TMP1" : getReg(lStr);
                    String rReg = isLiteral(rStr) ? "R_TMP2" : getReg(rStr);
                    if (isLiteral(lStr)) assembly.add("    MOV  " + lReg + ", " + lStr);
                    if (isLiteral(rStr)) assembly.add("    MOV  " + rReg + ", " + rStr);
                    assembly.add("    " + opToAsm(opNames[i]) + "  " + destReg + ", " + lReg + ", " + rReg);
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                // Unary or simple copy
                if (rhs.startsWith("-") || rhs.startsWith("!")) {
                    String op = String.valueOf(rhs.charAt(0));
                    String operand = rhs.substring(1).trim();
                    String oReg = isLiteral(operand) ? "R_TMP1" : getReg(operand);
                    if (isLiteral(operand)) assembly.add("    MOV  " + oReg + ", " + operand);
                    assembly.add("    " + (op.equals("-") ? "NEG" : "NOT") + "  " + destReg + ", " + oReg);
                } else {
                    // Simple copy
                    String srcReg = isLiteral(rhs) ? "R_TMP1" : getReg(rhs);
                    if (isLiteral(rhs)) assembly.add("    MOV  " + srcReg + ", " + rhs);
                    assembly.add("    MOV  " + destReg + ", " + srcReg);
                }
            }
        }
    }

    private boolean isLiteral(String s) {
        if (s == null || s.isEmpty()) return false;
        if (s.equals("true") || s.equals("false")) return true;
        if (s.startsWith("\"")) return true;
        try { Double.parseDouble(s); return true; } catch (Exception e) { return false; }
    }

    private String loadLiteral(String s) { return "R_LIT"; }

    private String opToAsm(String op) {
        return switch (op) {
            case "+"  -> "ADD ";
            case "-"  -> "SUB ";
            case "*"  -> "MUL ";
            case "/"  -> "DIV ";
            case "%"  -> "MOD ";
            case "==" -> "CEQ ";
            case "!=" -> "CNE ";
            case "<"  -> "CLT ";
            case ">"  -> "CGT ";
            case "<=" -> "CLE ";
            case ">=" -> "CGE ";
            case "&&" -> "AND ";
            case "||" -> "OR  ";
            default   -> "MOV ";
        };
    }
}
