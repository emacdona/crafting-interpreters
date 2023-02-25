package net.edmacdonald.craftinginterpreters

import net.edmacdonald.craftinginterpreters.parser.Expr

class AstPrinter : Expr.Visitor<String> {
    fun print(expr: Expr): String =
        expr.accept(this)

    private fun parenthesize(name: String, vararg exprs: Expr): String {
        val builder = StringBuilder()
        builder.append("(").append(name)
        for (expr in exprs) {
            builder.append(" ")
            builder.append(expr.accept(this))
        }
        builder.append(")")
        return builder.toString()
    }

    override fun visitBinary(it: Expr.Binary): String =
        parenthesize(it.operator.lexeme, it.left, it.right)

    override fun visitGrouping(it: Expr.Grouping): String =
        parenthesize("group", it.expression)

    override fun visitLiteral(it: Expr.Literal): String =
        if (it.value == null) "nil" else it.value.toString()

    override fun visitUnary(it: Expr.Unary): String =
        parenthesize(it.operator.lexeme, it.right)
}