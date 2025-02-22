package com.example.endlessrunner

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import android.graphics.RectF
class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle), SensorEventListener {

    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 60f
        // Set shadow: radius, x-offset, y-offset, shadow color.
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }
    private var equippedSkinFromFirebase: String = "default"
    private var profileSkinBitmap: Bitmap? = null
    private var backgroundOffset = 0f
    private var backgroundBitmap: Bitmap? = null
    private var platformBitmap: Bitmap? = null
    private var movingPlatformBitmap: Bitmap? = null
    private var breakablePlatformBitmap: Bitmap? = null

    // Physics body for the player.
    private lateinit var squareBody: PhysicsBody
    private var coinscollected = 0
    private val platformRect = RectF()

    // Physics parameters.
    private val gravity = 2f
    private val jumpVelocity = -60f
    private var scrollThreshold = 100f // Vertical scroll threshold

    // Score based on height reached.
    private var score: Float = 0f

    // Platform Manager.
    private var platformManager: PlatformManager? = null

    // Game loop coroutine.
    private var gameJob: Job? = null
    val tintedPaint = Paint().apply {
        colorFilter = PorterDuffColorFilter(Color.argb(100, 0, 0, 0), PorterDuff.Mode.DARKEN)
    }

    // **Accelerometer Setup**
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var tiltSensitivity = 4.0f // Adjust to make movement smoother/slower

    // **Control Inputs from Tilt and Touch**
    private var tiltControl = 0f   // Updated from accelerometer
    private var touchControl = 0f  // Updated from touch events

    private val boostJumpVelocity = -100f //

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadEquippedSkinFromFirebase()

        startGameLoop()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameJob?.cancel()
        sensorManager.unregisterListener(this) // Stop listening to accelerometer
    }
    private fun loadEquippedSkinFromFirebase() {
        val sharedPrefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").document(username)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        equippedSkinFromFirebase = document.getString("equippedSkin") ?: "default"
                        if (equippedSkinFromFirebase == "profile") {
                            val profileImageUrl = document.getString("profileImagePath") ?: ""
                            if (profileImageUrl.isNotEmpty()) {
                                // Load the image off the main thread using Glide.
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val bitmap = Glide.with(context)
                                            .asBitmap()
                                            .load(profileImageUrl)
                                            .submit()
                                            .get()
                                        profileSkinBitmap = Bitmap.createScaledBitmap(bitmap!!, squareBody.width.toInt(), squareBody.width.toInt(), false)


                                        withContext(Dispatchers.Main) {
                                            invalidate()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("GameView", "Failed to load profile image: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load skin: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun createColorSquare(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        return bmp
    }
    private fun startGameLoop() {
        gameJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateGame()
                invalidate()
                delay(16L) // ~60 FPS.
            }
        }
    }

    private fun updateGame() {
        if (width == 0 || height == 0) return

        // Update squareBody.vx as the sum of tilt and touch inputs.
        squareBody.vx = tiltControl * tiltSensitivity + touchControl
        squareBody.x += squareBody.vx

        val prevBottom = squareBody.y + squareBody.height
        squareBody.vy += gravity
        squareBody.y += squareBody.vy
        val newBottom = squareBody.y + squareBody.height
        var offset = 0f

        if (squareBody.y < scrollThreshold) {
            offset = scrollThreshold - squareBody.y
            squareBody.y = scrollThreshold
            score += offset
            // Update background offset; using a multiplier (e.g., 0.5f) for a parallax effect
            backgroundOffset += offset * 0.5f  // Adjust multiplier as needed for parallax effect.
        }

        platformManager?.update(offset, score)

        // Wrap-around screen horizontally.
        if (squareBody.x > width) {
            squareBody.x = 0f
        }
        else if (squareBody.x + squareBody.width < 0) {
            squareBody.x = width - squareBody.width
        }

        // Check collision with platforms.
        if (squareBody.vy > 0) {
            platformManager?.platforms?.toList()?.forEach { platform ->
                if (CollisionUtils.isCollidingWithPlatform(squareBody, platform, prevBottom, newBottom)) {
                    if (platform.isBreakable) {
                        platformManager?.platforms?.remove(platform)
                    }
                    squareBody.y = platform.y - squareBody.height
                    squareBody.vy = jumpVelocity
                }
            }
        }

        // Check collision with coins.
        platformManager?.coins?.removeIf { coin ->
            if (CollisionUtils.isCollidingWithCoin(squareBody, coin)) {
                coinscollected += 1
                true
            }
            else {
                false
            }
        }

        //  Apply Boost Jump Immediately Upon Collection
        platformManager?.boosts?.removeIf { boost ->
            if (CollisionUtils.isCollidingWithBoost(squareBody, boost)) { // Use new function
                squareBody.vy = boostJumpVelocity // Instant boost effect
                true // Remove boost after collection
            } else {
                false
            }
        }

        // Game Over Condition.
        if (squareBody.y > height) {
            val sharedPrefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = sharedPrefs.getString("username", null)

            if (username != null) {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").document(username)
                    .get()
                    .addOnSuccessListener { document ->
                        val previousCoins = document.getLong("coinsCollected") ?: 0L
                        val newTotal = previousCoins + coinscollected
                        firestore.collection("users").document(username)
                            .update("coinsCollected", newTotal)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Coins updated successfully: $newTotal")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Failed to update coins: ${e.message}")
                                Toast.makeText(context, "Failed to update coins.", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to retrieve user data: ${e.message}")
                        Toast.makeText(context, "Error retrieving user data.", Toast.LENGTH_SHORT).show()
                    }
            }
            //Transition to Game Over screen
            gameJob?.cancel()
            Toast.makeText(context, "Game Over! Score: ${score.toInt()}", Toast.LENGTH_LONG).show()
            val intent = Intent(context, GameOverActivity::class.java)
            intent.putExtra("score", score.toInt())
            context.startActivity(intent)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        backgroundBitmap?.let { bg ->
            val bgHeight = bg.height.toFloat()
            val yOffset = ((backgroundOffset % bgHeight) + bgHeight) % bgHeight

            canvas.drawBitmap(bg, 0f, yOffset, tintedPaint)
            if (yOffset > 0) {
                canvas.drawBitmap(bg, 0f, yOffset - bgHeight, tintedPaint)
            }
        } ?: canvas.drawColor(Color.LTGRAY)

        // Draw platforms using bitmaps.
        platformManager?.platforms?.forEach { platform ->
            val bmp: Bitmap? = when {
                platform.isBreakable -> breakablePlatformBitmap
                platform.isMoving -> movingPlatformBitmap
                else -> platformBitmap
            }
            bmp?.let {
                // Reuse the preallocated RectF to avoid object allocations.
                platformRect.set(platform.x, platform.y, platform.x + platform.width, platform.y + platform.height)
                canvas.drawBitmap(it, null, platformRect, null)
            }
        }


        paint.color = Color.YELLOW
        platformManager?.coins?.forEach { coin ->
            canvas.drawCircle(coin.x + coin.size / 2, coin.y + coin.size / 2, coin.size / 2, paint)
        }

        paint.color = Color.CYAN
        platformManager?.boosts?.forEach { boost ->
            canvas.drawOval(boost.x, boost.y, boost.x + boost.size, boost.y + boost.size, paint)
        }

        val playerBitmap = when (equippedSkinFromFirebase) {
            "red" -> createColorSquare(Color.RED, squareBody.width.toInt())
            "green" -> createColorSquare(Color.GREEN, squareBody.width.toInt())
            "profile" -> {
                if (profileSkinBitmap != null) {
                    profileSkinBitmap ?: createColorSquare(Color.MAGENTA, squareBody.width.toInt())
                } else {
                    createColorSquare(Color.MAGENTA, squareBody.width.toInt())
                }
            }
            else -> createColorSquare(Color.BLUE, squareBody.width.toInt())
        }

        // Draw player.
        canvas.drawBitmap(playerBitmap, squareBody.x, squareBody.y, paint)
        canvas.drawText("Score: ${score.toInt()}", 50f, 100f, textPaint)
        canvas.drawText("Coins: $coinscollected", 50f, 170f, textPaint)
    }



    // **Accelerometer: Update Tilt Control**
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            tiltControl = -event.values[0] * 2.0f
        }
    }

    // **Touch Control: Update Touch Control Value**
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val touchX = it.x
                    val centerX = width / 2f
                    touchControl = (touchX - centerX) / 20f
                }
                MotionEvent.ACTION_UP -> {
                    touchControl = 0f
                }
            }
        }
        return true
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        squareBody = PhysicsBody((w - 80f) / 2f, h - 180f, 80f, 80f, 0f, jumpVelocity)
        scrollThreshold = h / 3f
        platformManager = PlatformManager(w, h)

        // Load the background image and scale it to the view's width if needed
        backgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.bg1)
        platformBitmap = BitmapFactory.decodeResource(resources, R.drawable.platform1)
        movingPlatformBitmap = BitmapFactory.decodeResource(resources, R.drawable.movingplatform1)
        breakablePlatformBitmap = BitmapFactory.decodeResource(resources, R.drawable.breakableplatform1)
    }

}
