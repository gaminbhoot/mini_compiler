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
 *   java Main                — interactive mode (type your code)
 *   java Main <file.txt>     — compile a source file
 */
public class Main {

    static final String RESET   = "\u001B[0m";
    static final String BOLD    = "\u001B[1m";
    static final String CYAN    = "\u001B[36m";
    static final String GREEN   = "\u001B[32m";
    static final String YELLOW  = "\u001B[33m";
    static final String RED     = "\u001B[31m";
    static final String BLUE    = "\u001B[34m";
    static final String MAGENTA = "\u001B[35m";
    static final String WHITE   = "\u001B[97m";
    static final String DIM     = "\u001B[2m";

    public static void main(String[] args) throws Exception {
        printBanner();
        Scanner scanner = new Scanner(System.in);

        if (args.length > 0) {
            // ── File mode ──────────────────────────────────────────────────
            String fileName = args[0];
            String source = Files.readString(Path.of(fileName));
            System.out.println(CYAN + "Compiling file: " + WHITE + fileName + RESET + "\n");
            compileAndShow(source);
        } else {
            // ── Interactive REPL loop ──────────────────────────────────────
            printHelp();
            while (true) {
                System.out.println();
                System.out.println(CYAN + BOLD + "+-- What would you like to do?" + RESET);
                System.out.println(DIM + "  [1] Type / paste code");
                System.out.println("  [2] Load from file");
                System.out.println("  [3] Exit" + RESET);
                System.out.print(CYAN + "  > " + RESET);

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> {
                        String source = readCodeFromUser(scanner);
                        if (!source.isBlank()) compileAndShow(source);
                    }
                    case "2" -> {
                        System.out.print(CYAN + "  Enter file path: " + RESET);
                        String path = scanner.nextLine().trim();
                        try {
                            String source = Files.readString(Path.of(path));
                            compileAndShow(source);
                        } catch (IOException e) {
                            System.out.println(RED + "  [ERROR] File not found: " + path + RESET);
                        }
                    }
                    case "3", "exit", "quit" -> {
                        System.out.println(CYAN + "\n  Goodbye!\n" + RESET);
                        return;
                    }
                    default -> System.out.println(YELLOW + "  Please enter 1, 2, or 3." + RESET);
                }
            }
        }
    }

    // ─── Read multi-line code from user ───────────────────────────────────────

    static String readCodeFromUser(Scanner scanner) {
        System.out.println();
        System.out.println(CYAN + BOLD + "  +-- CODE EDITOR" + RESET);
        System.out.println(DIM + "  |  Type your code line by line.");
        System.out.println("  |  Type  END    on a new line when finished.");
        System.out.println("  |  Type  CANCEL to go back.");
        System.out.println("  +---------------------------------------------" + RESET);
        System.out.println();

        StringBuilder sb = new StringBuilder();
        int lineNum = 1;
        while (true) {
            System.out.printf(DIM + "  %2d | " + RESET, lineNum);
            String line = scanner.nextLine();
            if (line.trim().equalsIgnoreCase("END"))    break;
            if (line.trim().equalsIgnoreCase("CANCEL")) return "";
            sb.append(line).append("\n");
            lineNum++;
        }

        String code = sb.toString().trim();
        if (code.isBlank()) {
            System.out.println(YELLOW + "  No code entered." + RESET);
            return "";
        }
        return code;
    }

    // ─── Full Compiler Pipeline ───────────────────────────────────────────────

    static void compileAndShow(String source) {

        printSeparator("=", 66, CYAN);

        // ── Show Source ──────────────────────────────────────────────────────
        printPhaseHeader(0, "SOURCE CODE");
        String[] lines = source.strip().split("\n");
        for (int i = 0; i < lines.length; i++) {
            System.out.printf(DIM + "  %2d | " + RESET + "%s\n", i + 1, lines[i]);
        }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 1: LEXICAL ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(1, "LEXICAL ANALYSIS  (Tokenizer)");
        System.out.println(DIM + "  Scanning source code character-by-character...\n" + RESET);

        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.tokenize();
        List<String> lexErrors = lexer.getErrors();

        System.out.println("  " + DIM + String.format("%-4s  %-18s  %-18s", "LINE", "TYPE", "VALUE") + RESET);
        System.out.println("  " + DIM + "-".repeat(50) + RESET);
        for (Token t : tokens) {
            if (t.type == TokenType.EOF) continue;
            System.out.printf("  " + DIM + "%3d" + RESET + "   %-18s  %-18s\n",
                t.line, t.type, "'" + t.value + "'");
        }
        System.out.println();
        System.out.println("  " + GREEN + "[OK] " + RESET + tokens.size() + " tokens generated");
        showErrors(lexErrors);
        if (!lexErrors.isEmpty()) { printAbort("Lexical"); return; }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 2: SYNTAX ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(2, "SYNTAX ANALYSIS  (Parser / AST)");
        System.out.println(DIM + "  Checking grammar rules and building Abstract Syntax Tree...\n" + RESET);

        Parser parser = new Parser(tokens);
        ASTNode.Program ast = parser.parse();
        List<String> parseErrors = parser.getErrors();

        System.out.println(ast.toTree("  "));
        System.out.println("  " + GREEN + "[OK] " + RESET + "AST built successfully");
        showErrors(parseErrors);
        if (!parseErrors.isEmpty()) { printAbort("Syntax"); return; }

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 3: SEMANTIC ANALYSIS
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(3, "SEMANTIC ANALYSIS  (Type Check & Symbol Table)");
        System.out.println(DIM + "  Verifying types, checking declarations, building symbol table...\n" + RESET);

        SemanticAnalyzer sem = new SemanticAnalyzer();
        sem.analyze(ast);

        System.out.println("  " + BOLD + "Symbol Table:" + RESET);
        for (String entry : sem.getSymbolLog())
            System.out.println("  " + GREEN + entry + RESET);
        if (sem.getSymbolLog().isEmpty())
            System.out.println("  " + DIM + "(no declarations)" + RESET);

        System.out.println();
        showWarnings(sem.getWarnings());
        showErrors(sem.getErrors());
        if (!sem.getErrors().isEmpty()) { printAbort("Semantic"); return; }
        System.out.println("  " + GREEN + "[OK] " + RESET + "Semantic analysis passed");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 4: INTERMEDIATE CODE GENERATION
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(4, "INTERMEDIATE CODE  (Three-Address Code)");
        System.out.println(DIM + "  Translating AST to platform-independent TAC instructions...\n" + RESET);

        IntermediateCodeGen icg = new IntermediateCodeGen();
        List<String> tac = icg.generate(ast);

        for (String line : tac) {
            if (line.endsWith(":"))
                System.out.println(YELLOW + "  " + line + RESET);
            else if (line.startsWith("GOTO") || line.startsWith("if "))
                System.out.println(BLUE + "    " + line + RESET);
            else if (line.startsWith("PRINT") || line.startsWith("RETURN"))
                System.out.println(MAGENTA + "    " + line + RESET);
            else
                System.out.println(WHITE + "    " + line + RESET);
        }
        System.out.println();
        System.out.println("  " + GREEN + "[OK] " + RESET + tac.size() + " TAC instructions generated");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 5: CODE GENERATION
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(5, "CODE GENERATION  (Pseudo-Assembly)");
        System.out.println(DIM + "  Mapping TAC to target machine instructions...\n" + RESET);

        CodeGenerator cg = new CodeGenerator();
        List<String> asm = cg.generate(tac);

        for (String line : asm) {
            if (line.startsWith(";;"))      System.out.println(DIM    + "  " + line + RESET);
            else if (line.startsWith(".") || line.equals("_start:"))
                                            System.out.println(YELLOW + "  " + line + RESET);
            else if (line.endsWith(":"))    System.out.println(BLUE   + "  " + line + RESET);
            else                            System.out.println(GREEN  + "  " + line + RESET);
        }
        System.out.println();
        System.out.println("  " + GREEN + "[OK] " + RESET + asm.size() + " assembly instructions generated");

        // ─────────────────────────────────────────────────────────────────────
        // PHASE 6: EXECUTION
        // ─────────────────────────────────────────────────────────────────────
        printPhaseHeader(6, "EXECUTION  (Interpreter Output)");
        System.out.println(DIM + "  Running the program...\n" + RESET);

        Interpreter interp = new Interpreter();
        List<String> output = interp.run(ast);
        List<String> runtimeErrors = interp.getErrors();

        if (!output.isEmpty()) {
            printSeparator("-", 40, DIM);
            for (String line : output)
                System.out.println("  " + BOLD + "OUTPUT: " + RESET + GREEN + line + RESET);
            printSeparator("-", 40, DIM);
        } else {
            System.out.println("  " + DIM + "(no output produced)" + RESET);
        }
        showErrors(runtimeErrors);
        System.out.println();
        if (runtimeErrors.isEmpty())
            System.out.println("  " + GREEN + BOLD + "[OK] Compilation & execution successful!" + RESET);

        // ── Summary ──────────────────────────────────────────────────────────
        printPhaseHeader(0, "COMPILATION SUMMARY");
        System.out.printf("  %-35s %s%d%s\n", "Tokens generated:",      CYAN,   tokens.size() - 1,          RESET);
        System.out.printf("  %-35s %s%d%s\n", "AST nodes built:",        CYAN,   countNodes(ast),            RESET);
        System.out.printf("  %-35s %s%d%s\n", "Symbols declared:",       CYAN,   sem.getSymbolLog().size(),  RESET);
        System.out.printf("  %-35s %s%d%s\n", "TAC instructions:",       CYAN,   tac.size(),                 RESET);
        System.out.printf("  %-35s %s%d%s\n", "Assembly instructions:",  CYAN,   asm.size(),                 RESET);
        System.out.printf("  %-35s %s%d%s\n", "Output lines:",           CYAN,   output.size(),              RESET);
        System.out.printf("  %-35s %s%d%s\n", "Warnings:",               YELLOW, sem.getWarnings().size(),   RESET);
        System.out.printf("  %-35s %s%d%s\n", "Errors:",                 RED,
            lexErrors.size() + parseErrors.size() + sem.getErrors().size() + runtimeErrors.size(), RESET);

        printSeparator("=", 66, CYAN);
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

    static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "  +==============================================================+" + RESET);
        System.out.println(CYAN + BOLD + "  |        " + WHITE + "MINI COMPILER  -  Compiler Construction Project" + CYAN + "        |" + RESET);
        System.out.println(CYAN + BOLD + "  |   " + DIM + "Phases: Lexer -> Parser -> Semantic -> TAC -> Assembly -> Exec" + CYAN + "  |" + RESET);
        System.out.println(CYAN + BOLD + "  +==============================================================+" + RESET);
        System.out.println();
    }

    static void printHelp() {
        System.out.println(DIM + "  Supported syntax:" + RESET);
        System.out.println(DIM + "    int x = 5;           float f = 3.14;        bool b = true;" + RESET);
        System.out.println(DIM + "    string s = \"hello\";   x = x + 1;             print(x);" + RESET);
        System.out.println(DIM + "    if (x > 3) { ... }   else { ... }           while (x < 10) { ... }" + RESET);
        System.out.println(DIM + "    // this is a comment" + RESET);
        System.out.println();
    }

    static void printPhaseHeader(int phase, String title) {
        System.out.println();
        String label = phase == 0 ? "" : "PHASE " + phase + " | ";
        System.out.println(BOLD + BLUE + "  +-- " + label + title + RESET);
        System.out.println();
    }

    static void printSeparator(String ch, int len, String color) {
        System.out.println("  " + color + ch.repeat(len) + RESET);
    }

    static void showErrors(List<String> errors) {
        for (String e : errors)
            System.out.println("  " + RED + "[ERROR] " + e + RESET);
    }

    static void showWarnings(List<String> warnings) {
        for (String w : warnings)
            System.out.println("  " + YELLOW + "[WARN] " + w + RESET);
    }

    static void printAbort(String phase) {
        System.out.println();
        System.out.println(RED + BOLD + "  Compilation aborted at " + phase + " phase due to errors." + RESET);
    }
}