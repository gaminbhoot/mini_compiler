import java.util.*;

/**
 * PHASE 2: SYNTAX ANALYSIS (Parser)
 *
 * Takes the list of tokens from the Lexer and checks if they follow
 * the grammar rules of the language. Builds an AST (Abstract Syntax Tree).
 *
 * Grammar (simplified):
 *   program    → statement* EOF
 *   statement  → varDecl | assign | ifStmt | whileStmt | printStmt | returnStmt | block
 *   varDecl    → TYPE IDENTIFIER ('=' expr)? ';'
 *   assign     → IDENTIFIER '=' expr ';'
 *   ifStmt     → 'if' '(' expr ')' block ('else' block)?
 *   whileStmt  → 'while' '(' expr ')' block
 *   printStmt  → 'print' '(' expr ')' ';'
 *   returnStmt → 'return' expr? ';'
 *   block      → '{' statement* '}'
 *   expr       → equality (('&&'|'||') equality)*
 *   equality   → comparison (('=='|'!=') comparison)*
 *   comparison → term (('<'|'>'|'<='|'>=') term)*
 *   term       → factor (('+'|'-') factor)*
 *   factor     → unary (('*'|'/'|'%') unary)*
 *   unary      → ('!'|'-') unary | primary
 *   primary    → NUMBER | STRING | 'true' | 'false' | IDENTIFIER | '(' expr ')'
 */
public class Parser {

    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Token peek()         { return tokens.get(pos); }
    private Token previous()     { return tokens.get(pos - 1); }
    private boolean isAtEnd()    { return peek().type == TokenType.EOF; }

    private boolean check(TokenType t) {
        return !isAtEnd() && peek().type == t;
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { pos++; return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String msg) {
        if (check(type)) return tokens.get(pos++);
        errors.add("Line " + peek().line + ": " + msg + " (got '" + peek().value + "')");
        return peek(); // error recovery: return current token
    }

    // ─── Parse Entry ───────────────────────────────────────────────────────────

    public ASTNode.Program parse() {
        List<ASTNode> stmts = new ArrayList<>();
        while (!isAtEnd()) {
            ASTNode stmt = parseStatement();
            if (stmt != null) stmts.add(stmt);
        }
        return new ASTNode.Program(stmts);
    }

    // ─── Statements ────────────────────────────────────────────────────────────

    private ASTNode parseStatement() {
        if (isTypeKeyword())               return parseVarDecl();
        if (match(TokenType.IF))           return parseIf();
        if (match(TokenType.WHILE))        return parseWhile();
        if (match(TokenType.PRINT))        return parsePrint();
        if (match(TokenType.RETURN))       return parseReturn();
        if (check(TokenType.LBRACE))       return parseBlock();
        if (check(TokenType.IDENTIFIER))   return parseAssignOrExprStmt();
        // Skip unknown tokens
        errors.add("Line " + peek().line + ": Unexpected token '" + peek().value + "'");
        pos++;
        return null;
    }

    private boolean isTypeKeyword() {
        TokenType t = peek().type;
        return t == TokenType.INT || t == TokenType.FLOAT
            || t == TokenType.STRING_TYPE || t == TokenType.BOOL;
    }

    private ASTNode parseVarDecl() {
        String type = tokens.get(pos++).value;
        Token name = consume(TokenType.IDENTIFIER, "Expected variable name");
        ASTNode init = null;
        if (match(TokenType.ASSIGN)) {
            init = parseExpr();
        }
        consume(TokenType.SEMICOLON, "Expected ';' after variable declaration");
        return new ASTNode.VarDecl(type, name.value, init);
    }

    private ASTNode parseAssignOrExprStmt() {
        // Peek ahead to see if this is assignment
        if (pos + 1 < tokens.size() && tokens.get(pos + 1).type == TokenType.ASSIGN) {
            String name = tokens.get(pos++).value;
            pos++; // consume '='
            ASTNode val = parseExpr();
            consume(TokenType.SEMICOLON, "Expected ';' after assignment");
            return new ASTNode.Assign(name, val);
        }
        // Otherwise treat as expression statement
        ASTNode expr = parseExpr();
        consume(TokenType.SEMICOLON, "Expected ';'");
        return expr;
    }

    private ASTNode parseIf() {
        consume(TokenType.LPAREN, "Expected '(' after 'if'");
        ASTNode cond = parseExpr();
        consume(TokenType.RPAREN, "Expected ')' after if condition");
        ASTNode thenB = parseBlock();
        ASTNode elseB = null;
        if (match(TokenType.ELSE)) elseB = parseBlock();
        return new ASTNode.IfStmt(cond, thenB, elseB);
    }

    private ASTNode parseWhile() {
        consume(TokenType.LPAREN, "Expected '(' after 'while'");
        ASTNode cond = parseExpr();
        consume(TokenType.RPAREN, "Expected ')' after while condition");
        ASTNode body = parseBlock();
        return new ASTNode.WhileStmt(cond, body);
    }

    private ASTNode parsePrint() {
        consume(TokenType.LPAREN, "Expected '(' after 'print'");
        ASTNode expr = parseExpr();
        consume(TokenType.RPAREN, "Expected ')' after print expression");
        consume(TokenType.SEMICOLON, "Expected ';' after print statement");
        return new ASTNode.PrintStmt(expr);
    }

    private ASTNode parseReturn() {
        ASTNode expr = null;
        if (!check(TokenType.SEMICOLON)) expr = parseExpr();
        consume(TokenType.SEMICOLON, "Expected ';' after return");
        return new ASTNode.ReturnStmt(expr);
    }

    private ASTNode parseBlock() {
        consume(TokenType.LBRACE, "Expected '{'");
        List<ASTNode> stmts = new ArrayList<>();
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            ASTNode s = parseStatement();
            if (s != null) stmts.add(s);
        }
        consume(TokenType.RBRACE, "Expected '}'");
        return new ASTNode.Block(stmts);
    }

