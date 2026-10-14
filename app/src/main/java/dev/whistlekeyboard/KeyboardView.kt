package dev.whistlekeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Keyboard view with microphone button and switch keyboard hint.
 * Normal keyboard size (not fullscreen).
 */
class KeyboardView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @JvmOverloads constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    enum class State {
        IDLE,
        RECORDING,
        PROCESSING,
        ERROR
    }

    private var currentState: State = State.IDLE
    private var errorMessage: String? = null

    // Views
    private var micButton: Button? = null
    private var recordingIndicator: View? = null
    private var processingIndicator: ProgressBar? = null
    private var errorIcon: TextView? = null
    private var errorMessageView: TextView? = null
    private var switchKeyboardHint: TextView? = null

    // Callbacks
    var onMicTap: (() -> Unit)? = null
    var onSwitchKeyboard: (() -> Unit)? = null

    // Animation
    private var pulseAnimation: Animation? = null
    private var rotateAnimation: Animation? = null

    init {
        inflateView()
        setupViews()
        setupAnimations()
    }

    private fun inflateView() {
        LayoutInflater.from(context).inflate(R.layout.keyboard_view, this, true)
    }

    private fun setupViews() {
        micButton = findViewById(R.id.micButton)
        recordingIndicator = findViewById(R.id.recordingIndicator)
        processingIndicator = findViewById(R.id.processingIndicator)
        errorIcon = findViewById(R.id.errorIcon)
        errorMessageView = findViewById(R.id.errorMessage)
        switchKeyboardHint = findViewById(R.id.switchKeyboardHint)

        micButton?.setOnClickListener {
            onMicTap?.invoke()
        }

        switchKeyboardHint?.setOnClickListener {
            onSwitchKeyboard?.invoke()
        }
    }

    private fun setupAnimations() {
        // Pulsing animation for recording
        pulseAnimation = android.view.animation.ScaleAnimation(
            1f, 1.3f, 1f, 1.3f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 800
            repeatCount = Animation.INFINITE
            repeatMode = Animation.REVERSE
            interpolator = LinearInterpolator()
        }

        // Rotating animation for processing
        rotateAnimation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 1000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
    }

    fun setState(state: State) {
        currentState = state
        errorMessage = null
        updateUI()
    }

    fun setError(message: String) {
        currentState = State.ERROR
        errorMessage = message
        updateUI()
    }

    fun getState(): State = currentState

    private fun updateUI() {
        when (currentState) {
            State.IDLE -> showIdleState()
            State.RECORDING -> showRecordingState()
            State.PROCESSING -> showProcessingState()
            State.ERROR -> showErrorState()
        }
    }

    private fun showIdleState() {
        micButton?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.mic_button_background)
            contentDescription = context.getString(R.string.keyboard_mic_idle)
        }
        hideAllIndicators()
        errorMessageView?.visibility = View.GONE
        switchKeyboardHint?.visibility = View.VISIBLE
    }

    private fun showRecordingState() {
        micButton?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.mic_button_recording)
            contentDescription = context.getString(R.string.keyboard_mic_recording)
        }
        hideAllIndicators()
        recordingIndicator?.visibility = View.VISIBLE
        recordingIndicator?.startAnimation(pulseAnimation)
        errorMessageView?.visibility = View.GONE
        switchKeyboardHint?.visibility = View.GONE
    }

    private fun showProcessingState() {
        micButton?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.mic_button_processing)
            contentDescription = context.getString(R.string.keyboard_mic_processing)
        }
        hideAllIndicators()
        processingIndicator?.visibility = View.VISIBLE
        processingIndicator?.startAnimation(rotateAnimation)
        errorMessageView?.visibility = View.GONE
        switchKeyboardHint?.visibility = View.GONE
    }

    private fun showErrorState() {
        micButton?.apply {
            background = ContextCompat.getDrawable(context, R.drawable.mic_button_error)
            contentDescription = "Error"
        }
        hideAllIndicators()
        errorIcon?.visibility = View.VISIBLE
        errorMessageView?.apply {
            text = errorMessage
            visibility = View.VISIBLE
        }
        switchKeyboardHint?.visibility = View.VISIBLE
    }

    private fun hideAllIndicators() {
        recordingIndicator?.apply {
            visibility = View.GONE
            clearAnimation()
        }
        processingIndicator?.apply {
            visibility = View.GONE
            clearAnimation()
        }
        errorIcon?.visibility = View.GONE
    }
}