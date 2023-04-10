package net.edmacdonald.craftinginterpreters

import net.edmacdonald.craftinginterpreters.parser.Parser
import net.edmacdonald.craftinginterpreters.scanner.Scanner
import net.edmacdonald.craftinginterpreters.scanner.Token
import net.edmacdonald.craftinginterpreters.scanner.TokenType
import net.edmacdonald.craftinginterpreters.visitor.AstPrinter
import net.edmacdonald.craftinginterpreters.visitor.Interpreter
import net.edmacdonald.craftinginterpreters.visitor.RuntimeError
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object {
        var hadError: Boolean = false
        var hadRuntimeError: Boolean = false

        fun error(token: Token, message: String) {
            if (token.type == TokenType.EOF) {
                report(token.line, " at end", message)
            } else {
                report(token.line, "at '${token.lexeme}'", message)
            }
        }
    }
}

/*
fun main(args: Array<String>) {
    val expression: Expr = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1), Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1), Expr.Grouping(
            Expr.Literal(45.67)
        )
    )
    println(AstPrinter().print(expression))
}
*/

fun main(args: Array<String>) {
    if (args.size > 1) {
        println("Usage: jlox [script]")
        exitProcess(64)
    } else if (args.size == 1) {
        runFile(args[0])
    } else {
        runPrompt()
    }
}

fun runFile(path: String) {
    val bytes = Files.readAllBytes(Paths.get(path))
    run(String(bytes, Charset.defaultCharset()))
    if (Lox.hadError) exitProcess(65)
    if (Lox.hadRuntimeError) exitProcess(70)
}

fun runPrompt() {
    val input = InputStreamReader(System.`in`)
    val reader = BufferedReader(input)

    while (true) {
        print("> ")
        val line = reader.readLine() ?: break
        run(line)
        Lox.hadError = false
    }
}

fun run(source: String) {
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()

    println("Tokens:")
    tokens.forEach { println("\t${it}") }
    println()

    val parser = Parser(tokens)
    val statements = parser.parse()

    if(Lox.hadError) return

    println("Parsed:")
    println("\t${AstPrinter().print(statements!!)}")
    println("Evaluated:")
    println("\t${Interpreter().interpret(statements!!)}")
}

fun error(line: Int, message: String) {
    report(line, "", message)
}
fun runtimeError(error: RuntimeError) {
    System.err.println(
        """
        ${error.message}
        [line ${error.token.line}]
        """.trimIndent()
    )
    Lox.hadRuntimeError = true
}

fun report(line: Int, where: String, message: String) {
    println("[line ${line}] Error ${where}: ${message}")
    Lox.hadError = true
}