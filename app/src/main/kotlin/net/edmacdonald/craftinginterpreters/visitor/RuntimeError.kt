package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.scanner.Token

class RuntimeError(val token: Token, message: String) : RuntimeException(message)