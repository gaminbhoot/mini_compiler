#!/bin/bash
echo "Compiling Mini Compiler..."
javac *.java
if [ $? -eq 0 ]; then
    echo "Running Mini Compiler..."
    java Main
else
    echo "Compilation failed."
fi
