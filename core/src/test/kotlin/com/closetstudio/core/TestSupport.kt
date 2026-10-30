package com.closetstudio.core

import kotlin.math.abs
import kotlin.test.assertTrue

/** Millimetre comparison with a tolerance that hides float accumulation noise. */
fun assertMm(expected: Float, actual: Float, tolerance: Float = 0.01f) {
    assertTrue(
        abs(expected - actual) <= tolerance,
        "expected $expected mm +/- $tolerance but was $actual mm",
    )
}
