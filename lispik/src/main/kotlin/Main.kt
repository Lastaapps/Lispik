import domain.Repl

fun main(args: Array<String>) {
    Repl.createInstance().run(args.getOrNull(0), true)
}
