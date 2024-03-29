package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.Lox
import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.parser.Stmt
import net.edmacdonald.craftinginterpreters.scanner.Token
import java.util.*
import kotlin.math.exp

enum class FunctionType {
    NONE, FUNCTION, INITIALIZER, METHOD
}

enum class ClassType {
    NONE, CLASS, SUBCLASS
}

class Resolver(
    private val interpreter: Interpreter,
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack<MutableMap<String, Boolean>>(),
    private var currentFunction: FunctionType = FunctionType.NONE,
    private var currentClass: ClassType = ClassType.NONE
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
            if (scope.containsKey(name.lexeme)) {
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

    override fun visitClass(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS
        declare(stmt.name)
        define(stmt.name)

        if (stmt.superclass != null &&
            stmt.name.lexeme.equals(stmt.superclass.name.lexeme)
        ) {
            Lox.error(
                stmt.superclass.name,
                "A class can't inherit from itself."
            )
        }

        if (stmt.superclass != null) {
            currentClass = ClassType.SUBCLASS
            resolve(stmt.superclass)
        }

        if (stmt.superclass != null) {
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek().put("this", true)

        stmt.methods.forEach { method ->
            val declaration = if (method.name.lexeme.equals("init")) {
                FunctionType.INITIALIZER
            } else {
                FunctionType.METHOD
            }
            resolveFunction(method, declaration)
        }

        endScope()

        if (stmt.superclass != null) {
            endScope()
        }

        currentClass = enclosingClass
    }

    override fun visitExpression(it: Stmt.Expression) = resolve(it.expression)

    override fun visitFunction(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitPrint(stmt: Stmt.Print) = resolve(stmt.expression)

    override fun visitReturn(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code.")
        }
        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return a value from an initializer.")
            } else {
                resolve(it)
            }
        }
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

    override fun visitGet(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitGrouping(expr: Expr.Grouping) = resolve(expr.expression)

    override fun visitLiteral(it: Expr.Literal) {}

    override fun visitLogical(expr: Expr.Logical) {
        resolve(expr.left)
        resolve(expr.right)
    }

    override fun visitSet(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitSuper(expr: Expr.Super) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword,
                "Can't use 'super' outside of a class.")
        } else if (currentClass != ClassType.SUBCLASS) {
            Lox.error(expr.keyword,
                "Can't use 'super' in a class with no superclass.")
        }

        resolveLocal(expr, expr.keyword)
    }

    override fun visitThis(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class.")
        } else {
            resolveLocal(expr, expr.keyword)
        }
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