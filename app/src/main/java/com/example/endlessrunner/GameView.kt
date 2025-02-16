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

    // Physics parameters.
    private val gravity = 2f
    private val jumpVelocity = -40f

    // Ground properties.
    var groundSpeed = 10f
    // (Remove groundOffset – we’ll use world coordinates.)
    val groundHeight = 150f
    private var groundBitmap: Bitmap? = null

    // Background properties.
    // (Remove backgroundOffset – we'll calculate its effective drawing position from world coordinates.)
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
        platformManager?.update(groundSpeed)  // We'll modify PlatformManager to use world coordinates.

        // Apply gravity.
        runnerBody.vy += gravity
        runnerBody.y += runnerBody.vy

        // Check collisions with each platform.
        platformManager?.platforms?.forEach { platform ->
            // Create a temporary PhysicsBody for the platform.
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

        // Reset the game if the runner falls off the bottom.
        if (runnerBody.y > height) {
            resetGame()
        }
    }

    private fun resetGame() {
        score = 0
        runnerBody.vx = 0f
        runnerBody.vy = 0f
        // Reset runner's position: keep its world x as is or reset to a starting value,
        // and place it 200px above the ground.
        runnerBody.x = 200f  // Alternatively, you might keep world continuity.
        runnerBody.y = height - groundHeight - runnerBody.height - 200f
        platformManager?.platforms?.clear()
        platformManager = PlatformManager(width, height, groundHeight, groundSpeed)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // --- Draw Background with Parallax ---
        // Define a parallax factor (e.g., 0.5 means background moves at half the player's speed)
        val parallaxFactor = 0.5f
        // Compute the effective background offset from the runner's world position.
        // We use (runnerBody.x - desiredRunnerX) because desiredRunnerX is where the runner appears on screen.
        val rawBgOffset = (runnerBody.x - desiredRunnerX) * parallaxFactor
        // We want the background to loop seamlessly.
        // Compute effective offset in screen coordinates as a negative remainder of the background's width.
        val bmp = backgroundBitmap
        if (bmp != null) {
            // Compute remainder (in a way that gives a value in [-bmp.width, 0)).
            var effectiveBgOffset = - (rawBgOffset % bmp.width)
            if (effectiveBgOffset > 0) {
                effectiveBgOffset -= bmp.width
            }
            // Draw background tiles across the screen.
            var x = effectiveBgOffset
            while (x < width) {
                canvas.drawBitmap(bmp, x, 0f, null)
                x += bmp.width
            }
        } else {
            canvas.drawColor(Color.LTGRAY)
        }

        // --- Draw World Elements with Camera Translation ---
        // Compute camera offset so that the runner appears at desiredRunnerX.
        val cameraOffsetX = runnerBody.x - desiredRunnerX
        canvas.save()
        canvas.translate(-cameraOffsetX, 0f)



        // Draw platforms.
        paint.color = Color.DKGRAY
        platformManager?.platforms?.forEach { platform ->
            canvas.drawRect(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height, paint)
        }

        // Draw runner.
        paint.color = Color.BLUE
        canvas.drawRect(runnerBody.x, runnerBody.y, runnerBody.x + runnerBody.width, runnerBody.y + runnerBody.height, paint)

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
        // Initialize the runner's physics body.
        val startY = h - groundHeight - runnerSize - 200f  // 200px above ground.
        runnerBody = PhysicsBody(
            x = 200f,  // Starting world x-coordinate.
            y = startY,
            width = runnerSize,
            height = runnerSize,
            vx = 0f,
            vy = 0f,
            mass = 1f,
            restitution = 0.0f
        )
        // Initialize PlatformManager with world coordinates.
        platformManager = PlatformManager(w, h, groundHeight, groundSpeed)

        // Load and scale the background image (preserving aspect ratio).
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.desertnight_0)
        backgroundBitmap?.let { bmp ->
            val scale = h.toFloat() / bmp.height
            val newWidth = (bmp.width * scale).toInt()
            backgroundBitmap = Bitmap.createScaledBitmap(bmp, newWidth, h, true)
        }

        // Load and scale the ground image.
        groundBitmap = BitmapFactory.decodeResource(resources, R.drawable.desertnight_4)
        groundBitmap?.let { bmp ->
            val scale = groundHeight / bmp.height.toFloat()
            val newWidth = (bmp.width * scale).toInt()
            groundBitmap = Bitmap.createScaledBitmap(bmp, newWidth, groundHeight.toInt(), true)
        }
    }


    // Handle taps to trigger a jump.
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                // Jump only if the runner is not already moving vertically.
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