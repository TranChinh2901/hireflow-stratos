package com.hireflow.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ScoreCalculatorTest {
    @Test
    fun `average returns score across four criteria`() {
        assertEquals(3.75, ScoreCalculator.average(4, 3, 4, 4), 0.001)
    }

    @Test
    fun `average rejects score outside one to five`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScoreCalculator.average(0, 3, 4, 4)
        }
    }
}
