package domain

import data.repl.ReplImpl

interface Repl {

    companion object {
        fun createInstance() = ReplImpl()
    }

    fun run(
        filename: String?,
        showByteCode: Boolean,
        debug: Boolean,
        globalEnv: Boolean,
    )
}