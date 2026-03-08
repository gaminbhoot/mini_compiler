import java.util.*;
import java.io.*;
import java.nio.file.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *   MINI COMPILER — Compiler Construction Project
 *   Demonstrates all phases of compilation:
 *     1. Lexical Analysis    (Lexer/Tokenizer)
 *     2. Syntax Analysis     (Parser / AST Builder)
 *     3. Semantic Analysis   (Type Check / Symbol Table)
 *     4. Intermediate Code   (Three-Address Code)
 *     5. Code Generation     (Pseudo-Assembly)
 *     6. Execution           (Interpreter / Output)
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Usage:
 *   java Main                    — runs built-in demo programs
 *   java Main <file.txt>         — compiles a source file
 *   java Main --phase <1-6>      — runs only up to a given phase
 */
public class Main {

    static final String RESET  = "\u001B[0m";
    static final String BOLD   = "\u001B[1m";
    static final String CYAN   = "\u001B[36m";
    static final String GREEN  = "\u001B[32m";
    static final String YELLOW = "\u001B[33m";
    static final String RED    = "\u001B[31m";
    static final String BLUE   = "\u001B[34m";
    static final String MAGENTA= "\u001B[35m";
    static final String WHITE  = "\u001B[97m";
    static final String DIM    = "\u001B[2m";

    // ─── Demo Programs ────────────────────────────────────────────────────────

    static final String[][] DEMOS = {
        {
            "Hello World & Arithmetic",
            """
            // Demo 1: Basic variables and arithmetic
            int x = 10;
            int y = 3;
            int sum = x + y;
            int product = x * y;
            float avg = (x + y) / 2;
            print(sum);
            print(product);
            print(avg);
            """
        },
        {
            "If-Else & Boolean Logic",
            """
            // Demo 2: Conditionals
            int score = 85;
            bool passed = score >= 60;
            if (passed) {
                print(score);
            } else {
                int fail = 0;
                print(fail);
            }
            """
        },
        {
            "While Loop — Factorial",
            """
            // Demo 3: Factorial using while loop
            int n = 5;
            int result = 1;
            int i = 1;
            while (i <= n) {
                result = result * i;
                i = i + 1;
            }
            print(result);
            """
        },
        {
            "String Concatenation",
            """
            // Demo 4: Strings
            string name = "Compiler";
            string msg = "Hello, ";
            string full = msg + name;
            print(full);
            int year = 2025;
            print(year);
            """
        }
    };

    // ─── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) throws Exception {
        printBanner();

        String source = null;
        String fileName = null;

