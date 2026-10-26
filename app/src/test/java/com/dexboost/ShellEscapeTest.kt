package com.dexboost

import com.dexboost.optimization.PackageNameValidator
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellEscapeTest {

    @Test
    fun plainStringIsWrappedInSingleQuotes() {
        assertEquals("'com.example.app'", PackageNameValidator.escapeShellArg("com.example.app"))
    }

    @Test
    fun singleQuoteIsEscaped() {
        assertEquals(
            "'it'\\''s'",
            PackageNameValidator.escapeShellArg("it's"),
        )
    }

    @Test
    fun spacesArePreservedInsideQuotes() {
        assertEquals("'a b c'", PackageNameValidator.escapeShellArg("a b c"))
    }

    @Test
    fun dollarSemicolonAndBacktickStayLiteralInsideQuotes() {
        assertEquals(
            "'\$(id); `id`'",
            PackageNameValidator.escapeShellArg("\$(id); `id`"),
        )
    }

    @Test
    fun multipleQuotesAreEachEscaped() {
        assertEquals(
            "'a'\\''b'\\''c'",
            PackageNameValidator.escapeShellArg("a'b'c"),
        )
    }

    @Test
    fun emptyStringBecomesEmptyQuotedString() {
        assertEquals("''", PackageNameValidator.escapeShellArg(""))
    }
}
