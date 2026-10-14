package dev.whistlekeyboard

import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import org.json.JSONObject

/**
 * HTTP client for communicating with the local Whistle transcription server.
 * Sends WAV files to http://127.0.0.1:8765/transcribe and parses JSON responses.
 */
class TranscriptionClient(
    private val baseUrl: String = "http://127.0.0.1:8765",
    private val connectionTimeoutMs: Int = 5000,
    private val readTimeoutMs: Int = 30000
) {

    private const val HEALTH_PATH = "/health"
    private const val TRANSCRIBE_PATH = "/transcribe"
    private const val TAG = "TranscriptionClient"

    interface TranscriptionCallback {
        fun onSuccess(text: String)
        fun onError(error: String)
    }

    interface HealthCheckCallback {
        fun onHealthy()
        fun onUnhealthy(error: String)
    }

    /**
     * Checks if the transcription server is reachable and healthy.
     */
    fun checkHealth(callback: HealthCheckCallback) {
        Thread({
            var connection: HttpURLConnection? = null
            try {
                val url = URL(baseUrl + HEALTH_PATH)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = connectionTimeoutMs
                connection.readTimeout = connectionTimeoutMs
                connection.doInput = true

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    if (json.optString("status") == "ok") {
                        callback.onHealthy()
                    } else {
                        callback.onUnhealthy("Server returned unexpected status: $response")
                    }
                } else {
                    callback.onUnhealthy("HTTP $responseCode")
                }
            } catch (e: Exception) {
                callback.onUnhealthy(e.message ?: "Connection failed")
            } finally {
                connection?.disconnect()
            }
        }).start()
    }

    /**
     * Sends a WAV file for transcription.
     */
    fun transcribe(wavFile: File, callback: TranscriptionCallback) {
        Thread({
            var connection: HttpURLConnection? = null
            try {
                val url = URL(baseUrl + TRANSCRIBE_PATH)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = connectionTimeoutMs
                connection.readTimeout = readTimeoutMs
                connection.doOutput = true
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "application/octet-stream")
                connection.setRequestProperty("Content-Length", wavFile.length().toString())
                connection.useCaches = false

                // Stream the file directly to avoid loading large files into memory
                val fileSize = wavFile.length()
                val maxSize = 25 * 1024 * 1024 // 25 MB limit
                if (fileSize > maxSize) {
                    callback.onError("Audio file too large (${fileSize} bytes, max $maxSize)")
                    return@Thread
                }

                val inputStream = java.io.FileInputStream(wavFile)
                val outputStream = connection.outputStream
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                inputStream.close()
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseTranscriptionResponse(response, callback)
                } else {
                    val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                    callback.onError("HTTP $responseCode: $errorBody")
                }
            } catch (e: IOException) {
                callback.onError("Network error: ${e.message}")
            } catch (e: Exception) {
                callback.onError("Unexpected error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }).start()
    }

    private fun parseTranscriptionResponse(jsonText: String, callback: TranscriptionCallback) {
        try {
            val json = JSONObject(jsonText)
            val text = json.optString("text", "").trim()

            if (text.isEmpty()) {
                callback.onError("Empty transcription result")
            } else {
                Log.d(TAG, "Transcription received: $text")
                callback.onSuccess(text)
            }
        } catch (e: Exception) {
            callback.onError("Failed to parse JSON response: ${e.message}")
        }
    }
}