package de.zugalarm.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitousClientTest {

    @Test
    fun scheduledStopIsNotAdditional() {
        val scheduledStopIds = setOf("A", "B", "C")

        assertFalse(
            isAdditionalStop("B", scheduledStopIds)
        )
    }

    @Test
    fun unknownStopIsAdditional() {
        val scheduledStopIds = setOf("A", "B", "C")

        assertTrue(
            isAdditionalStop("X", scheduledStopIds)
        )
    }

    @Test
    fun missingStopIdIsNotAdditional() {
        val scheduledStopIds = setOf("A", "B", "C")

        assertFalse(
            isAdditionalStop("", scheduledStopIds)
        )
    }
}
