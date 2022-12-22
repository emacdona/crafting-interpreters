package net.edmacdonald.craftinginterpreters

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

class Lox {
    companion object{
        var hadError: Boolean = false
    }
}

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
    if(Lox.hadError) exitProcess(65)
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

fun run(source: String){
    val scanner = Scanner(source)
    val tokens = scanner.scanTokens()
    tokens.forEach{println(it)}
}

fun error(line: Int, message: String){
    report(line, "", message)
}

fun report(line: Int, where: String, message: String){
    println("[line ${line}] Error ${where}: ${message}")
    Lox.hadError = true
}
