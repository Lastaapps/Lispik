package data.repl

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Validated
import arrow.core.andThen
import arrow.core.getOrElse
import arrow.core.invalid
import arrow.core.some
import arrow.core.valid
import arrow.core.valueOr
import data.compiler.unwrap
import data.parsing.GlobalScope
import domain.Compiler
import domain.Parser
import domain.Repl
import domain.Tokenizer
import domain.VirtualMachine
import domain.model.Error
import kotlinx.datetime.Clock
import util.loadFile
import kotlin.system.exitProcess

class ReplImpl(
    private val tokenizerProvider: (String) -> Tokenizer = { Tokenizer.from(it) },
    private val parserProvider: (Tokenizer) -> Parser = { Parser.from(it) },
    private val compiler: Compiler = Compiler.from(),
    private val vm: VirtualMachine = VirtualMachine.from(),
) : Repl {

    companion object {
        private val log = ReplLogger.getInstance()
    }

    override fun run(
        filename: String?,
        showByteCode: Boolean,
        debug: Boolean,
        globalEnv: Boolean,
    ) {
        log.info { "Welcome to Lispík!" }

        val source = filename?.let {
            when (val res = loadFile(filename)) {
                is Either.Left -> {
                    log.error(res.value) { "Failed to read file '$filename'" }
                    exitProcess(1)
                }

                is Either.Right -> {
                    log.info { "Loaded file '$filename'" }
                    res.value
                }
            }
        } ?: run {
            log.info { "No file loaded" }
            ""
        }

        val global = parseSource(source)
            .valueOr {
                log.error(it) { "Failed to parse the input file" }
                exitProcess(2)
            }

        // execute in file expressions
        compileInput(global, globalEnv)
            .tap {
                if (showByteCode) {
                    log.stats { "Compiled bytecode: $it" }
                }
            }
            .andThen { vm.runCode(it, debug, globalEnv) }
            .tap { results ->
                if (results.isEmpty()) return@tap
                log.result { "Stack result after code evaluation:" }
                results.forEach { log.result { it.unwrap() } }
            }
            .tapInvalid {
                log.error(it) { "Execution failed" }
                exitProcess(3)
            }

        runRepl(global, showByteCode, debug, globalEnv)
    }

    private fun runRepl(
        global: GlobalScope,
        showByteCode: Boolean,
        debug: Boolean,
        globalEnv: Boolean
    ) {
        while (true) {
            log.waitForInput()
            val line = readFromStdIn() ?: break

            val timeStart = Clock.System.now()

            parseSource(line)
                .andThen {
                    if (it.functions.isNotEmpty())
                        Error.Repl.YouCannotDefineFunctionsInRepl.invalid()
                    else it.valid()
                }
                .andThen { local ->
                    compileAdditional(global, local, globalEnv).map {
                        it to Clock.System.now() // record compile time
                    }
                }

                .tap {
                    if (showByteCode) {
                        log.stats { "Compiled bytecode: ${it.first}" }
                    }
                }

                .andThen { (code, compileTime) ->
                    vm.runCode(code, debug = debug, globalEnv = globalEnv)
                        .map { it to compileTime }
                }

                .tap { (results, compileTime) ->

                    results.forEach { log.result { it.unwrap() } }

                    log.stats { "Compilation: ${compileTime - timeStart}" }
                    log.stats { "Execution:   ${Clock.System.now() - compileTime}" }
                    log.empty()
                }
                .tapInvalid {
                    log.error(it) { "Execution failed" }
                }
        }

        println()
        log.info { "Bye, see you." }
    }

    private fun parseSource(source: String): Validated<Error, GlobalScope> {
        val tokenizer = tokenizerProvider(source)
        val parser = parserProvider(tokenizer)
        return parser.parseToAST()
    }

    private fun compileInput(globalScope: GlobalScope, enableGlobalEnv: Boolean) =
        compiler.compile(globalScope, enableGlobalEnv)

    private fun compileAdditional(globalScope: GlobalScope, localScope: GlobalScope, enableGlobalEnv: Boolean) =
        globalScope.copy(expressions = localScope.expressions).let { combined ->
            compiler.compile(combined, enableGlobalEnv)
        }

    private fun readFromStdIn(): String? {
        StringBuilder().let { builder ->
            while (true) {
                val char = readChar().getOrElse {
                    return if (builder.isEmpty())
                        null
                    else
                        builder.toString()
                }

                when {
                    char == '\n' && builder.lastOrNull() != '\\' ->
                        return builder.toString()

                    char == '\n' && builder.lastOrNull() == '\\' ->
                        builder[builder.lastIndex] = '\n'

                    else -> builder.append(char)
                }
            }
        }
    }

    private fun readChar(): Option<Char> =
        System.`in`.read().let {
            if (it < 0) None else it.toChar().some()
        }
}
