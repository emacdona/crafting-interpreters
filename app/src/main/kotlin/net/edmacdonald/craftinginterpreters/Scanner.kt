package net.edmacdonald.craftinginterpreters

import net.edmacdonald.craftinginterpreters.TokenType.*

class Scanner(var source: String) {
    companion object {
        val keywords = mapOf(
            "and" to AND,
            "class" to CLASS,
            "else" to ELSE,
            "false" to FALSE,
            "for" to FOR,
            "fun" to FUN,
            "if" to IF,
            "nil" to NIL,
            "or" to OR,
            "print" to PRINT,
            "return" to RETURN,
            "super" to SUPER,
            "this" to THIS,
            "true" to TRUE,
            "var" to VAR,
            "while" to WHILE
        )
    }

    val tokens: MutableList<Token> = mutableListOf()
    var start = 0
    var current = 0
    var line = 0

    fun scanTokens(): List<Token> {
        while (!isAtEnd()) {
            start = current
            scanToken()
        }

        tokens.add(Token(EOF, "", null, line))
        return tokens
    }

    private fun isAtEnd(): Boolean =
        current >= source.length

    private fun scanToken() {
        var c = advance()
        when (c) {
            '(' -> addToken(LEFT_PAREN)
            ')' -> addToken(RIGHT_PAREN)
            '{' -> addToken(LEFT_BRACE)
            '}' -> addToken(RIGHT_BRACE)
            ',' -> addToken(COMMA)
            '.' -> addToken(DOT)
            '-' -> addToken(MINUS)
            '+' -> addToken(PLUS)
            ';' -> addToken(SEMICOLON)
            '*' -> addToken(STAR)

            '!' -> addToken(if (match('=')) BANG_EQUAL else BANG)
            '=' -> addToken(if (match('=')) EQUAL_EQUAL else EQUAL)
            '<' -> addToken(if (match('=')) LESS_EQUAL else LESS)
            '>' -> addToken(if (match('=')) GREATER_EQUAL else GREATER)

            '/' -> {
                if (match('/')) {
                    while (peek() != '\n' && !isAtEnd())
                        advance()
                } else {
                    addToken(SLASH)
                }
            }

            ' ', '\r', '\t' -> {}
            '\n' -> line++

            '"' -> string()

            else ->
                if (isDigit(c)) {
                    number()
                } else if (isAlpha(c)) {
                    identifier()
                } else {
                    error(line, "Unexpected character")
                }
        }
    }

    private fun advance(): Char =
        source[current++]

    private fun addToken(type: TokenType) =
        addToken(type, null)

    private fun addToken(type: TokenType, literal: Any?) {
        val text = source.substring(start, current)
        tokens.add(Token(type, text, literal, line))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd()) return false
        if (source[current] != expected) return false
        current++
        return true
    }

    private fun peek(): Char =
        if (isAtEnd())
            0.toChar()
        else
            source[current]

    private fun string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n')
                line++
            advance()
        }

        if (isAtEnd()) {
            error(line, "Unterminated string.")
            return
        }

        advance()

        // Trim surrounding quotes
        val value = source.substring(start + 1, current - 1);
        addToken(STRING, value)
    }

    private fun isDigit(c: Char): Boolean =
        c in '0'..'9'

    private fun number() {
        while (isDigit(peek()))
            advance()

        if (peek() == '.' && isDigit(peekNext())) {
            advance()

            while (isDigit(peek()))
                advance()
        }

        addToken(
            NUMBER,
            source.substring(start, current).toDouble()
        )
    }

    private fun peekNext(): Char =
        if (current + 1 >= source.length)
            0.toChar()
        else
            source[current + 1]

    private fun identifier() {
        while (isAlphaNumeric(peek()))
            advance()

        addToken(
            keywords.getOrDefault(
                source.substring(start, current),
                IDENTIFIER
            )
        )
    }

    private fun isAlpha(c: Char): Boolean =
        (c in 'a'..'z')
                || (c in 'A'..'Z')
                || c == '_'

    private fun isAlphaNumeric(c: Char) =
        isAlpha(c) || isDigit(c)
}