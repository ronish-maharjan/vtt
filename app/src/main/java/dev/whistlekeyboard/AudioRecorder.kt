package dev.whistlekeyboard

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Handles audio recording using Android's AudioRecord API.
 * Records PCM 16-bit mono audio at 16kHz and writes a valid WAV file.
 */
class AudioRecorder(
    private val context: Context,
    private val sampleRate: Int = 16000
) {

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var outputFile: File? = null
    private var fileOutputStream: FileOutputStream? = null
    private var isRecording = false
    private var bufferSize = 0

    // Recording state callbacks
    var onRecordingStarted: (() -> Unit)? = null
    var onRecordingStopped: ((File?) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    /**
     * Starts recording audio to a temporary WAV file.
     * Returns true if recording started successfully.
     */
    fun startRecording(): Boolean {
        if (isRecording) {
            onError?.invoke("Already recording")
            return false
        }

        // Calculate buffer size
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            onError?.invoke("Unsupported audio format")
            return false
        }

        // Create AudioRecord instance
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize * 2 // Double buffer for safety
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            onError?.invoke("Failed to initialize AudioRecord")
            releaseResources()
            return false
        }

        // Create temporary WAV file
        outputFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        try {
            fileOutputStream = FileOutputStream(outputFile!!)
            // Write placeholder WAV header (will be updated on stop)
            writeWavHeader(fileOutputStream!!, 0)
        } catch (e: IOException) {
            onError?.invoke("Failed to create audio file: ${e.message}")
            releaseResources()
            return false
        }

        isRecording = true
        audioRecord?.startRecording()

        // Start recording thread
        recordingThread = Thread(this::recordAudio, "AudioRecorder")
        recordingThread?.start()

        onRecordingStarted?.invoke()
        return true
    }

    /**
     * Stops recording and finalizes the WAV file.
     */
    fun stopRecording() {
        if (!isRecording) return

        isRecording = false

        // Wait for recording thread to finish
        recordingThread?.join(2000)
        recordingThread = null

        // Finalize WAV file with correct header
        finalizeWavFile()

        val file = outputFile
        releaseResources()
        onRecordingStopped?.invoke(file)
    }

    /**
     * Cancels recording and deletes the temporary file.
     */
    fun cancelRecording() {
        if (!isRecording) return

        isRecording = false
        recordingThread?.join(1000)
        recordingThread = null

        // Delete temporary file
        outputFile?.delete()
        releaseResources()
        onRecordingStopped?.invoke(null)
    }

    private fun recordAudio() {
        val buffer = ShortArray(bufferSize)
        var totalSamples = 0L

        while (isRecording && audioRecord != null) {
            val readResult = audioRecord?.read(buffer, 0, bufferSize)
            if (readResult != null && readResult > 0) {
                try {
                    // Write raw PCM data
                    val byteBuffer = java.nio.ByteBuffer.allocate(readResult * 2)
                    byteBuffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    buffer.forEachIndexed { index, sample ->
                        if (index < readResult) {
                            byteBuffer.putShort(sample)
                        }
                    }
                    fileOutputStream?.write(byteBuffer.array(), 0, readResult * 2)
                    totalSamples += readResult
                } catch (e: IOException) {
                    onError?.invoke("Failed to write audio data: ${e.message}")
                    break
                }
            } else if (readResult == AudioRecord.ERROR_INVALID_OPERATION) {
                // Recording stopped
                break
            }
        }

        // Store total samples for header update
        this.totalSamples = totalSamples
    }

    private var totalSamples: Long = 0

    private fun finalizeWavFile() {
        val file = outputFile ?: return
        val fos = fileOutputStream ?: return

        try {
            fos.flush()
            fos.close()

            // Rewrite the file with correct WAV header
            val pcmData = file.readBytes()
            val pcmSize = pcmData.size - 44 // Subtract header size

            if (pcmSize > 0) {
                val header = buildWavHeader(pcmSize.toLong())
                val outputStream = FileOutputStream(file)
                outputStream.write(header)
                outputStream.write(pcmData, 44, pcmSize.toInt())
                outputStream.close()
            }
        } catch (e: IOException) {
            onError?.invoke("Failed to finalize WAV file: ${e.message}")
        }
    }

    private fun writeWavHeader(outputStream: FileOutputStream, dataSize: Long) {
        val header = buildWavHeader(dataSize)
        outputStream.write(header)
    }

    private fun buildWavHeader(dataSize: Long): ByteArray {
        val header = ByteArray(44)
        val buffer = java.nio.ByteBuffer.wrap(header)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)

        // RIFF header
        buffer.put("RIFF".toByteArray())                    // ChunkID
        buffer.putInt((36 + dataSize).toInt())              // ChunkSize
        buffer.put("WAVE".toByteArray())                    // Format

        // fmt subchunk
        buffer.put("fmt ".toByteArray())                    // Subchunk1ID
        buffer.putInt(16)                                   // Subchunk1Size (PCM)
        buffer.putShort(1.toShort())                        // AudioFormat (PCM = 1)
        buffer.putShort(1.toShort())                        // NumChannels (mono = 1)
        buffer.putInt(sampleRate)                           // SampleRate
        buffer.putInt(sampleRate * 2)                       // ByteRate (SampleRate * NumChannels * BitsPerSample/8)
        buffer.putShort(2.toShort())                        // BlockAlign (NumChannels * BitsPerSample/8)
        buffer.putShort(16.toShort())                       // BitsPerSample

        // data subchunk
        buffer.put("data".toByteArray())                    // Subchunk2ID
        buffer.putInt(dataSize.toInt())                     // Subchunk2Size

        return header
    }

    private fun releaseResources() {
        try {
            audioRecord?.stop()
        } catch (e: IllegalStateException) {
            // Ignore if already stopped
        }
        audioRecord?.release()
        audioRecord = null

        try {
            fileOutputStream?.close()
        } catch (e: IOException) {
            // Ignore
        }
        fileOutputStream = null
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}