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
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
    }

    // Runner is now represented as a PhysicsBody.
    private lateinit var runnerBody: PhysicsBody
    private val runnerSize = 100f

    // We'll add a rotation property for the runner (in degrees).
    private var runnerRotation = 0f
    // Define a constant for how many degrees to rotate per frame while airborne.
    private val flipSpeed = 10f

    // Physics parameters.
    private val gravity = 2f
    private val jumpVelocity = -40f

    // Ground properties.
    var groundSpeed = 10f
    val groundHeight = 150f
    private var groundBitmap: Bitmap? = null

    // Background properties.
    private var backgroundBitmap: Bitmap? = null

    // Score.
    private var score: Int = 0

    // Platform Manager.
    private var platformManager: PlatformManager? = null

    // Coroutine job for game loop.
    private var gameJob: Job? = null

    // Desired runner screen x position (so runner appears fixed at this x).
    private val desiredRunnerX = 200f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startGameLoop()
    }

    private fun startGameLoop() {
        gameJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateGame()
                invalidate()
                delay(16L)  // Approximately 60 FPS.
            }
        }
    }

    private fun updateGame() {
        if (width == 0) return

        score++

        // Update runner's world position horizontally.
        runnerBody.x += groundSpeed

        // Update platforms.
        platformManager?.update(groundSpeed)

        // Apply gravity.
        runnerBody.vy += gravity
        runnerBody.y += runnerBody.vy

        // Check collisions with each platform.
        platformManager?.platforms?.forEach { platform ->
            val platformBody = PhysicsBody(
                x = platform.x,
                y = platform.y,
                width = platform.width,
                height = platform.height,
                vx = 0f,
                vy = 0f
            )
            if (aabbCollision(runnerBody, platformBody)) {
                resolveCollision(runnerBody, platformBody)
            }
        }

        // If runner is airborne (vertical velocity above a small epsilon), update flip.
        if (kotlin.math.abs(runnerBody.vy) > 0.5f) {
            runnerRotation += flipSpeed
        } else {
            // Consider the runner to be "on the ground" when vertical velocity is nearly zero.
            runnerRotation = 0f
        }

        // Reset the game if the runner falls off the bottom.
        if (runnerBody.y > height) {
            resetGame()
        }
    }


    private fun resetGame() {
        score = 0
        runnerBody.vx = 0f
        runnerBody.vy = 0f
        runnerRotation = 0f
        // Reset runner's position: world x resets to 200, and runner is placed 200px above ground.
        runnerBody.x = 200f
        runnerBody.y = height - groundHeight - runnerBody.height - 200f
        platformManager?.platforms?.clear()
        platformManager = PlatformManager(width, height, groundHeight, groundSpeed)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // --- Draw Background with Parallax ---
        val parallaxFactor = 0.5f
        val rawBgOffset = (runnerBody.x - desiredRunnerX) * parallaxFactor
        val bmp = backgroundBitmap
        if (bmp != null) {
            var effectiveBgOffset = - (rawBgOffset % bmp.width)
            if (effectiveBgOffset > 0) {
                effectiveBgOffset -= bmp.width
            }
            var x = effectiveBgOffset
            while (x < width) {
                canvas.drawBitmap(bmp, x, 0f, null)
                x += bmp.width
            }
        } else {
            canvas.drawColor(Color.LTGRAY)
        }

        // --- Draw World Elements with Camera Translation ---
        val cameraOffsetX = runnerBody.x - desiredRunnerX
        canvas.save()
        canvas.translate(-cameraOffsetX, 0f)

        // Draw platforms.
        paint.color = Color.DKGRAY
        platformManager?.platforms?.forEach { platform ->
            canvas.drawRect(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height, paint)
        }

        // Draw runner with flip.
        canvas.save()
        // Rotate about the runner's center.
        val runnerCenterX = runnerBody.x + runnerBody.width / 2
        val runnerCenterY = runnerBody.y + runnerBody.height / 2
        canvas.rotate(runnerRotation, runnerCenterX, runnerCenterY)
        paint.color = Color.BLUE
        canvas.drawRect(runnerBody.x, runnerBody.y, runnerBody.x + runnerBody.width, runnerBody.y + runnerBody.height, paint)
        canvas.restore()

        canvas.restore()

        // --- Draw UI Elements ---
        canvas.drawText("Score: $score", 50f, 100f, textPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameJob?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val startY = h - groundHeight - runnerSize - 200f  // 200px above ground.
        runnerBody = PhysicsBody(
            x = 200f,
            y = startY,
            width = runnerSize,
            height = runnerSize,
            vx = 0f,
            vy = 0f,
            mass = 1f,
            restitution = 0.0f
        )
        platformManager = PlatformManager(w, h, groundHeight, groundSpeed)
        // Load and scale background image.
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.desertnight_0)
        backgroundBitmap?.let { bmp ->
            val scale = h.toFloat() / bmp.height
            val newWidth = (bmp.width * scale).toInt()
            backgroundBitmap = Bitmap.createScaledBitmap(bmp, newWidth, h, true)
        }
        // Load and scale ground image.
        groundBitmap = BitmapFactory.decodeResource(resources, R.drawable.desertnight_4)
        groundBitmap?.let { bmp ->
            val scale = groundHeight / bmp.height.toFloat()
            val newWidth = (bmp.width * scale).toInt()
            groundBitmap = Bitmap.createScaledBitmap(bmp, newWidth, groundHeight.toInt(), true)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (runnerBody.vy == 0f) {
                    runnerBody.vy = jumpVelocity
                }
                performClick()
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
