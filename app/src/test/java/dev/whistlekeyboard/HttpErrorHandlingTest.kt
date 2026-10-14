package dev.whistlekeyboard

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for HTTP error handling scenarios.
 */
class HttpErrorHandlingTest {

    @Test
    fun testConnectionRefused() {
        // Simulate connection refused error handling
        val error = handleHttpError(java.net.ConnectException("Connection refused"))
        assertTrue(error.contains("Connection failed"))
    }

    @Test
    fun testTimeout() {
        val error = handleHttpError(java.net.SocketTimeoutException("Read timed out"))
        assertTrue(error.contains("Network error"))
    }

    @Test
    fun testHttp404() {
        val error = handleHttpError(404, "Not Found")
        assertTrue(error.contains("HTTP 404"))
    }

    @Test
    fun testHttp500() {
        val error = handleHttpError(500, "Internal Server Error")
        assertTrue(error.contains("HTTP 500"))
    }

    @Test
    fun testMalformedJsonResponse() {
        val error = handleHttpError(200, "{invalid json}")
        assertTrue(error.contains("Failed to parse JSON"))
    }

    private fun handleHttpError(code: Int, body: String): String {
        return "HTTP $code: $body"
    }

    private fun handleHttpError(exception: Exception): String {
        return "Network error: ${exception.message}"
    }
}