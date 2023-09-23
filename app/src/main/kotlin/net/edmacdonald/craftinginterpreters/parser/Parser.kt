package net.edmacdonald.craftinginterpreters.parser

import net.edmacdonald.craftinginterpreters.Lox
import net.edmacdonald.craftinginterpreters.scanner.Token
import net.edmacdonald.craftinginterpreters.scanner.TokenType
import net.edmacdonald.craftinginterpreters.scanner.TokenType.*

class Parser(val tokens: List<Token>) {
    var current: Int = 0

    private class ParseError : RuntimeException()

    fun parse(): List<Stmt> =
        mutableListOf<Stmt>()
            .also { statements ->
                while (!isAtEnd()) {
                    declaration()?.let {
                        statements += it
                    }
                }
            }

    private fun declaration(): Stmt? =
        try {
            if (match(VAR))
                varDeclaration()
            else
                statement()
        } catch (error: ParseError) {
            synchronize()
            null
        }

    private fun varDeclaration(): Stmt {
        val name: Token = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null;

        if (match(EQUAL))
            initializer = expression()

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun statement(): Stmt =
        when {
            match(PRINT) -> printStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }

    private fun block(): List<Stmt> =
        mutableListOf<Stmt>().also {
            while(!check(RIGHT_BRACE) && !isAtEnd()) {
                declaration()?.let { stmt ->
                    it += stmt
                }
            }
            consume(RIGHT_BRACE, "Expect '}' after block.")
        }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr =
        equality().also { expr ->
            if (match(EQUAL)) {
                val equals = previous()
                val value = assignment()

                when (expr) {
                    is Expr.Variable -> {
                        val name = expr.name
                        return Expr.Assign(name, value)
                    }

                    else -> error(equals, "Invalid assignment target.")
                }
            }
        }

    private fun equality(): Expr {
        var expr = comparison()
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            val operator = previous()
            val right = comparison()
            expr = Expr.Binary(expr, operator, right)
        }
        return expr
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: TokenType) =
        if (isAtEnd())
            false
        else
            peek().type == type

    private fun advance(): Token {
        if (!isAtEnd())
            current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == EOF

    private fun peek(): Token = tokens.get(current)

    private fun previous(): Token = tokens.get(current - 1)

    private fun comparison(): Expr {
        var expr = term()

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            val operator = previous()
            val right = term()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun term(): Expr {
        var expr = factor()

        while (match(MINUS, PLUS)) {
            val operator = previous()
            val right = factor()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun factor(): Expr {
        var expr = unary()

        while (match(SLASH, STAR)) {
            val operator = previous()
            val right = unary()
            expr = Expr.Binary(expr, operator, right)
        }

        return expr
    }

    private fun unary(): Expr {
        if (match(BANG, MINUS)) {
            val operator = previous()
            val right = unary()
            return Expr.Unary(operator, right)
        }

        return primary()
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)
        if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)
        if (match(IDENTIFIER)) return Expr.Variable(previous())

        if (match(LEFT_PAREN)) {
            val expr = expression()
            consume(RIGHT_PAREN, "Expect ')' after expression.")
            return Expr.Grouping(expr)
        }

        throw error(peek(), "Expect expression.")
    }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()

        throw error(peek(), message)
    }

    private fun error(token: Token, message: String): ParseError {
        Lox.error(token, message)
        return ParseError()
    }

    private fun synchronize() {
        advance()
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON)
                return
            when (peek().type) {
                CLASS, FOR, FUN, IF, PRINT, RETURN, VAR, WHILE -> return
                else -> advance()
            }
        }
    }
}