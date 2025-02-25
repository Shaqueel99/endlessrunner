package com.example.endlessrunner

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
/**
 * A custom view that displays a vertically scrolling background.
 * The background scrolls continuously and applies a tint overlay.
 */
class ScrollingBackgroundView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    // Load your background image (ensure R.drawable.bg1 is tileable vertically)
    private val backgroundBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg1)
    private val paint = Paint()
    private var offset = 0f
    private val tintPaint = Paint().apply {
        color = Color.parseColor("#C93A3939")
    }
    // Animator to update the offset for continuous scrolling.

    private val animator = ValueAnimator.ofFloat(0f, backgroundBitmap.height.toFloat()).apply {
        duration = 20000L // Adjust duration to control scrolling speed.
        interpolator = LinearInterpolator()  // Ensures constant speed.
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        addUpdateListener {
            offset = it.animatedValue as Float
            invalidate() // Redraw the view.
        }
        start()
    }
    /**
     * Draws the scrolling background and applies a tint overlay.
     *
     * @param canvas The canvas on which the background is drawn.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val bgHeight = backgroundBitmap.height.toFloat()
        // Calculate the vertical offset (wrap around using modulo)
        val yOffset = offset % bgHeight
        // Draw the first copy of the background.
        canvas.drawBitmap(backgroundBitmap, 0f, yOffset, paint)
        // Draw a second copy if needed to fill the gap.
        if (yOffset > 0) {
            canvas.drawBitmap(backgroundBitmap, 0f, yOffset - bgHeight, paint)
        }
        // Draw the tint overlay covering the whole view.
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tintPaint)
    }
}
