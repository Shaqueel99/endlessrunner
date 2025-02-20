package com.example.endlessrunner

import kotlin.random.Random

data class Platform(
    var x: Float, var y: Float,
    val width: Float, val height: Float,
    var isBreakable: Boolean = false, // Some platforms will disappear
    var isMoving: Boolean = false, // Some platforms will move
    var direction: Int = 1, // Movement direction (-1 left, 1 right)
    var speed: Float = 0f // Speed of movement
)
data class Coin(
    var x: Float,
    var y: Float,
    val size: Float = 30f // Coin size
)

class PlatformManager(
    private val viewWidth: Int,
    private val viewHeight: Int
) {
    val platforms = mutableListOf<Platform>()
    val coins = mutableListOf<Coin>() // Store all coins

    private val platformWidth = 150f
    private val platformHeight = 20f
    private val baseMinGap = 100f
    private val baseMaxGap = 200f

    private fun difficultyMultiplier(score: Float): Float {
        return 1f + score / 300f
    }

    private fun minPlatformCount(score: Float): Int {
        val count = 10 - (score / 300).toInt()
        return count.coerceAtLeast(2)
    }

    private fun shouldBeMoving(score: Float): Boolean {
        return score > 500 && Random.nextFloat() < 0.3f
    }

    private fun shouldBeBreakable(score: Float): Boolean {
        return score > 800 && Random.nextFloat() < 0.2f
    }

    private fun shouldSpawnCoin(): Boolean {
        return Random.nextFloat() < 0.1f // 10% chance to spawn a coin
    }

    private fun generateSpeed(): Float {
        return 5f + Random.nextFloat() * 5f
    }

    init {
        var currentY = viewHeight - 100f
        while (currentY > 0) {
            val xPos = Random.nextFloat() * (viewWidth - platformWidth)
            val isMoving = shouldBeMoving(0.0f)
            val isBreakable = shouldBeBreakable(0.0f)
            val speed = if (isMoving) generateSpeed() else 0f

            val platform = Platform(xPos, currentY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed)
            platforms.add(platform)

            // **Spawn Coin on Top of the Platform (10% Chance)**
            if (shouldSpawnCoin()) {
                val coinX = platform.x + (platform.width - 30f) / 2 // Center the coin
                val coinY = platform.y - 35f // Place coin slightly above platform
                coins.add(Coin(coinX, coinY))
            }

            currentY -= baseMinGap + Random.nextFloat() * (baseMaxGap - baseMinGap)
        }
    }

    fun update(offset: Float, score: Float) {
        val difficulty = difficultyMultiplier(score)
        val minGap = baseMinGap * difficulty
        val maxGap = baseMaxGap * difficulty

        platforms.forEach { platform ->
            if (platform.isMoving) {
                platform.x += platform.speed * platform.direction
                if (platform.x <= 0 || platform.x + platform.width >= viewWidth) {
                    platform.direction *= -1
                }
            }
        }

        platforms.forEachIndexed { i, platform ->
            platforms[i] = platform.copy(y = platform.y + offset)
        }

        // **Move Coins Downward Too**
        coins.forEachIndexed { i, coin ->
            coins[i] = coin.copy(y = coin.y + offset)
        }

        platforms.removeAll { it.y > viewHeight }
        coins.removeAll { it.y > viewHeight } // Remove off-screen coins

        val requiredPlatforms = minPlatformCount(score)
        var highestY = platforms.minByOrNull { it.y }?.y ?: viewHeight.toFloat()

        while (platforms.size < requiredPlatforms || highestY > 150f) {
            val gap = minGap + Random.nextFloat() * (maxGap - minGap)
            val newY = (highestY - gap).coerceAtLeast(0f)
            val newX = Random.nextFloat() * (viewWidth - platformWidth)

            val isMoving = shouldBeMoving(score)
            val isBreakable = shouldBeBreakable(score)
            val speed = if (isMoving) generateSpeed() else 0f

            val platform = Platform(newX, newY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed)
            platforms.add(platform)

            // **Spawn Coin on Top of the New Platform (10% Chance)**
            if (shouldSpawnCoin()) {
                val coinX = platform.x + (platform.width - 30f) / 2
                val coinY = platform.y - 35f
                coins.add(Coin(coinX, coinY))
            }

            highestY = platforms.minByOrNull { it.y }?.y ?: newY
        }
    }
}
