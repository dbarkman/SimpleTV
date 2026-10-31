package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {
    @Test
    fun morningRunsFromFiveToEleven() {
        assertEquals("Good morning", greetingFor(5))
        assertEquals("Good morning", greetingFor(11))
    }

    @Test
    fun afternoonRunsFromNoonToFour() {
        assertEquals("Good afternoon", greetingFor(12))
        assertEquals("Good afternoon", greetingFor(16))
    }

    @Test
    fun eveningCoversLateNightAndEarlyMorning() {
        assertEquals("Good evening", greetingFor(17))
        assertEquals("Good evening", greetingFor(23))
        assertEquals("Good evening", greetingFor(0))
        assertEquals("Good evening", greetingFor(4))
    }
}
