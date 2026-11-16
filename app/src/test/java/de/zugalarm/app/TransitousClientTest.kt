
package de.zugalarm.app

import org.junit.Assert.assertEquals
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

    @Test
    fun multipleAdditionalStopsAreRecognized() {
        val scheduledStopIds = setOf("A", "B", "C")
        val actualStopIds = listOf("A", "X", "B", "Y", "C")

        val results = actualStopIds.map {
            isAdditionalStop(it, scheduledStopIds)
        }

        assertEquals(
            listOf(false, true, false, true, false),
            results
        )
    }

    @Test
    fun differentStopOrderDoesNotChangeClassification() {
        val scheduledStopIds = setOf("A", "B", "C")
        val actualStopIds = listOf("C", "X", "A", "B")

        val results = actualStopIds.map {
            isAdditionalStop(it, scheduledStopIds)
        }

        assertEquals(
            listOf(false, true, false, false),
            results
        )
    }

    @Test
    fun duplicateStopIdsAreClassifiedConsistently() {
        val scheduledStopIds = setOf("A", "B", "C")

        assertFalse(
            isAdditionalStop("B", scheduledStopIds)
        )
        assertFalse(
            isAdditionalStop("B", scheduledStopIds)
        )
        assertTrue(
            isAdditionalStop("X", scheduledStopIds)
        )
        assertTrue(
            isAdditionalStop("X", scheduledStopIds)
        )
    }

    @Test
    fun whitespaceOnlyStopIdIsNotAdditional() {
        val scheduledStopIds = setOf("A", "B", "C")

        assertFalse(
            isAdditionalStop("   ", scheduledStopIds)
        )
    }
}
