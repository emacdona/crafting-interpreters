package net.edmacdonald.craftinginterpreters.environment

import net.edmacdonald.craftinginterpreters.scanner.Token
import net.edmacdonald.craftinginterpreters.visitor.RuntimeError

class Environment {
    private val values: MutableMap<String, Any?> = mutableMapOf()

    fun define(name: String, value: Any?): Any? =
        values.put(name, value)

    fun assign(name: Token, value: Any?): Unit =
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
        } else {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }

    fun get(name: Token): Any? =
        values.getOrElse(name.lexeme) {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
}