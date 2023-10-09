package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.Lox
import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.parser.Stmt
import net.edmacdonald.craftinginterpreters.scanner.Token
import java.util.Stack

class Resolver(
    private val interpreter: Interpreter,
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack<MutableMap<String, Boolean>>()
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    private fun resolve(statements: List<Stmt>) = statements.forEach { resolve(it) }
    private fun resolve(stmt: Stmt) = stmt.accept(this)
    private fun resolve(expr: Expr) = expr.accept(this)
    private fun beginScope() = scopes.push(HashMap())
    private fun endScope() = scopes.pop()
    private fun declare(name: Token) {
        if (!scopes.empty())
            scopes.peek()[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (!scopes.empty())
            scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token){
        for(i in scopes.lastIndex downTo 0){
            if(scopes.get(i).containsKey(name.lexeme))
                interpreter.resolve(expr, scopes.lastIndex - i)
        }
    }

    override fun visitIf(it: Stmt.If) {}

    override fun visitBlock(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpression(it: Stmt.Expression) {}

    override fun visitFunction(it: Stmt.Function) {}

    override fun visitPrint(it: Stmt.Print) {}

    override fun visitReturn(it: Stmt.Return) {}

    override fun visitWhile(it: Stmt.While) {}

    override fun visitVar(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.also { resolve(it) }
        define(stmt.name)
    }

    override fun visitAssign(it: Expr.Assign) {}

    override fun visitBinary(it: Expr.Binary) {}

    override fun visitCall(it: Expr.Call) {}

    override fun visitGrouping(it: Expr.Grouping) {}

    override fun visitLiteral(it: Expr.Literal) {}

    override fun visitLogical(it: Expr.Logical) {}

    override fun visitUnary(it: Expr.Unary) {}

    override fun visitVariable(expr: Expr.Variable) {
        if (!scopes.isEmpty() &&
            scopes.peek()[expr.name.lexeme] == false
        ) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }
}