    // ─── Expressions (recursive descent) ──────────────────────────────────────

    private ASTNode parseExpr() { return parseLogical(); }

    private ASTNode parseLogical() {
        ASTNode left = parseEquality();
        while (match(TokenType.AND, TokenType.OR)) {
            String op = previous().value;
            ASTNode right = parseEquality();
            left = new ASTNode.BinaryExpr(left, op, right);
        }
        return left;
    }

    private ASTNode parseEquality() {
        ASTNode left = parseComparison();
        while (match(TokenType.EQ, TokenType.NEQ)) {
            String op = previous().value;
            ASTNode right = parseComparison();
            left = new ASTNode.BinaryExpr(left, op, right);
        }
        return left;
    }

    private ASTNode parseComparison() {
        ASTNode left = parseTerm();
        while (match(TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE)) {
            String op = previous().value;
            ASTNode right = parseTerm();
            left = new ASTNode.BinaryExpr(left, op, right);
        }
        return left;
    }

    private ASTNode parseTerm() {
        ASTNode left = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            String op = previous().value;
            ASTNode right = parseFactor();
            left = new ASTNode.BinaryExpr(left, op, right);
        }
        return left;
    }

    private ASTNode parseFactor() {
        ASTNode left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            String op = previous().value;
            ASTNode right = parseUnary();
            left = new ASTNode.BinaryExpr(left, op, right);
        }
        return left;
    }

    private ASTNode parseUnary() {
        if (match(TokenType.NOT, TokenType.MINUS)) {
            String op = previous().value;
            return new ASTNode.UnaryExpr(op, parseUnary());
        }
        return parsePrimary();
    }

    private ASTNode parsePrimary() {
        if (match(TokenType.NUMBER))     return new ASTNode.NumberLit(previous().value);
        if (match(TokenType.STRING))     return new ASTNode.StringLit(previous().value);
        if (match(TokenType.TRUE))       return new ASTNode.BoolLit(true);
        if (match(TokenType.FALSE))      return new ASTNode.BoolLit(false);
        if (match(TokenType.IDENTIFIER)) return new ASTNode.VarRef(previous().value);
        if (match(TokenType.LPAREN)) {
            ASTNode expr = parseExpr();
            consume(TokenType.RPAREN, "Expected ')' after expression");
            return expr;
        }
        errors.add("Line " + peek().line + ": Unexpected token in expression: '" + peek().value + "'");
        pos++;
        return new ASTNode.NumberLit("0"); // error recovery
    }

    public List<String> getErrors() { return errors; }
}
