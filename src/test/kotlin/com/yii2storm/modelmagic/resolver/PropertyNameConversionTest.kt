package com.yii2storm.modelmagic.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PropertyNameConversionTest {

    private val resolver = MagicPropertyResolver()

    @Test
    fun `snake case property converts to camel case getter`() {
        assertEquals("getUserId", resolver.propertyNameToGetter("user_id"))
    }

    @Test
    fun `snake case property converts to camel case setter`() {
        assertEquals("setUserId", resolver.propertyNameToSetter("user_id"))
    }

    @Test
    fun `camel case property keeps its word boundaries`() {
        assertEquals("getFullName", resolver.propertyNameToGetter("fullName"))
        assertEquals("setFullName", resolver.propertyNameToSetter("fullName"))
    }

    @Test
    fun `candidate lists retain snake and camel conventions`() {
        val getters = resolver.propertyNameToGetterCandidates("user_id")
        val setters = resolver.propertyNameToSetterCandidates("user_id")

        assertTrue(getters.contains("getUserId"))
        assertTrue(setters.contains("setUserId"))
    }
}
