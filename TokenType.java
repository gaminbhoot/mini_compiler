public enum TokenType {
    // Literals
    NUMBER, STRING, IDENTIFIER,

    // Keywords
    INT, FLOAT, STRING_TYPE, BOOL,
    IF, ELSE, WHILE, FOR,
    PRINT, RETURN, TRUE, FALSE,

    // Operators
    PLUS, MINUS, STAR, SLASH, MOD,
    ASSIGN,
    EQ, NEQ, LT, GT, LTE, GTE,
    AND, OR, NOT,

    // Delimiters
    LPAREN, RPAREN,
    LBRACE, RBRACE,
    SEMICOLON, COMMA,

    // Special
    EOF, UNKNOWN
}
