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

class LoxClass(val name: String, val superclass: LoxClass?, val methods: Map<String, LoxFunction>) : LoxCallable {

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) {
            return methods[name]
        } else if (superclass != null) {
            return superclass.findMethod(name)
        } else {
            return null
        }
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")

        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments)
        }

        return instance
    }

    override fun arity(): Int {
        val initializer = findMethod("init")
        if (initializer == null) return 0
        return initializer.arity()
    }

    override fun toString(): String = name
}

class LoxInstance(
    val klazz: LoxClass, val
    fields: MutableMap<String, Any?> = mutableMapOf()
) {
    override fun toString() = "${klazz.name} instance"

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }

        val method = klazz.findMethod(name.lexeme)
        if (method != null) {
            return method.bind(this)
        }

        throw RuntimeError(name, "Undefined property '${name.lexeme}'.")
    }

    fun set(name: Token, value: Any?) {
        fields.put(name.lexeme, value)
    }
}

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean
) : LoxCallable {

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)

        (declaration.params zip arguments).forEach { (param, argument) ->
            environment.define(param.lexeme, argument)
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return if (isInitializer) {
                closure.getAt(0, "this")
            } else {
                returnValue.value
            }
        }
        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    override fun arity(): Int = declaration.params.size

    override fun toString(): String = "<fn ${declaration.name.lexeme}>"
}

class Return(val value: Any?) : RuntimeException(null, null, false, false)

class Interpreter(
    val globals: Environment = Environment(),
    private var environment: Environment = globals,
    private val locals: MutableMap<Expr, Int> = mutableMapOf()
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

    fun resolve(expr: Expr, depth: Int): Unit {
        locals[expr] = depth
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

    override fun visitVariable(expr: Expr.Variable): Any? = lookUpVariable(expr.name, expr)

    private fun lookUpVariable(name: Token, expr: Expr): Any? =
        locals[expr]?.let { distance ->
            environment.getAt(distance, name.lexeme)
        } ?: globals.get(name)

    override fun visitIf(it: Stmt.If) {
        if (isTruthy(evaluate(it.condition)))
            execute(it.thenBranch)
        else
            it.elseBranch?.let { execute(it) }
    }

    override fun visitBlock(it: Stmt.Block) =
        executeBlock(it.statements, Environment(environment))

    override fun visitClass(stmt: Stmt.Class) {
        var superclass: Any? = null
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass)
            if (!(superclass is LoxClass)) {
                throw RuntimeError(
                    stmt.superclass.name,
                    "Superclass must be a class."
                )
            }
        }

        environment.define(stmt.name.lexeme, null)

        if (stmt.superclass != null) {
            environment = Environment(environment)
            environment.define("super", superclass)
        }

        val methods: MutableMap<String, LoxFunction> = mutableMapOf()

        stmt.methods.forEach { method ->
            val function = LoxFunction(method, environment, method.name.lexeme.equals("init"))
            methods[method.name.lexeme] = function
        }

        val klass = LoxClass(stmt.name.lexeme, superclass as LoxClass?, methods)

        if (superclass != null) {
            environment = environment.enclosing!!
        }

        environment.assign(stmt.name, klass)
    }

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
        environment.define(stmt.name.lexeme, LoxFunction(stmt, environment, false))
    }

    override fun visitPrint(stmt: Stmt.Print) {
        println(stringify(evaluate(stmt.expression)))
    }

    override fun visitReturn(stmt: Stmt.Return) =
        throw Return(stmt.value?.let { evaluate(it) })

    override fun visitAssign(expr: Expr.Assign): Any? =
        evaluate(expr.value).also { value ->
            locals.get(expr)?.let { distance ->
                environment.assignAt(distance, expr.name, value)
            } ?: globals.assign(expr.name, value)
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

    override fun visitGet(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)

        when {
            (obj is LoxInstance) -> return obj.get(expr.name)
            else -> throw RuntimeError(expr.name, "Only instances have properties.")
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

    override fun visitSet(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)

        if (obj is LoxInstance) {
            val value = evaluate(expr.value)
            obj.set(expr.name, value)
            return value
        } else {
            throw RuntimeError(expr.name, "Only instances have fields.")
        }
    }

    override fun visitSuper(expr: Expr.Super): Any? {
        val distance = locals.get(expr)
        val superclass = environment.getAt(distance!!, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme)
        if (method == null) {
            throw RuntimeError(
                expr.method,
                "Undefined property '${expr.method.lexeme}'."
            )
        }
        return method.bind(obj)
    }

    override fun visitThis(expr: Expr.This): Any? =
        lookUpVariable(expr.keyword, expr)

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