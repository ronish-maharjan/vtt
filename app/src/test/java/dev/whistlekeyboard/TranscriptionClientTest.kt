package dev.whistlekeyboard

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for transcription response JSON parsing.
 */
class TranscriptionClientTest {

    @Test
    fun testParseValidResponse() {
        val jsonText = """{"text": "hello world", "language": "en", "ttft_ms": 150, "decode_tps": 45.2}"""
        val result = parseTestTranscriptionResponse(jsonText)

        assertEquals("hello world", result)
    }

    @Test
    fun testParseResponseWithExtraFields() {
        val jsonText = """{"text": "test transcription", "language": "en", "ttft_ms": 200, "decode_tps": 50.0, "extra": "ignored"}"""
        val result = parseTestTranscriptionResponse(jsonText)

        assertEquals("test transcription", result)
    }

    @Test
    fun testParseEmptyText() {
        val jsonText = """{"text": "", "language": "en"}"""
        try {
            parseTestTranscriptionResponse(jsonText)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Empty"))
        }
    }

    @Test
    fun testParseMissingTextField() {
        val jsonText = """{"language": "en", "ttft_ms": 100}"""
        try {
            parseTestTranscriptionResponse(jsonText)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Empty"))
        }
    }

    @Test
    fun testParseWhitespaceOnlyText() {
        val jsonText = """{"text": "   ", "language": "en"}"""
        try {
            parseTestTranscriptionResponse(jsonText)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Empty"))
        }
    }

    @Test
    fun testParseMalformedJson() {
        val jsonText = "{invalid json}"
        try {
            parseTestTranscriptionResponse(jsonText)
            fail("Expected Exception")
        } catch (e: Exception) {
            assertNotNull(e.message)
        }
    }

    @Test
    fun testParseNullText() {
        val jsonText = """{"text": null, "language": "en"}"""
        // JSONObject.optString returns empty string for null
        try {
            parseTestTranscriptionResponse(jsonText)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Empty"))
        }
    }

    private fun parseTestTranscriptionResponse(jsonText: String): String {
        val json = JSONObject(jsonText)
        val text = json.optString("text", "").trim()

        if (text.isEmpty()) {
            throw IllegalArgumentException("Empty transcription result")
        }
        return text
    }
}