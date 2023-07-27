package net.edmacdonald.craftinginterpreters.environment

import net.edmacdonald.craftinginterpreters.scanner.Token
import net.edmacdonald.craftinginterpreters.visitor.RuntimeError

class Environment {
    private val values: MutableMap<String, Any> = mutableMapOf()

    // Kotlin won't let you put a null value in a Map
    private val nullVariables : MutableSet<String> = mutableSetOf()

    fun define(name: String, value: Any?): Any? =
        when {
            value != null -> values.put(name, value)
            else -> nullVariables.add(name)
        }

    fun get(name: Token): Any? =
        when {
            values.contains(name.lexeme) -> values.get(name.lexeme)
            nullVariables.contains(name.lexeme) -> null
            else -> throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
}