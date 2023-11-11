package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.Lox
import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.parser.Stmt
import net.edmacdonald.craftinginterpreters.scanner.Token
import java.util.*

enum class FunctionType {
    NONE, FUNCTION
}
class Resolver(
    private val interpreter: Interpreter,
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack<MutableMap<String, Boolean>>(),
    private var currentFunction: FunctionType = FunctionType.NONE
) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {

    fun resolve(statements: List<Stmt>) = statements.forEach { resolve(it) }
    private fun resolve(stmt: Stmt) = stmt.accept(this)
    private fun resolve(expr: Expr) = expr.accept(this)
    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type

        beginScope()
        function.params.forEach { param ->
            declare(param)
            define(param)
        }
        resolve(function.body)
        endScope()

        currentFunction = enclosingFunction
    }

    private fun beginScope() = scopes.push(HashMap())
    private fun endScope() = scopes.pop()
    private fun declare(name: Token) {
        if (!scopes.empty()) {
            val scope = scopes.peek()
            if(scope.containsKey(name.lexeme)){
                Lox.error(name, "Already a variable with this name in this scope.")
            }
            scope[name.lexeme] = false
        }
    }

    private fun define(name: Token) {
        if (!scopes.empty())
            scopes.peek()[name.lexeme] = true
    }

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in scopes.lastIndex downTo 0) {
            if (scopes.get(i).containsKey(name.lexeme))
                interpreter.resolve(expr, scopes.lastIndex - i)
        }
    }

    override fun visitIf(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.also { resolve(it) }
    }

    override fun visitBlock(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitExpression(it: Stmt.Expression) = resolve(it.expression)

    override fun visitFunction(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitPrint(stmt: Stmt.Print) = resolve(stmt.expression)

    override fun visitReturn(stmt: Stmt.Return) {
        if(currentFunction == FunctionType.NONE){
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }
        stmt.value?.let { resolve(it) }
    }

    override fun visitWhile(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitVar(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.also { resolve(it) }
        define(stmt.name)
    }

    override fun visitAssign(it: Expr.Assign) {
        resolve(it.value)
        resolveLocal(it, it.name)
    }

    override fun visitBinary(expr: Expr.Binary) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitCall(expr: Expr.Call) {
        resolve(expr.callee)
        for (argument: Expr in expr.arguments) {
            resolve(argument)
        }
    }

    override fun visitGrouping(expr: Expr.Grouping) = resolve(expr.expression)

    override fun visitLiteral(it: Expr.Literal) {}

    override fun visitLogical(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitUnary(expr: Expr.Unary) = resolve(expr.right)

    override fun visitVariable(expr: Expr.Variable) {
        if (!scopes.isEmpty() &&
            scopes.peek()[expr.name.lexeme] == false
        ) {
            Lox.error(expr.name, "Can't read local variable in its own initializer.")
        }
        resolveLocal(expr, expr.name)
    }
}