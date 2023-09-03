package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.parser.Stmt

class AstPrinter : Expr.Visitor<String>, Stmt.Visitor<String>{
    fun print(statements: List<Stmt>): String =
        "(program ${
            statements
                .map { stmt -> stmt.accept(this) }
                .joinToString(" ")
        })"

    private fun parenthesize(names: List<String>, vararg exprs: Expr?): String =
        parenthesize(names.joinToString(separator = " "), exprs.asList())

    private fun parenthesize(name: String, vararg exprs: Expr?): String =
        parenthesize(name, exprs.asList())

    private fun parenthesize(name: String, exprs: List<Expr?>): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(
                when (expr) {
                    null -> "nil"
                    else -> expr.accept(this)
                }
            )
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visitExpression(it: Stmt.Expression): String {
        return it.expression.accept(this)
    }

    override fun visitPrint(it: Stmt.Print): String {
        return parenthesize("print", it.expression)
    }

    override fun visitAssign(it: Expr.Assign): String {
        return parenthesize(listOf("=", it.name.lexeme), it.value)
    }

    override fun visitBinary(it: Expr.Binary): String =
        parenthesize(it.operator.lexeme, it.left, it.right)

    override fun visitGrouping(it: Expr.Grouping): String =
        parenthesize("group", it.expression)

    override fun visitLiteral(it: Expr.Literal): String =
        when(it.value){
            null -> "nil"
            is String -> "\"${it.value.toString()}\""
            else -> it.value.toString()
        }

    override fun visitUnary(it: Expr.Unary): String =
        parenthesize(it.operator.lexeme, it.right)

    override fun visitVar(it: Stmt.Var): String =
        parenthesize(listOf("var", it.name.lexeme), it.initializer)

    override fun visitVariable(it: Expr.Variable): String = it.name.lexeme
}