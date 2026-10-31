package com.redmesastudios.simpletv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotosTest {
    @Test
    fun photosShrinkToFitTheTvButNeverGrow() {
        assertEquals(1440 to 1080, fitWithin(4032, 3024, 1920, 1080))
        assertEquals(608 to 1080, fitWithin(3024, 5376, 1920, 1080))
        assertEquals(1920 to 1080, fitWithin(3840, 2160, 1920, 1080))
        assertEquals(800 to 600, fitWithin(800, 600, 1920, 1080))
    }

    @Test
    fun onlyStoreMadeNamesAreTouched() {
        assertTrue(isPhotoName("0f8fad5b-d9cb-469f-a165-70867728950e.jpg"))
        assertFalse(isPhotoName("../shared_prefs/youtube.xml"))
        assertFalse(isPhotoName("photo.jpg"))
    }

    @Test
    fun pinsAreFourDigits() {
        val random = Random(7)
        repeat(200) { assertTrue(newUploadPin(random).matches(Regex("\\d{4}"))) }
    }

    @Test
    fun requestsParseTheirPathQueryAndHeaders() {
        val request = parseRequest("PUT /api/upload?pin=0042&x=a%20b HTTP/1.1", listOf("Content-Length: 12", "X-Pin: 0042"))!!
        assertEquals("PUT", request.method)
        assertEquals("/api/upload", request.path)
        assertEquals("0042", request.query["pin"])
        assertEquals("a b", request.query["x"])
        assertEquals("12", request.headers["content-length"])
        assertNull(parseRequest("garbage", emptyList()))
    }

    @Test
    fun portraitAndSquarePhotosShowWhole() {
        assertTrue(showsWhole(608, 1080))
        assertTrue(showsWhole(1080, 1080))
        assertFalse(showsWhole(1440, 1080))
        assertFalse(showsWhole(1920, 1080))
    }

    @Test
    fun timingStepsThroughItsChoicesAndStopsAtTheEnds() {
        assertEquals(20, SlideshowTiming.nextChoice(12, longer = true))
        assertEquals(8, SlideshowTiming.nextChoice(12, longer = false))
        assertEquals(120, SlideshowTiming.nextChoice(120, longer = true))
        assertEquals(5, SlideshowTiming.nextChoice(5, longer = false))
        assertEquals("20 seconds", SlideshowTiming.describe(20))
        assertEquals("1 minute", SlideshowTiming.describe(60))
        assertEquals("2 minutes", SlideshowTiming.describe(120))
    }
}
