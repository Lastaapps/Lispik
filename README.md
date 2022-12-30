# Lispík - TinyLisp interpreter

TinyList is an implementation of a Lisp like language. This project contains lexer, parser, compiler and VM for running
List programs. It uses SECD machine under the hood.

It was developed by Petr Laštovička as semester theses for the subject Programing paradigms at the Faculty of
Information technology CTU Prague University in 2022.

## Example usage

### Building

Project uses Gradle system to build. Open it in IntelliJ or run the `build.sh` script.

### Differences from standard SECD

To implement function calling, I modified semantics of some SECD compiler instructions.
The bottom of the environment stack stores functions and instructions respect it.
Accessing function env is done using the `Ld [-1 . x]`.
You can disable this behaviour with a flag, but then `defun` cannot be used.

### Running

Code is written in Kotlin, compile JVM version is set to **Java 11**.
You can run the generated jar using the `run.sh args` script for better convenience.
To view all the options, run `run.sh -h`.
The script opens REPL by default, it accepts a filename as argument to load functions from.
Also, expressions in that files are evaluated and printed.
This is the only way to load functions, REPL will show an error if you try to do it later.

### Supported build-ins

To view supported build-ins, view the `src/main/kotlin/domain/model/Node.kt` and `FunToken.kt` files.

### Supported SECD instructions

To view supported SECD instructions, view the `src/main/kotlin/domain/model/ByteCode.kt` and `ByteInstructions.kt`
files.

### Tokens, parsing, compilation, runtime

View the adequate package in the `src/main/kolin/data` folder.

### Example inputs

Example inputs are located in the 'lispik/examples' folder.

Of course there are many more in the tests inside the project

*let syntax is Tiny as this Lisp, multiple variables in one statement is not supported
and therefore the syntax is only with one bracket pair (for now).*

```shell
# Basic REPL
./run.sh
# Merge sort lib, call '(merge-sort x)` from REPL
./run.sh examples/merge-sort.lsp
# Factorial implemented using defun
./run.sh examples/factorial.lsp
# Factorial implemented using letrec (cannot be called from REPL obviously),
# see eval res when repl is started or use the -e option
./run.sh examples/factorial-recursive.lsp
# Map and fold implementation working just fine
./run.sh examples/map-fold.lsp
# Showcase of same basic math operators in action
./run.sh examples/pow-mod.lsp
# Write two numbers and you will get list of operators applied to them
# Then enter a list and you will get it's last element
./run.sh examples/read-print.lsp
```

### Testing

Tokenizer, Parser and Compiler with VM are located in the `src/test/kotlin/data` folder.

## Sources

- https://courses.fit.cvut.cz/BIE-PPA/files/lectures/BIE-PPA-7.pdf
- https://gitlab.fit.cvut.cz/BI-PPA/bi-ppa
- https://gitlab.fit.cvut.cz/majpetr/tinylisp
- Peter Kogge: Architecture of Symbolic Computers

## License
Project is licensed under the GNU GPL version 3.0 license.
