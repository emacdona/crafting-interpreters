package net.edmacdonald.craftinginterpreters

import net.edmacdonald.craftinginterpreters.TokenType.*
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
                    listOf(CLASS, IDENTIFIER, LESS, NUMBER, PLUS, STRING,
                    IDENTIFIER, EQUAL, NUMBER, EOF)
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