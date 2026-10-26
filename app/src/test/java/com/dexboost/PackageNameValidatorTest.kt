package com.dexboost

import com.dexboost.optimization.PackageNameValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageNameValidatorTest {

    @Test
    fun validPackageNamesAreAccepted() {
        assertTrue(PackageNameValidator.isValid("com.example.app"))
        assertTrue(PackageNameValidator.isValid("com.dexboost"))
        assertTrue(PackageNameValidator.isValid("a.b"))
        assertTrue(PackageNameValidator.isValid("com.example.app2"))
        assertTrue(PackageNameValidator.isValid("com.example.my_app"))
        assertTrue(PackageNameValidator.isValid("com.example.a1_b2"))
    }

    @Test
    fun shellMetacharactersAreRejected() {
        assertFalse(PackageNameValidator.isValid("com.example.app; rm -rf /"))
        assertFalse(PackageNameValidator.isValid("com.example.app\$(id)"))
        assertFalse(PackageNameValidator.isValid("com.example.app`id`"))
        assertFalse(PackageNameValidator.isValid("com.example.app|cat"))
        assertFalse(PackageNameValidator.isValid("com.example.app&cat"))
        assertFalse(PackageNameValidator.isValid("com.example.app'"))
        assertFalse(PackageNameValidator.isValid("com.example.app\""))
        assertFalse(PackageNameValidator.isValid("com.example.app app"))
        assertFalse(PackageNameValidator.isValid("com.example.app\n"))
    }

    @Test
    fun emptyAndMalformedNamesAreRejected() {
        assertFalse(PackageNameValidator.isValid(""))
        assertFalse(PackageNameValidator.isValid(".com.example"))
        assertFalse(PackageNameValidator.isValid("com.example."))
        assertFalse(PackageNameValidator.isValid("com..example"))
        assertFalse(PackageNameValidator.isValid("com"))
        assertFalse(PackageNameValidator.isValid("com.example-app"))
        assertFalse(PackageNameValidator.isValid("com.example.1app"))
    }

    @Test
    fun uppercaseIsRejected() {
        assertFalse(PackageNameValidator.isValid("com.Example.app"))
        assertFalse(PackageNameValidator.isValid("Com.example.app"))
        assertFalse(PackageNameValidator.isValid("COM.EXAMPLE.APP"))
    }

    @Test
    fun leadingDotIsRejected() {
        assertFalse(PackageNameValidator.isValid(".com.example.app"))
    }

    @Test
    fun tooLongNamesAreRejected() {
        val longSegment = "a".repeat(64)
        assertFalse(PackageNameValidator.isValid("com.example.$longSegment"))
        val longName = "com." + "a".repeat(252)
        assertFalse(PackageNameValidator.isValid(longName))
    }

    @Test
    fun segmentAtMaxLengthIsAccepted() {
        val segment = "a".repeat(63)
        assertTrue(PackageNameValidator.isValid("com.example.$segment"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun requireValidThrowsForInvalidName() {
        PackageNameValidator.requireValid("com.example.app; evil")
    }

    @Test
    fun requireValidPassesForValidName() {
        // Must not throw.
        PackageNameValidator.requireValid("com.example.app")
    }
}
