package com.example.endlessrunner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.*

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint()

    // Runner (square) properties
    var squareX = 200f
    var squareY = 300f
    val squareSize = 100f

    // Gravity and jump properties
    private var velocityY = 0f
    private val gravity = 2f
    private val jumpVelocity = -40f

    // Ground (platform) properties
    var groundSpeed = 10f
    var groundOffset = 0f
    val groundHeight = 150f

    // Background properties
    var backgroundSpeed = groundSpeed / 2  // slower for a parallax effect
    var backgroundOffset = 0f
    private var backgroundBitmap: Bitmap? = null

    // Coroutine job for game loop
    private var gameJob: Job? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateGame()   // update positions/state
                invalidate()   // trigger redraw
                delay(16L)     // roughly 60 fps
            }
        }
    }

    private fun updateGame() {
        if (width == 0) return

        // Update background offset for scrolling effect using the bitmap's width
        backgroundOffset -= backgroundSpeed
        backgroundBitmap?.let { bmp ->
            if (backgroundOffset <= -bmp.width) {
                // Reset offset by adding bmp.width (or use modulus for a more general solution)
                backgroundOffset += bmp.width
            }
        }

        // Update the ground offset for scrolling effect (using view width here)
        groundOffset -= groundSpeed
        if (groundOffset <= -width.toFloat()) {
            groundOffset = 0f
        }

        // Update jump/gravity for the runner
        val groundLevel = height - groundHeight - squareSize
        if (squareY < groundLevel || velocityY < 0f) {
            velocityY += gravity
            squareY += velocityY
            if (squareY > groundLevel) {
                squareY = groundLevel
                velocityY = 0f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw scrolling background image if available
        backgroundBitmap?.let { bmp ->
            // Draw enough copies to cover the entire width of the view.
            var x = backgroundOffset
            while (x < width) {
                canvas.drawBitmap(bmp, x, 0f, null)
                x += bmp.width
            }
        } ?: run {
            // Fallback: fill with a default color if no image is loaded.
            canvas.drawColor(Color.LTGRAY)
        }

        // Draw the ground on top of the background
        paint.color = Color.WHITE
        canvas.drawRect(groundOffset, height - groundHeight, groundOffset + width, height.toFloat(), paint)
        if (groundOffset < 0) {
            canvas.drawRect(groundOffset + width, height - groundHeight, groundOffset + 2 * width, height.toFloat(), paint)
        }

        // Draw the runner (a blue square) on top of everything
        paint.color = Color.BLUE
        canvas.drawRect(squareX, squareY, squareX + squareSize, squareY + squareSize, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameJob?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Position the runner so its bottom aligns with the ground.
        squareY = h - groundHeight - squareSize

        // Load the background image and scale it so its height matches the view height,
        // preserving the aspect ratio.
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.desertnight_0)
        backgroundBitmap?.let { bmp ->
            val scale = h.toFloat() / bmp.height
            val newWidth = (bmp.width * scale).toInt()
            backgroundBitmap = Bitmap.createScaledBitmap(bmp, newWidth, h, true)
        }
    }

    // Detect taps to trigger a jump
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                val groundLevel = height - groundHeight - squareSize
                if (squareY >= groundLevel) {
                    velocityY = jumpVelocity
                }
                performClick() // for accessibility
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
