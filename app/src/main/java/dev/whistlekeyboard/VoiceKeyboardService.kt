package dev.whistlekeyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Main Input Method Service for the Whistle Voice Keyboard.
 * Handles audio recording, transcription, and text insertion.
 */
class VoiceKeyboardService : InputMethodService() {

    private var keyboardView: KeyboardView? = null
    private var audioRecorder: AudioRecorder? = null
    private var transcriptionClient: TranscriptionClient? = null
    private var pendingInputConnection: InputConnection? = null

    private val handler = Handler(Looper.getMainLooper())
    private val TAG = "VoiceKeyboardService"

    override fun onCreate() {
        super.onCreate()
        transcriptionClient = TranscriptionClient()
        audioRecorder = AudioRecorder(this)
        setupAudioRecorderCallbacks()
    }

    override fun onCreateInputView(): View {
        keyboardView = KeyboardView(this, null, 0).apply {
            setOnMicTapListener { onMicTapped() }
            setOnSwitchKeyboardListener { onSwitchKeyboard() }
            setOnBackspaceListener { onBackspace() }
            setOnEnterListener { onEnter() }
            setOnSpaceListener { onSpace() }
        }
        return keyboardView!!
    }

    override fun onStartInput(editorInfo: android.view.inputmethod.EditorInfo, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        // Store reference to current input connection
        pendingInputConnection = currentInputConnection
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // Clean up if recording when input finishes
        if (audioRecorder?.isCurrentlyRecording() == true) {
            audioRecorder?.cancelRecording()
        }
        pendingInputConnection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.cancelRecording()
        handler.removeCallbacksAndMessages(null)
    }

    private fun setupAudioRecorderCallbacks() {
        audioRecorder?.onRecordingStarted = {
            handler.post {
                keyboardView?.setState(KeyboardView.State.RECORDING)
            }
        }

        audioRecorder?.onRecordingStopped = { file ->
            handler.post {
                if (file != null) {
                    keyboardView?.setState(KeyboardView.State.PROCESSING)
                    transcribeAudio(file)
                } else {
                    keyboardView?.setState(KeyboardView.State.IDLE)
                }
            }
        }

        audioRecorder?.onError = { error ->
            handler.post {
                keyboardView?.setError(error)
            }
        }
    }

    private fun onMicTapped() {
        val state = keyboardView?.getState() ?: KeyboardView.State.IDLE

        when (state) {
            KeyboardView.State.IDLE -> {
                // Check permission before recording
                if (checkRecordingPermission()) {
                    startRecording()
                } else {
                    requestRecordingPermission()
                }
            }
            KeyboardView.State.RECORDING -> {
                stopRecording()
            }
            KeyboardView.State.PROCESSING -> {
                // Ignore taps during processing
            }
            KeyboardView.State.ERROR -> {
                // Retry from error state
                keyboardView?.setState(KeyboardView.State.IDLE)
            }
        }
    }

    private fun onSwitchKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun onBackspace() {
        val inputConnection = pendingInputConnection ?: currentInputConnection
        inputConnection?.let {
            // Delete one character before cursor
            it.deleteSurroundingText(1, 0)
            Log.d(TAG, "Backspace")
        } ?: run {
            Log.w(TAG, "No input connection for backspace")
        }
    }

    private fun onEnter() {
        val inputConnection = pendingInputConnection ?: currentInputConnection
        inputConnection?.let {
            // Send enter key event
            it.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER))
            it.sendKeyEvent(android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER))
            Log.d(TAG, "Enter")
        } ?: run {
            Log.w(TAG, "No input connection for enter")
        }
    }

    private fun onSpace() {
        val inputConnection = pendingInputConnection ?: currentInputConnection
        inputConnection?.let {
            // Insert space character
            it.commitText(" ", 1)
            Log.d(TAG, "Space")
        } ?: run {
            Log.w(TAG, "No input connection for space")
        }
    }

    private fun checkRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRecordingPermission() {
        // IME cannot directly request permissions - need to launch MainActivity
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("request_permission", true)
        startActivity(intent)
    }

    private fun startRecording() {
        val started = audioRecorder?.startRecording() ?: false
        if (!started) {
            keyboardView?.setError("Failed to start recording")
        }
    }

    private fun stopRecording() {
        audioRecorder?.stopRecording()
    }

    private fun transcribeAudio(wavFile: File) {
        transcriptionClient?.transcribe(wavFile, object : TranscriptionClient.TranscriptionCallback {
            override fun onSuccess(text: String) {
                handler.post {
                    insertText(text)
                    keyboardView?.setState(KeyboardView.State.IDLE)
                    // Clean up temp file after successful transcription
                    wavFile.delete()
                }
            }

            override fun onError(error: String) {
                handler.post {
                    keyboardView?.setError(error)
                    wavFile.delete()
                }
            }
        })
    }

    private fun insertText(text: String) {
        val inputConnection = pendingInputConnection ?: currentInputConnection
        inputConnection?.let {
            // Commit the transcribed text
            it.commitText(text, 1)
            Log.d(TAG, "Inserted text: $text")
        } ?: run {
            Log.w(TAG, "No input connection available for text insertion")
            keyboardView?.setError("No text field active")
        }
    }

    // Helper to set click listeners on KeyboardView
    private fun KeyboardView.setOnMicTapListener(listener: () -> Unit) {
        this.onMicTap = listener
    }

    private fun KeyboardView.setOnSwitchKeyboardListener(listener: () -> Unit) {
        this.onSwitchKeyboard = listener
    }

    private fun KeyboardView.setOnBackspaceListener(listener: () -> Unit) {
        this.onBackspace = listener
    }

    private fun KeyboardView.setOnEnterListener(listener: () -> Unit) {
        this.onEnter = listener
    }

    private fun KeyboardView.setOnSpaceListener(listener: () -> Unit) {
        this.onSpace = listener
    }
}