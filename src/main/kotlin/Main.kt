import arrow.core.Either
import arrow.core.andThen
import data.compiler.unwrap
import data.getMessage
import domain.Compiler
import domain.Parser
import domain.Repl
import domain.Tokenizer
import domain.VirtualMachine
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import util.loadFile
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    val parser = ArgParser("lispik")

    val noGlobalEnv by parser.option(
        type = ArgType.Boolean,
        shortName = "ng",
        fullName = "no-global",
        description = "Disables usage of the global environment for calling functions. Define keyword is not available in this mode and will throw an error",
    ).default(false)

    val evalOnly by parser.option(
        type = ArgType.Boolean,
        shortName = "e",
        fullName = "eval-only",
        description = "Evaluates the file on input, print results and exit. Disables REPL.",
    ).default(false)

    val compileOnly by parser.option(
        type = ArgType.Boolean,
        shortName = "c",
        fullName = "compile-only",
        description = "Just prints out compiled bytecode in human-readable form. Disables REPL.",
    ).default(false)

    val showByteCode by parser.option(
        type = ArgType.Boolean,
        shortName = "b",
        fullName = "show-bytecode",
        description = "Show compiled bytecode in the REPL mode",
    ).default(false)

    val debug by parser.option(
        type = ArgType.Boolean,
        shortName = "d",
        fullName = "debug",
        description = "Shows debug logs, lots of them",
    ).default(false)

    val filename by parser.argument(
        type = ArgType.String,
        description = "Load a library at startup and evaluates it.",
    ).optional()

    parser.parse(args)

    when {
        compileOnly -> compileOnly(filename, !noGlobalEnv)
        evalOnly -> evalOnly(filename, !noGlobalEnv)
        else ->
            Repl.createInstance().run(
                filename = filename,
                showByteCode = showByteCode,
                debug = debug,
                globalEnv = !noGlobalEnv,
            )
    }
}

private fun compileOnly(filename: String?, globalEnabled: Boolean) {
    val source = filename?.let {
        when (val res = loadFile(filename)) {
            is Either.Left -> {
                println("Failed to read file '$filename'")
                exitProcess(1)
            }

            is Either.Right -> {
                res.value
            }
        }
    } ?: run {
        println("No file given, exiting")
        exitProcess(2)
    }

    val tokenizer = Tokenizer.from(source)
    val parser = Parser.from(tokenizer)
    parser.parseToAST().andThen { globalScope ->
        Compiler.from().compile(globalScope, globalEnabled)
    }.tap {
        println(it)
    }.tapInvalid {
        println(it.getMessage())
    }
}

private fun evalOnly(filename: String?, globalEnabled: Boolean) {
    val source = filename?.let {
        when (val res = loadFile(filename)) {
            is Either.Left -> {
                println("Failed to read file '$filename'")
                exitProcess(1)
            }

            is Either.Right -> {
                res.value
            }
        }
    } ?: run {
        println("No file given, exiting")
        exitProcess(2)
    }

    val tokenizer = Tokenizer.from(source)
    val parser = Parser.from(tokenizer)
    parser.parseToAST().andThen { globalScope ->
        Compiler.from().compile(globalScope, globalEnabled)
    }.andThen {
        VirtualMachine.from().runCode(it, globalEnv = globalEnabled)
    }
        .tap { stack ->
            stack.forEach {
                println(it.unwrap())
            }
        }.tapInvalid {
            println(it.getMessage())
        }
}