        if (args.length > 0) {
            fileName = args[0];
            source = Files.readString(Path.of(fileName));
            System.out.println(CYAN + "Compiling file: " + WHITE + fileName + RESET);
            compileAndShow(source, fileName);
        } else {
            System.out.println(CYAN + "No file specified — running " + DEMOS.length + " built-in demo programs.\n" + RESET);
            for (int i = 0; i < DEMOS.length; i++) {
                System.out.println();
                printSeparator("═", 66, MAGENTA);
                System.out.println(MAGENTA + BOLD + "  DEMO " + (i+1) + ": " + DEMOS[i][0] + RESET);
                printSeparator("═", 66, MAGENTA);
                compileAndShow(DEMOS[i][1], "demo" + (i+1));
                System.out.println();
                if (i < DEMOS.length - 1) {
                    System.out.print(DIM + "Press ENTER for next demo..." + RESET);
                    new Scanner(System.in).nextLine();
                }
            }
        }
    }

    // ─── Full Pipeline ────────────────────────────────────────────────────────

    static void compileAndShow(String source, String name) {

        // ── Show Source ──────────────────────────────────────────────────────
        printPhaseHeader(0, "SOURCE CODE", "📄");
        String[] lines = source.strip().split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf(DIM + "  %2d │ " + RESET + "%s\n", i + 1, lines[i]);
        }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 1: LEXICAL ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(1, "LEXICAL ANALYSIS  (Tokenizer)", "🔤");
        System.out.println(DIM + "  Scanning source code character-by-character...\n" + RESET);

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        List<String> lexErrors = lexer.getErrors();

        // Print token table
        System.out.println("  " + DIM + String.format("%-4s  %-18s  %-18s  %s", "LINE", "TYPE", "VALUE", "") + RESET);
        System.out.println("  " + DIM + "─".repeat(55) + RESET);
        for (Token t : tokens) {
            if (t.type == TokenType.EOF) continue;
            String color = tokenColor(t.type);
            System.out.printf("  " + DIM + "%3d" + RESET + "   " + color + "%-18s" + RESET + "  %-18s\n",
                t.line, t.type, "'" + t.value + "'");
        }
        System.out.println();
        System.out.println("  " + GREEN + "✔ " + RESET + tokens.size() + " tokens generated");
        showErrors(lexErrors);

        if (!lexErrors.isEmpty()) { printAbort("Lexical"); return; }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 2: SYNTAX ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(2, "SYNTAX ANALYSIS  (Parser / AST)", "🌳");
        System.out.println(DIM + "  Checking grammar rules and building Abstract Syntax Tree...\n" + RESET);

        Parser parser = new Parser(tokens);
        ASTNode.Program ast = parser.parse();
        List<String> parseErrors = parser.getErrors();

        System.out.println(CYAN + ast.toTree("  ") + RESET);
        System.out.println("  " + GREEN + "✔ " + RESET + "AST built successfully");
        showErrors(parseErrors);

        if (!parseErrors.isEmpty()) { printAbort("Syntax"); return; }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 3: SEMANTIC ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(3, "SEMANTIC ANALYSIS  (Type Check & Symbol Table)", "🔍");
        System.out.println(DIM + "  Verifying types, checking declarations, building symbol table...\n" + RESET);

        SemanticAnalyzer sem = new SemanticAnalyzer();
        sem.analyze(ast);

        System.out.println("  " + BOLD + "Symbol Table Log:" + RESET);
        for (String entry : sem.getSymbolLog()) {
            System.out.println("  " + GREEN + entry + RESET);
        }
        if (sem.getSymbolLog().isEmpty()) System.out.println("  " + DIM + "(no declarations)" + RESET);

        System.out.println();
        showWarnings(sem.getWarnings());
        showErrors(sem.getErrors());

        if (!sem.getErrors().isEmpty()) { printAbort("Semantic"); return; }
        System.out.println("  " + GREEN + "✔ " + RESET + "Semantic analysis passed");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 4: INTERMEDIATE CODE GENERATION
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(4, "INTERMEDIATE CODE GENERATION  (Three-Address Code)", "⚙️");
        System.out.println(DIM + "  Translating AST to platform-independent TAC instructions...\n" + RESET);

        IntermediateCodeGen icg = new IntermediateCodeGen();
        List<String> tac = icg.generate(ast);

        for (int i = 0; i < tac.size(); i++) {
            String line = tac.get(i);
            String indent = "    ";
            if (line.endsWith(":"))         // LABEL
                System.out.println(YELLOW + "  " + line + RESET);
            else if (line.startsWith("GOTO") || line.startsWith("if "))
                System.out.println(BLUE + indent + line + RESET);
            else if (line.startsWith("PRINT") || line.startsWith("RETURN"))
                System.out.println(MAGENTA + indent + line + RESET);
            else
                System.out.println(WHITE + indent + line + RESET);
        }
        System.out.println();
        System.out.println("  " + GREEN + "✔ " + RESET + tac.size() + " TAC instructions generated");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 5: CODE GENERATION (Pseudo-Assembly)
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(5, "CODE GENERATION  (Pseudo-Assembly)", "💻");
        System.out.println(DIM + "  Mapping TAC to target machine instructions...\n" + RESET);

        CodeGenerator cg = new CodeGenerator();
        List<String> asm = cg.generate(tac);

        for (String line : asm) {
            if (line.startsWith(";;"))
                System.out.println(DIM + "  " + line + RESET);
            else if (line.startsWith(".") || line.equals("_start:"))
                System.out.println(YELLOW + "  " + line + RESET);
            else if (line.endsWith(":"))
                System.out.println(BLUE + "  " + line + RESET);
            else
                System.out.println(GREEN + "  " + line + RESET);
        }
        System.out.println();
        System.out.println("  " + GREEN + "✔ " + RESET + asm.size() + " assembly instructions generated");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 6: EXECUTION / INTERPRETER
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(6, "EXECUTION  (Interpreter Output)", "▶️");
        System.out.println(DIM + "  Running the program...\n" + RESET);

        Interpreter interp = new Interpreter();
        List<String> output = interp.run(ast);
        List<String> runtimeErrors = interp.getErrors();

        if (!output.isEmpty()) {
            printSeparator("─", 40, DIM);
            for (String line : output) {
                System.out.println("  " + BOLD + WHITE + "OUTPUT: " + RESET + GREEN + line + RESET);
            }
            printSeparator("─", 40, DIM);
        } else {
            System.out.println("  " + DIM + "(no output produced)" + RESET);
        }
        showErrors(runtimeErrors);
        System.out.println();
        if (runtimeErrors.isEmpty())
            System.out.println("  " + GREEN + BOLD + "✔ Compilation & execution successful!" + RESET);

        // ─── Summary ─────────────────────────────────────────────────────────
        printPhaseHeader(0, "COMPILATION SUMMARY", "📊");
        System.out.printf("  %-35s %s%d%s\n",   "Tokens generated:",       CYAN,    tokens.size() - 1, RESET);
        System.out.printf("  %-35s %s%d%s\n",   "AST nodes built:",        CYAN,    countNodes(ast),   RESET);
        System.out.printf("  %-35s %s%d%s\n",   "Symbols declared:",       CYAN,    sem.getSymbolLog().size(), RESET);
        System.out.printf("  %-35s %s%d%s\n",   "TAC instructions:",       CYAN,    tac.size(),        RESET);
        System.out.printf("  %-35s %s%d%s\n",   "Assembly instructions:",  CYAN,    asm.size(),        RESET);
        System.out.printf("  %-35s %s%d%s\n",   "Output lines:",           CYAN,    output.size(),     RESET);
        System.out.printf("  %-35s %s%d%s\n",   "Warnings:",               YELLOW,  sem.getWarnings().size(), RESET);
        System.out.printf("  %-35s %s%d%s\n",   "Errors:",                 RED,     lexErrors.size() + parseErrors.size() + sem.getErrors().size() + runtimeErrors.size(), RESET);
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    static int countNodes(ASTNode node) {
        if (node == null) return 0;
        int count = 1;
        if (node instanceof ASTNode.Program p)    for (ASTNode s : p.statements) count += countNodes(s);
        if (node instanceof ASTNode.Block b)       for (ASTNode s : b.statements) count += countNodes(s);
        if (node instanceof ASTNode.VarDecl vd)   count += countNodes(vd.initializer);
        if (node instanceof ASTNode.Assign a)      count += countNodes(a.value);
        if (node instanceof ASTNode.IfStmt i)      { count += countNodes(i.condition) + countNodes(i.thenBranch) + countNodes(i.elseBranch); }
        if (node instanceof ASTNode.WhileStmt w)   { count += countNodes(w.condition) + countNodes(w.body); }
        if (node instanceof ASTNode.PrintStmt ps)  count += countNodes(ps.expr);
        if (node instanceof ASTNode.ReturnStmt rs) count += countNodes(rs.expr);
        if (node instanceof ASTNode.BinaryExpr be) { count += countNodes(be.left) + countNodes(be.right); }
        if (node instanceof ASTNode.UnaryExpr ue)  count += countNodes(ue.operand);
        return count;
    }

    static String tokenColor(TokenType t) {
        return switch (t) {
            case NUMBER -> YELLOW;
            case STRING -> GREEN;
            case IDENTIFIER -> WHITE;
            case INT, FLOAT, STRING_TYPE, BOOL, IF, ELSE, WHILE, FOR, PRINT, RETURN, TRUE, FALSE -> CYAN;
            case PLUS, MINUS, STAR, SLASH, MOD, ASSIGN, EQ, NEQ, LT, GT, LTE, GTE, AND, OR, NOT -> MAGENTA;
            default -> DIM;
        };
    }

    static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "  ╔══════════════════════════════════════════════════════════════╗" + RESET);
        System.out.println(CYAN + BOLD + "  ║        " + WHITE + "MINI COMPILER  —  Compiler Construction Project" + CYAN + "        ║" + RESET);
        System.out.println(CYAN + BOLD + "  ║   " + DIM + "Phases: Lexer → Parser → Semantic → TAC → Assembly → Exec" + CYAN + "   ║" + RESET);
        System.out.println(CYAN + BOLD + "  ╚══════════════════════════════════════════════════════════════╝" + RESET);
        System.out.println();
    }

    static void printPhaseHeader(int phase, String title, String icon) {
        System.out.println();
        String label = phase == 0 ? "" : "PHASE " + phase + " │ ";
        System.out.println(BOLD + BLUE + "  ┌─ " + icon + "  " + label + title + " " + RESET);
        System.out.println();
    }

    static void printSeparator(String ch, int len, String color) {
        System.out.println("  " + color + ch.repeat(len) + RESET);
    }

    static void showErrors(List<String> errors) {
        for (String e : errors)
            System.out.println("  " + RED + "✖ ERROR: " + e + RESET);
    }

    static void showWarnings(List<String> warnings) {
        for (String w : warnings)
            System.out.println("  " + YELLOW + "⚠ WARNING: " + w + RESET);
    }

    static void printAbort(String phase) {
        System.out.println();
        System.out.println(RED + BOLD + "  ✖ Compilation aborted at " + phase + " phase due to errors." + RESET);
    }
}
