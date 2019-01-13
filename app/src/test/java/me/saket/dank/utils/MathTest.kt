package me.saket.dank.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class MathTest {

  @Test
  fun `clamp() should work correctly`() {
    val maxValue = 100F

    val testToExpectedValues = mapOf(
        -120F to -100F,
        -100F to -100F,
        -50F to -50F,
        50F to 50F,
        100F to 100F,
        120F to 100F)

    testToExpectedValues.forEach { (testValue, expectedValue) ->
      assertEquals(expectedValue, testValue.clamp(min = -maxValue, max = maxValue))
    }
  }
}
