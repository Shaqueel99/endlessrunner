package com.example.endlessrunner

data class Platform(val x: Float, val y: Float, val width: Float, val height: Float)

class PlatformManager(
    private var viewWidth: Int,
    private var viewHeight: Int,
    private var groundHeight: Float,
    private var groundSpeed: Float
) {
    // List of platforms currently in play.
    val platforms = mutableListOf<Platform>()
    private var spawnCounter = 0
    private val spawnInterval = 10  // Adjust this value to control spawn frequency.

    // Constants to control spacing.
    private val minHorizontalGap = 150f
    private val maxHorizontalGap = 500f
    private val platformHeight = 20f

    init {
        // Create an initial ground platform that spans the bottom.
        val groundY = viewHeight - groundHeight - platformHeight
        platforms.add(Platform(0f, groundY, viewWidth.toFloat(), platformHeight))
    }

    fun update(currentGroundSpeed: Float) {
        // Move all platforms to the left by the current ground speed.
        for (i in platforms.indices) {
            val p = platforms[i]
            platforms[i] = p.copy(x = p.x - currentGroundSpeed)
        }
        // Remove platforms that have scrolled off screen.
        platforms.removeAll { it.x + it.width < 0 }

        // Increment counter and spawn a new platform at intervals.
        spawnCounter++
        if (spawnCounter >= spawnInterval) {
            spawnPlatform()
            spawnCounter = 0
        }
    }

    private fun spawnPlatform() {
        // Define vertical bounds so platforms appear within a reachable range:
        // Platforms will be spawned between 400px and 50px above the ground.
        val minY = viewHeight - groundHeight - 400f
        val maxY = viewHeight - groundHeight - 50f

        // Find the rightmost platform's right edge.
        val lastPlatform = platforms.maxByOrNull { it.x + it.width }
        val startX = lastPlatform?.x?.plus(lastPlatform.width) ?: viewWidth.toFloat()

        // Random horizontal gap.
        val gap = minHorizontalGap + (Math.random().toFloat() * (maxHorizontalGap - minHorizontalGap))
        val xPos = startX + gap

        // Random vertical position within the new, narrower bounds.
        val yPos = minY + (Math.random().toFloat() * (maxY - minY))

        // Random platform width between 500 and 700.
        val platformWidth = 500f + (Math.random().toFloat() * 200f)

        platforms.add(Platform(xPos, yPos, platformWidth, platformHeight))
    }
}
