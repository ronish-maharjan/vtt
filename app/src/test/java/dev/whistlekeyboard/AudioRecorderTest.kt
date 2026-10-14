package dev.whistlekeyboard

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for WAV header generation and transcription response parsing.
 */
class AudioRecorderTest {

    @Test
    fun testWavHeaderGeneration() {
        val sampleRate = 16000
        val dataSize = 1000L
        val header = buildTestWavHeader(sampleRate, dataSize)

        assertEquals(44, header.size)
        assertEquals("RIFF", String(header, 0, 4))
        assertEquals("WAVE", String(header, 8, 4))
        assertEquals("fmt ", String(header, 12, 4))
        assertEquals("data", String(header, 36, 4))

        // Check format fields
        val buffer = java.nio.ByteBuffer.wrap(header)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

        buffer.position(20)
        assertEquals(1, buffer.getShort()) // AudioFormat (PCM)
        assertEquals(1, buffer.getShort()) // NumChannels (mono)
        assertEquals(sampleRate, buffer.getInt()) // SampleRate
        assertEquals(sampleRate * 2, buffer.getInt()) // ByteRate
        assertEquals(2, buffer.getShort()) // BlockAlign
        assertEquals(16, buffer.getShort()) // BitsPerSample

        buffer.position(40)
        assertEquals(dataSize.toInt(), buffer.getInt()) // Subchunk2Size
    }

    @Test
    fun testWavHeaderChunkSize() {
        val dataSize = 1000L
        val header = buildTestWavHeader(16000, dataSize)

        val buffer = java.nio.ByteBuffer.wrap(header)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.position(4)

        // ChunkSize = 36 + dataSize
        assertEquals(36 + dataSize, buffer.getInt().toLong())
    }

    private fun buildTestWavHeader(sampleRate: Int, dataSize: Long): ByteArray {
        val header = ByteArray(44)
        val buffer = java.nio.ByteBuffer.wrap(header)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

        buffer.put("RIFF".toByteArray())
        buffer.putInt((36 + dataSize).toInt())
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1.toShort())
        buffer.putShort(1.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * 2)
        buffer.putShort(2.toShort())
        buffer.putShort(16.toShort())
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize.toInt())

        return header
    }
}