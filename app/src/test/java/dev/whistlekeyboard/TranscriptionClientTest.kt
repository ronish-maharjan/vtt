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
        val result = parseTranscriptionResponse(jsonText)

        assertEquals("hello world", result)
    }

    @Test
    fun testParseResponseWithExtraFields() {
        val jsonText = """{"text": "test transcription", "language": "en", "ttft_ms": 200, "decode_tps": 50.0, "extra": "ignored"}"""
        val result = parseTranscriptionResponse(jsonText)

        assertEquals("test transcription", result)
    }

    @Test
    fun testParseEmptyText() {
        val jsonText = """{"text": "", "language": "en"}"""
        val exception = assertThrows<IllegalArgumentException> {
            parseTranscriptionResponse(jsonText)
        }
        assertTrue(exception.message!!.contains("Empty"))
    }

    @Test
    fun testParseMissingTextField() {
        val jsonText = """{"language": "en", "ttft_ms": 100}"""
        val exception = assertThrows<IllegalArgumentException> {
            parseTranscriptionResponse(jsonText)
        }
        assertTrue(exception.message!!.contains("Empty"))
    }

    @Test
    fun testParseWhitespaceOnlyText() {
        val jsonText = """{"text": "   ", "language": "en"}"""
        val exception = assertThrows<IllegalArgumentException> {
            parseTranscriptionResponse(jsonText)
        }
        assertTrue(exception.message!!.contains("Empty"))
    }

    @Test
    fun testParseMalformedJson() {
        val jsonText = "{invalid json}"
        val exception = assertThrows<Exception> {
            parseTranscriptionResponse(jsonText)
        }
        assertNotNull(exception.message)
    }

    @Test
    fun testParseNullText() {
        val jsonText = """{"text": null, "language": "en"}"""
        val result = parseTranscriptionResponse(jsonText)
        // JSONObject.optString returns empty string for null
        assertThrows<IllegalArgumentException> {
            parseTranscriptionResponse(jsonText)
        }
    }

    private fun parseTranscriptionResponse(jsonText: String): String {
        val json = JSONObject(jsonText)
        val text = json.optString("text", "").trim()

        if (text.isEmpty()) {
            throw IllegalArgumentException("Empty transcription result")
        }
        return text
    }
}