# Lispík - TinyLisp interpreter

### Task assignment

> TinyLisp je implementace velmi malého lisp-like jazyka. Obsahuje malý, ale funkční, lexer, parser, runtime, garbage kolektor, SECD překladač a jeho VM. Kód je pro vás k dispozici a můžete se na něj podívat.
> Vaše aplikace musí:
> - překládat a interpretovat základní LISPovské konstrukce,
> - podporovat všechny SECD instrukce (kromě RAP)
>
> Extra body jsou za následující rozšíření.
> - podpora instrukce RAP

### Describe which features were implemented.

I have implement both the basic functionality and the extended one.

## Example usage

>Describe the usage of your program.
>Attach some example inputs that show the functionality of your program (for every mandatory and optional feature).

### Building
Run the `build.sh` script. But there will be the latest build from me already in this folder.

### Testing
Tokenizer, Parser and Compiler with VM are located in the `src/test/kotlin/data` folder.

### Differences from standard SECD
To implement function calling, I modified semantics of some SECD compiler instructions.
The bottom of the environment stack stores functions and instructions respect it.
Accessing function env is done using the `Ld [-1 . x]`.
You can disable this behaviour with a flag, but then `defun` cannot be used.

### Running
You can run the generated jar using the `run.sh args` script for better convenience.
To view all the options (just for fun), run `run.sh -h`.
The script opens REPL by default, it accepts a filename as argument to load functions from.
Also, expressions in that files are evaluated and printed.
This is the only way to load functions, REPL will show an error if you try to do it later.

### Supported build-ins
To view supported build-ins, view the `src/main/kotlin/domain/model/Node.kt` and `FunToken.kt` files.

### Supported SECD instructions
To view supported SECD instructions, view the `src/main/kotlin/domain/model/ByteCode.kt` and `ByteInstructions.kt` files.

### Tokens, parsing, compilation, runtime
View the adequate package in the `src/main/kolin/data` folder.

### Example inputs
Example inputs are located in the 'lispik/examples' folder.

Of course there are many more in the tests inside the project

*let syntax is Tiny as this Lisp, multiple variables in one statement is not supported
and therefore the syntax is only with one bracket pair.*

```shell
# Basic REPL
./run.sh
# Merge sort lib, call '(merge-sort x)` from REPL
./run.sh lispik/examples/merge-sort.lsp
# Factorial implemented using defun
./run.sh lispik/examples/factorial.lsp
# Factorial implemented using letrec (cannot be called from REPL obviously),
# see eval res when repl is started or use the -e option
./run.sh lispik/examples/factorial-recursive.lsp
# Map and fold implementation working just fine
./run.sh lispik/examples/map-fold.lsp
# Showcase of same basic math operators in action
./run.sh lispik/examples/pow-mod.lsp
# Write two numbers and you will get list of operators applied to them
# Then enter a list and you will get it's last element
./run.sh lispik/examples/read-print.lsp
```

// TODO zero, =, not, and, or, equal, atom?
// TODO read list without quote

## Sources
 - https://courses.fit.cvut.cz/BIE-PPA/files/lectures/BIE-PPA-7.pdf
 - https://gitlab.fit.cvut.cz/BI-PPA/bi-ppa
 - https://gitlab.fit.cvut.cz/majpetr/tinylisp
 - Peter Kogge: Architecture of Symbolic Computers




