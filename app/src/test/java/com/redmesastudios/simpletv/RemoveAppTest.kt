package com.redmesastudios.simpletv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoveAppTest {
    @Test
    fun aNormalPressIsNotAHold() {
        assertFalse(isHeldLongEnough(downTime = 10_000, eventTime = 10_450))
        assertFalse(isHeldLongEnough(downTime = 10_000, eventTime = 11_499))
    }

    @Test
    fun holdingOkForASecondAndAHalfIsAHold() {
        assertTrue(isHeldLongEnough(downTime = 10_000, eventTime = 11_500))
        assertTrue(isHeldLongEnough(downTime = 10_000, eventTime = 14_000))
    }
}
