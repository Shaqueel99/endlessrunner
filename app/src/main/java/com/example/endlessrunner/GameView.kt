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
import android.graphics.RectF
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

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle), SensorEventListener {


    // Paints
    private val paint = Paint()
    private val textPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
        textSize = 60f
        // White text with a black shadow
        setShadowLayer(8f, 0f, 0f, Color.BLACK)
    }
    val tintedPaint = Paint().apply {
        colorFilter = PorterDuffColorFilter(Color.argb(100, 0, 0, 0), PorterDuff.Mode.DARKEN)
    }

    // Bitmaps & Backgrounds
    private var backgroundBitmap1: Bitmap? = null
    private var backgroundBitmap2: Bitmap? = null
    private var backgroundBitmap3: Bitmap? = null
    private var backgroundBitmap4: Bitmap? = null
    private var backgroundBitmap5: Bitmap? = null
    private var currentBackground: Bitmap? = null
    private var platformBitmap: Bitmap? = null
    private var movingPlatformBitmap: Bitmap? = null
    private var breakablePlatformBitmap: Bitmap? = null

    private var platformBitmap2: Bitmap? = null
    private var movingPlatformBitmap2: Bitmap? = null
    private var breakablePlatformBitmap2: Bitmap? = null


    private var platformBitmap3: Bitmap? = null
    private var movingPlatformBitmap3: Bitmap? = null
    private var breakablePlatformBitmap3: Bitmap? = null


    private var backgroundOffset = 0f
    private var transitionBackgroundAdded = false
    private var transitionBackgroundAddedLevel3 = false     // for level 3 (bg4 then bg5)

    // Level & Score
    private var currentLevel = 1
    private var score: Float = 0f

    // Player & Game Entities
    private lateinit var squareBody: PhysicsBody
    private var coinscollected = 0
    private val platformRect = RectF()
    private var platformManager: PlatformManager? = null

    // Physics parameters
    private val gravity = 2f
    private val jumpVelocity = -60f
    private var scrollThreshold = 100f // Vertical scroll threshold

    // Input
    private var tiltControl = 0f
    private var touchControl = 0f
    private val boostJumpVelocity = -100f
    private var tiltSensitivity = 4.0f

    // Skins & Profile
    private var equippedSkinFromFirebase: String = "default"
    private var profileSkinBitmap: Bitmap? = null

    // Sensors
    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var accelerometer: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val backgroundQueue = mutableListOf<ScrollingBackground>()

    // Game loop
    private var gameJob: Job? = null
    // This holds one background image and the vertical position where it begins.
    data class ScrollingBackground(
        val bitmap: Bitmap,
        var startY: Float
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        loadEquippedSkinFromFirebase()
        startGameLoop()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        gameJob?.cancel()
        sensorManager.unregisterListener(this)
    }

    // ----------------- Initialization Helpers -----------------

    private fun loadEquippedSkinFromFirebase() {
        val sharedPrefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPrefs.getString("username", null)
        if (username != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("users").whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { documents ->
                    val document = documents.firstOrNull()  // Get the first matching document
                    if (document != null) {
                        equippedSkinFromFirebase = document.getString("equippedSkin") ?: "default"
                        if (equippedSkinFromFirebase == "profile") {
                            val profileImageUrl = document.getString("profileImagePath") ?: ""
                            if (profileImageUrl.isNotEmpty()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val bitmap = Glide.with(context)
                                            .asBitmap()
                                            .load(profileImageUrl)
                                            .submit()
                                            .get()
                                        profileSkinBitmap = Bitmap.createScaledBitmap(
                                            bitmap!!,
                                            squareBody.width.toInt(),
                                            squareBody.width.toInt(),
                                            false
                                        )
                                        withContext(Dispatchers.Main) { invalidate() }
                                    } catch (e: Exception) {
                                        Log.e("GameView", "Failed to load profile image: ${e.message}")
                                    }
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load skin: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun loadImages(w: Int, h: Int) {
        squareBody = PhysicsBody((w - 80f) / 2f, h - 180f, 80f, 80f, 0f, jumpVelocity)
        scrollThreshold = h / 3f
        platformManager = PlatformManager(w, h)

        backgroundBitmap1 = BitmapFactory.decodeResource(resources, R.drawable.bg1)
        backgroundBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.bg2)
        backgroundBitmap3 = BitmapFactory.decodeResource(resources, R.drawable.bg3)
        backgroundBitmap4 = BitmapFactory.decodeResource(resources, R.drawable.bg4)
        backgroundBitmap5 = BitmapFactory.decodeResource(resources, R.drawable.bg5)
        backgroundQueue.add(ScrollingBackground(backgroundBitmap1!!, 0f))

        currentBackground = backgroundBitmap1

        platformBitmap = BitmapFactory.decodeResource(resources, R.drawable.platform1)
        movingPlatformBitmap = BitmapFactory.decodeResource(resources, R.drawable.movingplatform1)
        breakablePlatformBitmap = BitmapFactory.decodeResource(resources, R.drawable.breakableplatform1)

        platformBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.platform2)
        movingPlatformBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.movingplatform2)
        breakablePlatformBitmap2 = BitmapFactory.decodeResource(resources, R.drawable.breakableplatform2)


        platformBitmap3 = BitmapFactory.decodeResource(resources, R.drawable.platform3)
        movingPlatformBitmap3 = BitmapFactory.decodeResource(resources, R.drawable.movingplatform3)
        breakablePlatformBitmap3 = BitmapFactory.decodeResource(resources, R.drawable.breakableplatform3)
    }
    private fun manageBackgroundQueue() {
        if (backgroundQueue.isEmpty()) return

        val viewHeight = height.toFloat()

        // Remove any backgrounds that are fully off the bottom of the screen.
        backgroundQueue.removeAll { bg ->
            // When drawn, the top edge is at: bg.startY + backgroundOffset.
            (bg.startY + backgroundOffset) > viewHeight
        }

        // Ensure there are at least 2 backgrounds.
        // Since we're scrolling downward, add a new background at the top.
        if (backgroundQueue.size < 2) {
            val firstBg = backgroundQueue.firstOrNull() ?: return
            // Position the new background exactly one background height above the first.
            val newStartY = firstBg.startY - firstBg.bitmap.height

            // Choose the appropriate background based on current level:
            // Level 1: always use bg1.
            // Level 2: if bg2 hasn't been added yet, add it once; then use bg3.
            // Level 3: if bg4 hasn't been added yet, add it once; then use bg5.
            val nextBitmap = when {
                currentLevel == 1 -> backgroundBitmap1
                currentLevel == 2 -> {
                    if (!transitionBackgroundAdded) {
                        transitionBackgroundAdded = true
                        backgroundBitmap2
                    } else {
                        backgroundBitmap3
                    }
                }
                currentLevel == 3 -> {
                    if (!transitionBackgroundAddedLevel3) {
                        transitionBackgroundAddedLevel3 = true
                        backgroundBitmap4
                    } else {
                        backgroundBitmap5
                    }
                }
                else -> backgroundBitmap5  // Fallback if needed.
            }

            nextBitmap?.let {
                backgroundQueue.add(0, ScrollingBackground(it, newStartY))
            }
        }
    }




    // ----------------- Game Loop -----------------

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

        updatePlayerPosition()
        updateBackgroundOffset()  // keep incrementing backgroundOffset
        manageBackgroundQueue()    // add/remove backgrounds as needed
        checkLevelTransition()
        platformManager?.update(currentOffset, score)
        checkScreenWrap()
        checkPlatformCollisions()
        checkCoinCollisions()
        checkBoostCollisions()
        checkGameOver()
    }

    // ----------------- Update Helpers -----------------

    private var currentOffset = 0f

    private fun updatePlayerPosition() {
        squareBody.vx = tiltControl * tiltSensitivity + touchControl
        squareBody.x += squareBody.vx

        val prevBottom = squareBody.y + squareBody.height
        squareBody.vy += gravity
        squareBody.y += squareBody.vy
        val newBottom = squareBody.y + squareBody.height

        if (squareBody.y < scrollThreshold) {
            currentOffset = scrollThreshold - squareBody.y
            squareBody.y = scrollThreshold
            score += currentOffset
        }
    }

    private fun updateBackgroundOffset()
    {
        backgroundOffset += currentOffset * 0.5f
    }

    private fun drawBackgrounds(canvas: Canvas) {
        // Draw all backgrounds in the queue from earliest to latest
        for (bg in backgroundQueue) {
            val drawY = bg.startY + backgroundOffset
            canvas.drawBitmap(bg.bitmap, 0f, drawY, tintedPaint)
        }
    }

    private fun checkLevelTransition() {
        if (score >= 10000 && currentLevel == 1) {
            currentLevel = 2
            transitionBackgroundAdded = false  // reset for level 2 transition (bg2 then bg3)
        }
        else if (score >= 40000 && currentLevel == 2) {
            currentLevel = 3
            transitionBackgroundAddedLevel3 = false  // reset for level 3 transition (bg4 then bg5)
        }
    }

    private fun drawPlatforms(canvas: Canvas) {
        platformManager?.platforms?.forEach { platform ->
            val bmp: Bitmap? = when (platform.spawnLevel) {
                1 -> when {
                    platform.isBreakable -> breakablePlatformBitmap
                    platform.isMoving -> movingPlatformBitmap
                    else -> platformBitmap
                }
                2 -> when {
                    platform.isBreakable -> breakablePlatformBitmap2
                    platform.isMoving -> movingPlatformBitmap2
                    else -> platformBitmap2
                }
                3 -> when {
                    platform.isBreakable -> breakablePlatformBitmap3
                    platform.isMoving -> movingPlatformBitmap3
                    else -> platformBitmap3
                }
                else -> platformBitmap
            }
            bmp?.let {
                platformRect.set(
                    platform.x,
                    platform.y,
                    platform.x + platform.width,
                    platform.y + platform.height
                )
                canvas.drawBitmap(it, null, platformRect, null)
            }
        }
    }


    private fun checkScreenWrap() {
        if (squareBody.x > width) {
            squareBody.x = 0f
        } else if (squareBody.x + squareBody.width < 0) {
            squareBody.x = width - squareBody.width
        }
    }

    private fun checkPlatformCollisions() {
        if (squareBody.vy > 0) {
            val prevBottom = squareBody.y + squareBody.height - squareBody.vy
            val newBottom = squareBody.y + squareBody.height
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
    }

    private fun checkCoinCollisions() {
        platformManager?.coins?.removeIf { coin ->
            if (CollisionUtils.isCollidingWithCoin(squareBody, coin)) {
                coinscollected += 1
                true
            } else {
                false
            }
        }
    }

    private fun checkBoostCollisions() {
        platformManager?.boosts?.removeIf { boost ->
            if (CollisionUtils.isCollidingWithBoost(squareBody, boost)) {
                squareBody.vy = boostJumpVelocity
                true
            } else {
                false
            }
        }
    }

    private fun checkGameOver() {
        if (squareBody.y > height) {
            val sharedPrefs = context.getSharedPreferences("user_prefs", MODE_PRIVATE)
            val username = sharedPrefs.getString("username", null)

            if (username != null) {
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users").whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener { documents ->
                        val document = documents.firstOrNull()  // Get the first matching document
                        if (document != null) {
                        val previousCoins = document.getLong("coinsCollected") ?: 0L
                        val newTotal = previousCoins + coinscollected
                            // update coins
                            firestore.collection("users")
                                .whereEqualTo("username", username) // Find the correct document
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        firestore.collection("users").document(document.id)
                                            .update("coinsCollected", newTotal)
                                            .addOnSuccessListener {
                                                Log.d(
                                                    "Firestore",
                                                    "Coins updated successfully: $newTotal"
                                                )
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Failed to update coins: ${e.message}")
                                    Toast.makeText(context, "Failed to update coins.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                 }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update coins: ${e.message}")
                        Toast.makeText(context, "Failed to update coins.", Toast.LENGTH_SHORT).show()
                    }
            }
            gameJob?.cancel()
            Toast.makeText(context, "Game Over! Score: ${score.toInt()}", Toast.LENGTH_LONG).show()
            val intent = Intent(context, GameOverActivity::class.java)
            intent.putExtra("score", score.toInt())
            context.startActivity(intent)
        }
    }

    // ----------------- Drawing Helpers -----------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBackgrounds(canvas) // draws the entire queue in back
        drawPlatforms(canvas)
        drawCoins(canvas)
        drawBoosts(canvas)
        drawPlayer(canvas)
        drawUI(canvas)
    }





    private fun drawCoins(canvas: Canvas) {
        paint.color = Color.YELLOW
        platformManager?.coins?.forEach { coin ->
            canvas.drawCircle(
                coin.x + coin.size / 2,
                coin.y + coin.size / 2,
                coin.size / 2,
                paint
            )
        }
    }

    private fun drawBoosts(canvas: Canvas) {
        paint.color = Color.CYAN
        platformManager?.boosts?.forEach { boost ->
            canvas.drawOval(
                boost.x,
                boost.y,
                boost.x + boost.size,
                boost.y + boost.size,
                paint
            )
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val playerBitmap = when (equippedSkinFromFirebase) {
            "red" -> createColorSquare(Color.RED, squareBody.width.toInt())
            "green" -> createColorSquare(Color.GREEN, squareBody.width.toInt())
            "profile" -> profileSkinBitmap ?: createColorSquare(Color.MAGENTA, squareBody.width.toInt())
            else -> createColorSquare(Color.BLUE, squareBody.width.toInt())
        }
        canvas.drawBitmap(playerBitmap, squareBody.x, squareBody.y, paint)
    }

    private fun createColorSquare(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint().apply { this.color = color }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), p)
        return bmp

    }

    private fun drawUI(canvas: Canvas) {
        val levelText = "Level: $currentLevel"
        val scoreText = "${score.toInt()}m"
        val levelX = (width - textPaint.measureText(levelText)) / 2
        val scoreX = (width - textPaint.measureText(scoreText)) / 2
        canvas.drawText(levelText, levelX, 170f, textPaint)
        canvas.drawText(scoreText, scoreX, 240f, textPaint)
        canvas.drawText("Coins: $coinscollected", 50f, 170f, textPaint)
    }

    // ----------------- Input Handling -----------------

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            tiltControl = -event.values[0] * 2.0f
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            val centerX = width / 2f
            when (it.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    touchControl = (it.x - centerX) / 20f
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
        loadImages(w, h)
    }
}
