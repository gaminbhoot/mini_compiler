# Mini Compiler — Java
## Compiler Construction Semester Project

A fully functional mini compiler written in Java that demonstrates all major phases of compilation.

## Files
| File | Phase | Description |
|------|-------|-------------|
| `TokenType.java` | - | Enum of all token types |
| `Token.java` | - | Token data class |
| `Lexer.java` | Phase 1 | Lexical Analysis — tokenizes source code |
| `ASTNode.java` | - | AST node class hierarchy |
| `Parser.java` | Phase 2 | Syntax Analysis — builds Abstract Syntax Tree |
| `SemanticAnalyzer.java` | Phase 3 | Semantic Analysis — type checking, symbol table |
| `IntermediateCodeGen.java` | Phase 4 | Intermediate Code — Three-Address Code (TAC) |
| `CodeGenerator.java` | Phase 5 | Code Generation — Pseudo-Assembly output |
| `Interpreter.java` | Phase 6 | Execution — runs the program and shows output |
| `Main.java` | Driver | Orchestrates all phases with colored output |

## How to Compile and Run
```bash
javac *.java
java Main
```
Or to compile a custom source file:
```bash
java Main myprogram.txt
```

## Supported Language Syntax
```
// Variable declaration
int x = 10;
float f = 3.14;
string s = "hello";
bool b = true;

// Assignment
x = x + 1;

// If-Else
if (x > 5) {
    print(x);
} else {
    print(0);
}

// While loop
while (i <= 10) {
    i = i + 1;
}

// Print
print(x + y);
```

## Compiler Phases Explained

1. **Lexical Analysis** — Reads characters, produces tokens
2. **Syntax Analysis** — Checks grammar, builds AST
3. **Semantic Analysis** — Type checks, symbol table
4. **Intermediate Code** — Platform-independent Three-Address Code
5. **Code Generation** — Pseudo-assembly target code
6. **Execution** — Interpreter runs the program
