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
            when {
                match(CLASS) -> classDeclaration()
                match(FUN) -> function("function")
                match(VAR) -> varDeclaration()
                else -> statement()
            }
        } catch (error: ParseError) {
            synchronize()
            null
        }

    private fun classDeclaration(): Stmt? {
        val name = consume(IDENTIFIER, "Expect class name.")

        var superclass: Expr.Variable? = null
        if (match(LESS)) {
            consume(IDENTIFIER, "Expect superclass name.")
            superclass = Expr.Variable(previous())
        }

        consume(LEFT_BRACE, "Expect '{' before class body.")

        val methods = mutableListOf<Stmt.Function>()
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            methods.add(function("method"))
        }
        consume(RIGHT_BRACE, "Expect '}' after class body.")

        return Stmt.Class(name, superclass, methods)
    }

    private fun varDeclaration(): Stmt {
        val name: Token = consume(IDENTIFIER, "Expect variable name.")

        var initializer: Expr? = null;

        if (match(EQUAL))
            initializer = expression()

        consume(SEMICOLON, "Expect ';' after variable declaration.")
        return Stmt.Var(name, initializer)
    }

    private fun whileStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after condition.")
        val body = statement()

        return Stmt.While(condition, body)
    }

    private fun statement(): Stmt =
        when {
            match(FOR) -> forStatement()
            match(IF) -> ifStatement()
            match(PRINT) -> printStatement()
            match(RETURN) -> returnStatement()
            match(WHILE) -> whileStatement()
            match(LEFT_BRACE) -> Stmt.Block(block())
            else -> expressionStatement()
        }

    private fun forStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'for'.")

        val initializer = when {
            match(SEMICOLON) -> null
            match(VAR) -> varDeclaration()
            else -> expressionStatement()
        }

        var condition = when {
            !check(SEMICOLON) -> expression()
            else -> null
        }

        consume(SEMICOLON, "Expect ';' after loop condition.")

        val increment = when {
            !check(RIGHT_PAREN) -> expression()
            else -> null
        }

        consume(RIGHT_PAREN, "Expect ')' after for clauses.")

        var body = statement()

        increment?.let {
            body = Stmt.Block(listOf(body, Stmt.Expression(it)))
        }

        condition = condition ?: Expr.Literal(true)

        body = Stmt.While(condition, body)

        initializer?.let {
            body = Stmt.Block(listOf(it, body))
        }

        return body
    }

    private fun block(): List<Stmt> =
        mutableListOf<Stmt>().also {
            while (!check(RIGHT_BRACE) && !isAtEnd()) {
                declaration()?.let { stmt ->
                    it += stmt
                }
            }
            consume(RIGHT_BRACE, "Expect '}' after block.")
        }

    private fun ifStatement(): Stmt {
        consume(LEFT_PAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(RIGHT_PAREN, "Expect ')' after if condition.")

        val thenBranch = statement()
        var elseBranch: Stmt? = if (match(ELSE)) statement() else null

        return Stmt.If(condition, thenBranch, elseBranch)
    }

    private fun printStatement(): Stmt {
        val value = expression()
        consume(SEMICOLON, "Expect ';' after value.")
        return Stmt.Print(value)
    }

    private fun returnStatement(): Stmt {
        val keyword = previous()
        var value: Expr? = null
        if (!check(SEMICOLON)) {
            value = expression()
        }
        consume(SEMICOLON, "Expect ';' after return value.")
        return Stmt.Return(keyword, value)
    }

    private fun expressionStatement(): Stmt {
        val expr = expression()
        consume(SEMICOLON, "Expect ';' after expression.")
        return Stmt.Expression(expr)
    }

    private fun function(kind: String): Stmt.Function {
        val name = consume(IDENTIFIER, "Expect ${kind} name.")
        consume(LEFT_PAREN, "Expect '(' after ${kind} name.")
        val parameters = mutableListOf<Token>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size >= 255) {
                    error(peek(), "Can't have more than 255 parameters.")
                }
                parameters += consume(IDENTIFIER, "Expect parameter name.")
            } while (match(COMMA))
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters.")
        consume(LEFT_BRACE, "Expect '{' before ${kind} body.")
        val body = block()
        return Stmt.Function(name, parameters, body)
    }

    private fun expression(): Expr = assignment()

    private fun assignment(): Expr =
        or().also { expr ->
            if (match(EQUAL)) {
                val equals = previous()
                val value = assignment()

                when (expr) {
                    is Expr.Variable -> {
                        val name = expr.name
                        return Expr.Assign(name, value)
                    }

                    is Expr.Get -> {
                        val get = expr;
                        return Expr.Set(get.obj, get.name, value)
                    }

                    else -> error(equals, "Invalid assignment target.")
                }
            }
        }

    private fun or(): Expr {
        var expr = and()
        while (match(OR)) {
            val operator = previous()
            val right = and()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
    }

    private fun and(): Expr {
        var expr = equality()
        while (match(AND)) {
            val operator = previous()
            val right = equality()
            expr = Expr.Logical(expr, operator, right)
        }
        return expr
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

        return call()
    }

    private fun call(): Expr {
        var expr = primary()

        while (true) {
            if (match(LEFT_PAREN)) {
                expr = finishCall(expr)
            } else if (match(DOT)) {
                val name = consume(IDENTIFIER, "Expect property name after '.'.")
                expr = Expr.Get(expr, name)
            } else {
                break
            }
        }

        return expr
    }

    private fun finishCall(callee: Expr): Expr {
        val arguments = mutableListOf<Expr>()
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size >= 255) {
                    error(peek(), "Can't have more than 255 arguments.")
                }
                arguments += expression()
            } while (match(COMMA))
        }

        val paren = consume(RIGHT_PAREN, "Expect ')' after arguments.")

        return Expr.Call(callee, paren, arguments)
    }

    private fun primary(): Expr {
        if (match(FALSE)) return Expr.Literal(false)
        if (match(TRUE)) return Expr.Literal(true)
        if (match(NIL)) return Expr.Literal(null)
        if (match(NUMBER, STRING)) return Expr.Literal(previous().literal)
        if (match(SUPER)) {
            val keyword = previous()
            consume(DOT, "Expect '.' after 'super'.")
            val method = consume(IDENTIFIER, "Expect superclass method name.")
            return Expr.Super(keyword, method)
        }
        if (match(THIS)) return Expr.This(previous())
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