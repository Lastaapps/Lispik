package data.repl

import data.getMessage
import domain.model.Error
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface ReplLogger {
    companion object {
        fun getInstance(): ReplLogger = ReplLoggerImpl()
    }

    fun info(msg: () -> String)
    fun stats(msg: () -> String)
    fun result(msg: () -> String)
    fun waitForInput()
    fun empty()
    fun error(msg: () -> String)
}

private class ReplLoggerImpl : ReplLogger {

    private val now
        get() =
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString().dropLast(3)
    private val tag get() = "[$now Lispík]"

    override fun info(msg: () -> String) {
        println("$tag$ " + msg())
    }

    override fun result(msg: () -> String) {
        println("$tag: " + msg())
    }

    override fun stats(msg: () -> String) {
        println("$tag# " + msg())
    }

    override fun waitForInput() {
        print("$tag> ")
    }

    override fun empty() {
        println(tag)
    }

    override fun error(msg: () -> String) {
        println("$tag! " + msg())
    }
}

fun ReplLogger.error(error: Error, msg: () -> String) =
    error { msg() + "\n" + error.getMessage() }

fun ReplLogger.error(exception: Exception, msg: () -> String) =
    error { msg() + "\n" + exception.message }
