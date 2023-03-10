package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.scanner.TokenType

class Interpreter : Expr.Visitor<Any?> {
    fun interpret(expression: Expr): String {
        val value = evaluate(expression)
        return stringify(value)
    }

    fun stringify(o: Any?): String =
        when (o) {
            null -> "nil"
            is Double -> {
                var text = o.toString()
                if (text.endsWith(".0")) {
                    text = text.substring(0, text.length - 2)
                }
                text
            }

            else -> o.toString()
        }

    override fun visitBinary(expr: Expr.Binary): Any? = expr.run {
        val lval = evaluate(left)
        val rval = evaluate(right)

        return when {
            (lval is Double && rval is Double) ->
                when (operator.type) {
                    TokenType.MINUS -> lval - rval
                    TokenType.SLASH -> lval / rval
                    TokenType.STAR -> lval * rval
                    TokenType.PLUS -> lval + rval
                    TokenType.GREATER -> lval > rval
                    TokenType.GREATER_EQUAL -> lval >= rval
                    TokenType.LESS -> lval < rval
                    TokenType.LESS_EQUAL -> lval <= rval
                    TokenType.BANG_EQUAL -> !isEqual(left, right)
                    TokenType.EQUAL_EQUAL -> isEqual(left, right)
                    else -> throw RuntimeException("unknown operator for types")
                }

            (lval is String && rval is String) ->
                when (operator.type) {
                    TokenType.PLUS -> lval + rval
                    TokenType.BANG_EQUAL -> !isEqual(left, right)
                    TokenType.EQUAL_EQUAL -> isEqual(left, right)
                    else -> throw RuntimeException("unknown operator for types")
                }

            else ->
                when (operator.type) {
                    TokenType.BANG_EQUAL -> !isEqual(left, right)
                    TokenType.EQUAL_EQUAL -> isEqual(left, right)
                    else -> throw RuntimeException("unknown operator for types")
                }
        }
    }

    private fun isEqual(a: Any?, b: Any?): Boolean =
        when {
            (a == null && b == null) -> true
            (a == null) -> false
            else -> a == b
        }

    override fun visitGrouping(expr: Expr.Grouping): Any? = evaluate(expr.expression)

    override fun visitLiteral(expr: Expr.Literal): Any? = expr.value

    override fun visitUnary(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS ->
                when (right) {
                    is Double -> -right
                    else -> throw RuntimeException("Expected double")
                }

            TokenType.BANG -> !isTruthy(right)
            else -> throw RuntimeException("Unexpected operator type")
        }
    }

    private fun evaluate(expr: Expr): Any? = expr.accept(this)

    private fun isTruthy(o: Any?): Boolean =
        when {
            (o == null) -> false
            (o is Boolean) -> o
            else -> true
        }
}