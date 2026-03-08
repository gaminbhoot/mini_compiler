import java.util.*;

/**
 * PHASE 1: LEXICAL ANALYSIS (Lexer / Scanner / Tokenizer)
 *
 * Reads raw source code character by character and groups them
 * into meaningful units called TOKENS (lexemes).
 *
 * Example:  "int x = 5 + 3;"
 *   → Token(INT, 'int'), Token(IDENTIFIER, 'x'), Token(ASSIGN, '='),
 *     Token(NUMBER, '5'), Token(PLUS, '+'), Token(NUMBER, '3'), Token(SEMICOLON, ';')
 */
public class Lexer {

    private final String source;
    private int pos = 0;
    private int line = 1;
    private final List<Token> tokens = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();
    static {
        KEYWORDS.put("int",    TokenType.INT);
        KEYWORDS.put("float",  TokenType.FLOAT);
        KEYWORDS.put("string", TokenType.STRING_TYPE);
        KEYWORDS.put("bool",   TokenType.BOOL);
        KEYWORDS.put("if",     TokenType.IF);
        KEYWORDS.put("else",   TokenType.ELSE);
        KEYWORDS.put("while",  TokenType.WHILE);
        KEYWORDS.put("for",    TokenType.FOR);
        KEYWORDS.put("print",  TokenType.PRINT);
        KEYWORDS.put("return", TokenType.RETURN);
        KEYWORDS.put("true",   TokenType.TRUE);
        KEYWORDS.put("false",  TokenType.FALSE);
    }

    public Lexer(String source) {
        this.source = source;
    }

    public List<Token> tokenize() {
        while (pos < source.length()) {
            skipWhitespaceAndComments();
            if (pos >= source.length()) break;

            char c = source.charAt(pos);

            if (Character.isDigit(c))           readNumber();
            else if (Character.isLetter(c) || c == '_') readIdentifierOrKeyword();
            else if (c == '"')                  readString();
            else                                readSymbol();
        }
        tokens.add(new Token(TokenType.EOF, "EOF", line));
        return tokens;
    }

    private void skipWhitespaceAndComments() {
        while (pos < source.length()) {
            char c = source.charAt(pos);
            if (c == '\n') { line++; pos++; }
            else if (Character.isWhitespace(c)) { pos++; }
            else if (pos + 1 < source.length() && source.charAt(pos) == '/' && source.charAt(pos+1) == '/') {
                // Single-line comment
                while (pos < source.length() && source.charAt(pos) != '\n') pos++;
            }
            else break;
        }
    }

    private void readNumber() {
        int start = pos;
        boolean isFloat = false;
        while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        if (pos < source.length() && source.charAt(pos) == '.') {
            isFloat = true;
            pos++;
            while (pos < source.length() && Character.isDigit(source.charAt(pos))) pos++;
        }
        tokens.add(new Token(TokenType.NUMBER, source.substring(start, pos), line));
    }

    private void readIdentifierOrKeyword() {
        int start = pos;
        while (pos < source.length() && (Character.isLetterOrDigit(source.charAt(pos)) || source.charAt(pos) == '_'))
            pos++;
        String word = source.substring(start, pos);
        TokenType type = KEYWORDS.getOrDefault(word, TokenType.IDENTIFIER);
        tokens.add(new Token(type, word, line));
    }

    private void readString() {
        pos++; // skip opening "
        int start = pos;
        while (pos < source.length() && source.charAt(pos) != '"') {
            if (source.charAt(pos) == '\n') line++;
            pos++;
        }
        String value = source.substring(start, pos);
        if (pos < source.length()) pos++; // skip closing "
        else errors.add("Line " + line + ": Unterminated string literal");
        tokens.add(new Token(TokenType.STRING, value, line));
    }

    private void readSymbol() {
        char c = source.charAt(pos);
        char next = (pos + 1 < source.length()) ? source.charAt(pos + 1) : '\0';

        switch (c) {
            case '+' -> { tokens.add(new Token(TokenType.PLUS,      "+", line)); pos++; }
            case '-' -> { tokens.add(new Token(TokenType.MINUS,     "-", line)); pos++; }
            case '*' -> { tokens.add(new Token(TokenType.STAR,      "*", line)); pos++; }
            case '/' -> { tokens.add(new Token(TokenType.SLASH,     "/", line)); pos++; }
            case '%' -> { tokens.add(new Token(TokenType.MOD,       "%", line)); pos++; }
            case '(' -> { tokens.add(new Token(TokenType.LPAREN,    "(", line)); pos++; }
            case ')' -> { tokens.add(new Token(TokenType.RPAREN,    ")", line)); pos++; }
            case '{' -> { tokens.add(new Token(TokenType.LBRACE,    "{", line)); pos++; }
            case '}' -> { tokens.add(new Token(TokenType.RBRACE,    "}", line)); pos++; }
            case ';' -> { tokens.add(new Token(TokenType.SEMICOLON, ";", line)); pos++; }
            case ',' -> { tokens.add(new Token(TokenType.COMMA,     ",", line)); pos++; }
            case '=' -> {
                if (next == '=') { tokens.add(new Token(TokenType.EQ,     "==", line)); pos += 2; }
                else             { tokens.add(new Token(TokenType.ASSIGN, "=",  line)); pos++; }
            }
            case '!' -> {
                if (next == '=') { tokens.add(new Token(TokenType.NEQ, "!=", line)); pos += 2; }
                else             { tokens.add(new Token(TokenType.NOT, "!",  line)); pos++; }
            }
            case '<' -> {
                if (next == '=') { tokens.add(new Token(TokenType.LTE, "<=", line)); pos += 2; }
                else             { tokens.add(new Token(TokenType.LT,  "<",  line)); pos++; }
            }
            case '>' -> {
                if (next == '=') { tokens.add(new Token(TokenType.GTE, ">=", line)); pos += 2; }
                else             { tokens.add(new Token(TokenType.GT,  ">",  line)); pos++; }
            }
            case '&' -> {
                if (next == '&') { tokens.add(new Token(TokenType.AND, "&&", line)); pos += 2; }
                else { errors.add("Line " + line + ": Unknown character '&'"); pos++; }
            }
            case '|' -> {
                if (next == '|') { tokens.add(new Token(TokenType.OR, "||", line)); pos += 2; }
                else { errors.add("Line " + line + ": Unknown character '|'"); pos++; }
            }
            default -> {
                errors.add("Line " + line + ": Unknown character '" + c + "'");
                tokens.add(new Token(TokenType.UNKNOWN, String.valueOf(c), line));
                pos++;
            }
        }
    }

    public List<String> getErrors() { return errors; }
}
