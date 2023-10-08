package net.edmacdonald.craftinginterpreters.visitor

import net.edmacdonald.craftinginterpreters.environment.Environment
import net.edmacdonald.craftinginterpreters.parser.Expr
import net.edmacdonald.craftinginterpreters.parser.Stmt
import net.edmacdonald.craftinginterpreters.runtimeError
import net.edmacdonald.craftinginterpreters.scanner.Token
import net.edmacdonald.craftinginterpreters.scanner.TokenType

interface LoxCallable {
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
    fun arity(): Int
}

class LoxFunction(private val declaration: Stmt.Function) : LoxCallable {
    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(interpreter.globals)

        (declaration.params zip arguments).forEach{(param, argument) ->
            environment.define(param.lexeme, argument)
        }

        interpreter.executeBlock(declaration.body, environment)
        return null
    }

    override fun arity(): Int = declaration.params.size

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}

class Interpreter(
    val globals: Environment = Environment(),
    private var environment: Environment = globals
) : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {

    init {
        globals.define("clock", object : LoxCallable {
            override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? =
                System.currentTimeMillis() / 1000.0

            override fun arity(): Int = 0
            override fun toString(): String = "<native fn>"
        })
    }

    fun interpret(statements: List<Stmt>): Unit {
        try {
            statements.forEach { statement ->
                execute(statement)
            }
        } catch (error: RuntimeError) {
            runtimeError(error)
        }
    }

    private fun execute(stmt: Stmt): Unit {
        stmt.accept(this)
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

    override fun visitVar(stmt: Stmt.Var) {
        environment.define(
            stmt.name.lexeme,
            if (stmt.initializer != null)
                evaluate(stmt.initializer)
            else
                null
        )
    }

    override fun visitWhile(it: Stmt.While) {
        while (isTruthy(evaluate(it.condition))) {
            execute(it.body)
        }
    }

    override fun visitVariable(expr: Expr.Variable): Any? = environment.get(expr.name)

    override fun visitIf(it: Stmt.If) {
        if (isTruthy(evaluate(it.condition)))
            execute(it.thenBranch)
        else
            it.elseBranch?.let { execute(it) }
    }

    override fun visitBlock(it: Stmt.Block) =
        executeBlock(it.statements, Environment(environment))

    fun executeBlock(statements: List<Stmt>, environment: Environment): Unit {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach { statement ->
                execute(statement)
            }
        } finally {
            this.environment = previous
        }
    }

    override fun visitExpression(stmt: Stmt.Expression) {
        evaluate(stmt.expression)
    }

    override fun visitFunction(stmt: Stmt.Function) {
        environment.define(stmt.name.lexeme, LoxFunction(stmt))
    }

    override fun visitPrint(stmt: Stmt.Print) {
        println(stringify(evaluate(stmt.expression)))
    }

    override fun visitAssign(expr: Expr.Assign): Any? =
        evaluate(expr.value).also { value ->
            environment.assign(expr.name, value)
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
                    else -> throw RuntimeError(operator, "unknown operator for types")
                }

            (lval is String && rval is String) ->
                when (operator.type) {
                    TokenType.PLUS -> lval + rval
                    TokenType.BANG_EQUAL -> !isEqual(left, right)
                    TokenType.EQUAL_EQUAL -> isEqual(left, right)
                    else -> throw RuntimeError(operator, "unknown operator for types")
                }

            else ->
                when (operator.type) {
                    TokenType.BANG_EQUAL -> !isEqual(left, right)
                    TokenType.EQUAL_EQUAL -> isEqual(left, right)
                    else -> throw RuntimeError(operator, "unknown operator for types")
                }
        }
    }

    override fun visitCall(expr: Expr.Call): Any? {
        val callee: Any? = evaluate(expr.callee)
        val arguments: MutableList<Any?> = mutableListOf()

        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }

        return when (callee) {
            is LoxCallable ->
                if (arguments.size != callee.arity())
                    throw RuntimeError(expr.paren, "Expected ${callee.arity()} arguments but got ${arguments.size}.")
                else
                    callee.call(this, arguments)

            else -> throw RuntimeError(expr.paren, "Can only call functions and classes.")
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

    override fun visitLogical(it: Expr.Logical): Any? {
        val left = evaluate(it.left)

        return when (it.operator.type) {
            TokenType.OR -> when {
                isTruthy(left) -> left
                else -> evaluate(it.right)
            }

            else -> when {
                !isTruthy(left) -> left
                else -> evaluate(it.right)
            }
        }
    }

    override fun visitUnary(expr: Expr.Unary): Any? {
        val right = evaluate(expr.right)

        return when (expr.operator.type) {
            TokenType.MINUS -> {
                checkNumberOperand(expr.operator, right)
                -(right as Double)
            }

            TokenType.BANG -> !isTruthy(right)
            else -> throw RuntimeError(expr.operator, "Unexpected operator type")
        }
    }

    private fun checkNumberOperand(operator: Token, operand: Any?) {
        if (operand !is Double)
            throw RuntimeError(operator, "Operand must be a number.")
    }

    private fun checkNumberOperands(operator: Token, left: Any?, right: Any?) {
        if (left !is Double || right !is Double)
            throw RuntimeError(operator, "Operands must be numbers.")
    }

    private fun evaluate(expr: Expr): Any? = expr.accept(this)

    private fun isTruthy(o: Any?): Boolean =
        when {
            (o == null) -> false
            (o is Boolean) -> o
            else -> true
        }
}