package com.example.muschool // ✅ Must match XML reference

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*

class MuschoolLoader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 48f
    }

    private val primaryColor = Color.parseColor("#00E5FF") // Cyan
    private val secondaryColor = Color.parseColor("#00C853") // Green
    private val darkBlue = Color.parseColor("#1A237E")

    private var rotationAngle = 0f
    private var pulseScale = 1f
    private var outerRingProgress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { animation ->
            rotationAngle = animation.animatedValue as Float
            outerRingProgress = (animation.animatedValue as Float) / 360f
            invalidate()
        }
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0.8f, 1.2f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { animation ->
            pulseScale = animation.animatedValue as Float
            invalidate()
        }
    }

    init {
        setBackgroundColor(darkBlue)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    private fun startAnimation() {
        animator.start()
        pulseAnimator.start()
    }

    private fun stopAnimation() {
        animator.cancel()
        pulseAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = minOf(width, height) * 0.15f

        canvas.save()
        canvas.translate(centerX, centerY)

        drawOuterRing(canvas, baseRadius * 2.2f)
        drawMiddleCircle(canvas, baseRadius * 1.6f)
        drawInnerElements(canvas, baseRadius)
        drawCenterLogo(canvas)

        canvas.restore()
        drawBrandText(canvas, centerX, centerY + baseRadius * 3f)
    }

    private fun drawOuterRing(canvas: Canvas, radius: Float) {
        paint.color = primaryColor
        paint.strokeWidth = 6f
        paint.alpha = 180

        val sweepAngle = 120f
        val startAngle = rotationAngle - sweepAngle / 2

        canvas.drawArc(-radius, -radius, radius, radius, startAngle, sweepAngle, false, paint)

        paint.strokeWidth = 4f
        for (i in 0..7) {
            val angle = Math.toRadians((rotationAngle + i * 45).toDouble())
            val startR = radius + 10f
            val endR = radius + 25f + sin(rotationAngle * 2 * PI / 180) * 8f
            canvas.drawLine(
                (cos(angle) * startR).toFloat(),
                (sin(angle) * startR).toFloat(),
                (cos(angle) * endR).toFloat(),
                (sin(angle) * endR).toFloat(),
                paint
            )
        }
    }

    private fun drawMiddleCircle(canvas: Canvas, radius: Float) {
        paint.color = secondaryColor
        paint.strokeWidth = 4f
        paint.alpha = (255 * pulseScale).toInt().coerceIn(100, 255)
        canvas.drawCircle(0f, 0f, radius * pulseScale, paint)
    }

    private fun drawInnerElements(canvas: Canvas, baseRadius: Float) {
        canvas.save()
        canvas.rotate(-rotationAngle * 1.5f)
        paint.color = primaryColor
        paint.strokeWidth = 8f
        paint.alpha = 255

        for (i in 0..2) {
            canvas.save()
            canvas.rotate(i * 120f)
            val path = Path().apply {
                moveTo(0f, -baseRadius * 0.3f)
                quadTo(baseRadius * 0.6f, -baseRadius * 0.1f, baseRadius * 0.4f, baseRadius * 0.5f)
            }
            canvas.drawPath(path, paint)
            canvas.restore()
        }
        canvas.restore()
    }

    private fun drawCenterLogo(canvas: Canvas) {
        textPaint.color = Color.WHITE
        textPaint.textSize = 36f
        canvas.drawText("M", 0f, 12f, textPaint)

        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        val trianglePath = Path().apply {
            moveTo(0f, -25f)
            lineTo(-8f, -12f)
            lineTo(8f, -12f)
            close()
        }
        canvas.drawPath(trianglePath, paint)
        paint.style = Paint.Style.STROKE
    }

    private fun drawBrandText(canvas: Canvas, x: Float, y: Float) {
        textPaint.color = primaryColor
        textPaint.alpha = (255 * (0.7f + 0.3f * sin(rotationAngle * PI / 180))).toInt()
        textPaint.textSize = 32f
        textPaint.letterSpacing = 0.2f
        canvas.drawText("MUSCHOOL", x, y, textPaint)
    }

    fun show() {
        visibility = VISIBLE
        startAnimation()
    }

    fun hide() {
        visibility = GONE
        stopAnimation()
    }

    fun setLoadingText(text: String) {
        // Extend for custom loading text if needed
    }
}
