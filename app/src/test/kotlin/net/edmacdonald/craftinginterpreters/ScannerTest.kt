package net.edmacdonald.craftinginterpreters

import net.edmacdonald.craftinginterpreters.scanner.TokenType.*
import net.edmacdonald.craftinginterpreters.scanner.Scanner
import net.edmacdonald.craftinginterpreters.scanner.TokenType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ScannerTest {
    companion object {
        @JvmStatic
        fun data(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    "1 + 2",
                    listOf(NUMBER, PLUS, NUMBER, EOF)
                ),
                Arguments.of(
                    "class foo < 1 + \"hello world\"\nbar = 10",
                    listOf(
                        CLASS, IDENTIFIER, LESS, NUMBER, PLUS, STRING,
                        IDENTIFIER, EQUAL, NUMBER, EOF
                    )
                ),
                Arguments.of(
                    "(){},.-+;/*! != = == > >= < <= id \"str\" 123.456 and class else false fun for if nil or print return super this true var while",
                    listOf(
                        LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
                        COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,
                        BANG, BANG_EQUAL, EQUAL, EQUAL_EQUAL,
                        GREATER, GREATER_EQUAL, LESS, LESS_EQUAL,
                        IDENTIFIER, STRING, NUMBER,
                        AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
                        PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
                        EOF
                    )
                )
            )
    }

    @ParameterizedTest
    @MethodSource("data")
    fun testStuff(source: String, tokenTypes: List<TokenType>) {
        Assertions.assertEquals(
            tokenTypes,
            Scanner(source)
                .scanTokens()
                .map { it.type }
        )
    }
}