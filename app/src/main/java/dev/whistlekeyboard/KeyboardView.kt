package dev.whistlekeyboard

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.inputmethodservice.InputMethodService
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat

/**
 * Custom keyboard view with a microphone button and state indicators.
 * States: IDLE, RECORDING, PROCESSING, ERROR
 */
class KeyboardView(
    context: Context,
    attrs: AttributeSet,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {  
    @JvmOverloads constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    enum class State {
        IDLE,
        RECORDING,
        PROCESSING,
        ERROR
    }

    private var currentState: State = State.IDLE
    private var errorMessage: String? = null

    // UI dimensions
    private val buttonRadius = 80f
    private val indicatorRadius = 12f
    private val strokeWidth = 4f

    // Paints
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val errorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val switchPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Button bounds
    private val buttonRect = RectF()
    private val switchRect = RectF()

    // Callbacks
    var onMicTap: (() -> Unit)? = null
    var onSwitchKeyboard: (() -> Unit)? = null

    init {
        initPaints()
    }

    private fun initPaints() {
        // Microphone button paint
        buttonPaint.style = Paint.Style.FILL
        buttonPaint.color = ContextCompat.getColor(context, R.color.idle_gray)

        // Recording indicator paint
        indicatorPaint.style = Paint.Style.FILL
        indicatorPaint.color = ContextCompat.getColor(context, R.color.recording_red)

        // Text paint
        textPaint.textSize = 36f
        textPaint.color = ContextCompat.getColor(context, R.color.white)
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.isAntiAlias = true

        // Error text paint
        errorPaint.textSize = 28f
        errorPaint.color = ContextCompat.getColor(context, R.color.error)
        errorPaint.textAlign = Paint.Align.CENTER
        errorPaint.isAntiAlias = true

        // Switch keyboard text paint
        switchPaint.textSize = 24f
        switchPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        switchPaint.textAlign = Paint.Align.CENTER
        switchPaint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateLayout(w, h)
    }

    private fun updateLayout(width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f

        // Microphone button centered
        buttonRect.set(
            centerX - buttonRadius,
            centerY - buttonRadius,
            centerX + buttonRadius,
            centerY + buttonRadius
        )

        // Switch keyboard text at bottom
        val switchText = context.getString(R.string.keyboard_switch_keyboard)
        val textBounds = Rect()
        switchPaint.getTextBounds(switchText, 0, switchText.length, textBounds)
        switchRect.set(
            centerX - textBounds.width() / 2f - 24,
            height.toFloat() - textBounds.height() - 40,
            centerX + textBounds.width() / 2f + 24,
            height.toFloat() - 20
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // Update button color based on state
        when (currentState) {
            State.IDLE -> buttonPaint.color = ContextCompat.getColor(context, R.color.idle_gray)
            State.RECORDING -> buttonPaint.color = ContextCompat.getColor(context, R.color.recording_red)
            State.PROCESSING -> buttonPaint.color = ContextCompat.getColor(context, R.color.processing_blue)
            State.ERROR -> buttonPaint.color = ContextCompat.getColor(context, R.color.error)
        }

        // Draw microphone button
        canvas.drawRoundRect(buttonRect, buttonRadius, buttonRadius, buttonPaint)

        // Draw microphone icon or state indicator
        when (currentState) {
            State.IDLE -> drawMicIcon(canvas, centerX, centerY)
            State.RECORDING -> drawRecordingIndicator(canvas, centerX, centerY)
            State.PROCESSING -> drawProcessingIndicator(canvas, centerX, centerY)
            State.ERROR -> drawErrorIcon(canvas, centerX, centerY)
        }

        // Draw error message if present
        errorMessage?.let { msg ->
            val y = centerY + buttonRadius + 60
            canvas.drawText(msg, centerX, y, errorPaint)
        }

        // Draw switch keyboard hint
        val switchText = context.getString(R.string.keyboard_switch_keyboard)
        canvas.drawText(switchText, centerX, switchRect.bottom - 8, switchPaint)
    }

    private fun drawMicIcon(canvas: Canvas, cx: Float, cy: Float) {
        // Simple microphone icon using shapes
        val micWidth = 24f
        val micHeight = 36f
        val handleWidth = 4f

        // Mic body
        val bodyRect = RectF(
            cx - micWidth / 2,
            cy - micHeight / 2,
            cx + micWidth / 2,
            cy + micHeight / 2 - 8
        )
        val bodyPaint = Paint(buttonPaint)
        bodyPaint.color = ContextCompat.getColor(context, R.color.white)
        canvas.drawRoundRect(bodyRect, 12f, 12f, bodyPaint)

        // Mic handle
        val handleRect = RectF(
            cx - handleWidth / 2,
            cy + micHeight / 2 - 8,
            cx + handleWidth / 2,
            cy + micHeight / 2 + 8
        )
        canvas.drawRect(handleRect, bodyPaint)

        // Mic stand
        val standPaint = Paint(bodyPaint)
        standPaint.style = Paint.Style.STROKE
        standPaint.strokeWidth = 3f
        val standRect = RectF(
            cx - 20,
            cy + micHeight / 2 + 8,
            cx + 20,
            cy + micHeight / 2 + 20
        )
        canvas.drawArc(standRect, 180f, 180f, false, standPaint)
    }

    private fun drawRecordingIndicator(canvas: Canvas, cx: Float, cy: Float) {
        // Pulsing red circle
        val time = System.currentTimeMillis() % 1000
        val scale = 0.8f + 0.4f * (time / 1000f)
        val radius = indicatorRadius * scale
        canvas.drawCircle(cx, cy, radius, indicatorPaint)
    }

    private fun drawProcessingIndicator(canvas: Canvas, cx: Float, cy: Float) {
        // Spinning arcs
        val time = System.currentTimeMillis() % 1200
        val angle = (time / 1200f) * 360f
        val radius = indicatorRadius * 2.5f
        val arcPaint = Paint(indicatorPaint)
        arcPaint.style = Paint.Style.STROKE
        arcPaint.strokeWidth = 4f
        arcPaint.color = ContextCompat.getColor(context, R.color.white)

        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(rect, angle, 120f, false, arcPaint)
    }

    private fun drawErrorIcon(canvas: Canvas, cx: Float, cy: Float) {
        // Exclamation mark
        val paint = Paint(textPaint)
        paint.color = ContextCompat.getColor(context, R.color.white)
        paint.textSize = 48f
        canvas.drawText("!", cx, cy + 16, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (buttonRect.contains(event.x, event.y)) {
                    onMicTap?.invoke()
                    return true
                }
                if (switchRect.contains(event.x, event.y)) {
                    onSwitchKeyboard?.invoke()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun setState(state: State) {
        currentState = state
        errorMessage = null
        invalidate()
    }

    fun setError(message: String) {
        currentState = State.ERROR
        errorMessage = message
        invalidate()
    }

    fun getState(): State = currentState
}