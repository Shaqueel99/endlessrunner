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
import kotlin.math.abs

// Data class to represent a coin.
data class Coin(var x: Float, var y: Float, val width: Float, val height: Float)

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 60f
        isAntiAlias = true
    }

    // Runner represented as a PhysicsBody.
    private lateinit var runnerBody: PhysicsBody
    private val runnerSize = 100f

    // Rotation properties.
    private var runnerRotation = 0f
    private var flipSpeed = 0f

    // Physics parameters.
    private val gravity = 2f
    private val jumpVelocity = -40f

    // Variable jump parameters.
    private var isJumping = false

    // Ground properties.
    var groundSpeed = 5f
    // Acceleration to increase groundSpeed over time.
    private val acceleration = 0.0025f
    val groundHeight = 150f
    private var groundBitmap: Bitmap? = null

    // Background properties.
    private var backgroundBitmap: Bitmap? = null

    // Score and multiplier.
    private var score: Float = 0f
    private var scoreMultiplier = 1f

    // Coins.
    private val coins = mutableListOf<Coin>()
    // Chance to spawn a coin on a platform (e.g., 1% chance per update).
    private val coinChance = 0.001
    private val coinSize = 40f

    // Platform Manager.
    private var platformManager: PlatformManager? = null

    // Coroutine job for game loop.
    private var gameJob: Job? = null

    // Desired runner screen x position.
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

        // Increase score by multiplier.
        score += scoreMultiplier

        // Ramp up groundSpeed over time.
        groundSpeed += acceleration
        flipSpeed = groundSpeed
        // Update runner's world position horizontally.
        runnerBody.x += groundSpeed

        // Update platforms using the current groundSpeed.
        platformManager?.update(groundSpeed)

        // Spawn coins on platforms if not already present.
        platformManager?.platforms?.forEach { platform ->
            // Simple check: if no coin exists on this platform (by x overlap).
            if (coins.none { coin -> coin.x >= platform.x && coin.x <= platform.x + platform.width }) {
                if (Math.random() < coinChance) {
                    spawnCoinOnPlatform(platform)
                }
            }
        }

        // Update coins: move them left along with the world.
        coins.forEach { it.x -= groundSpeed }
        coins.removeAll { it.x + it.width < 0 }

        // Apply gravity.
        runnerBody.vy += gravity
        runnerBody.y += runnerBody.vy

        // Update runner rotation (flip) when airborne.
        if (abs(runnerBody.vy) > 0.0f) {
            runnerRotation += flipSpeed
        } else {
            runnerRotation = 0f
        }

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

        // Check collision with coins.
        val coinIterator = coins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            if (runnerBody.x < coin.x + coin.width &&
                runnerBody.x + runnerBody.width > coin.x &&
                runnerBody.y < coin.y + coin.height &&
                runnerBody.y + runnerBody.height > coin.y) {
                coinIterator.remove()
                scoreMultiplier += 0.5f
            }
        }

        // Reset the game if the runner falls off the bottom.
        if (runnerBody.y > height) {
            resetGame()
        }
    }

    private fun spawnCoinOnPlatform(platform: Platform) {
        // Randomize coin's x coordinate so it sits randomly on the platform.
        val coinX = platform.x + (Math.random().toFloat() * (platform.width - coinSize))
        // Place the coin 20px higher above the platform.
        val coinY = platform.y - coinSize - 20f
        coins.add(Coin(coinX, coinY, coinSize, coinSize))
    }

    private fun resetGame() {
        score = 0f
        groundSpeed = 5f
        scoreMultiplier = 1f
        runnerBody.vx = 0f
        runnerBody.vy = 0f
        runnerRotation = 0f
        runnerBody.x = 200f
        runnerBody.y = height - groundHeight - runnerBody.height - 200f
        platformManager?.platforms?.clear()
        platformManager = PlatformManager(width, height, groundHeight, groundSpeed)
        coins.clear()
        isJumping = false
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

        // Draw coins.
        paint.color = Color.YELLOW
        coins.forEach { coin ->
            canvas.drawOval(coin.x, coin.y, coin.x + coin.width, coin.y + coin.height, paint)
        }

        // Draw runner with rotation.
        canvas.save()
        val runnerCenterX = runnerBody.x + runnerBody.width / 2
        val runnerCenterY = runnerBody.y + runnerBody.height / 2
        canvas.rotate(runnerRotation, runnerCenterX, runnerCenterY)
        paint.color = Color.BLUE
        canvas.drawRect(runnerBody.x, runnerBody.y, runnerBody.x + runnerBody.width, runnerBody.y + runnerBody.height, paint)
        canvas.restore()

        canvas.restore()

        // --- Draw UI Elements ---
        canvas.drawText("Score: ${score.toInt()}", 50f, 100f, textPaint)
        canvas.drawText("Multiplier: $scoreMultiplier", 50f, 170f, textPaint)
        canvas.drawText("Speed: ${groundSpeed.toInt()}", 50f, 240f, textPaint)

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameJob?.cancel()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val startY = h - groundHeight - runnerSize - 200f
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
                    isJumping = true
                    runnerBody.vy = jumpVelocity * 1.25f
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isJumping && runnerBody.vy < 0f) {
                    runnerBody.vy *= 0.5f
                }
                isJumping = false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
