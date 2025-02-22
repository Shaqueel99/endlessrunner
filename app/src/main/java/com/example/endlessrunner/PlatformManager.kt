package com.example.endlessrunner

import kotlin.random.Random

data class Platform(
    var x: Float, var y: Float,
    val width: Float, val height: Float,
    var isBreakable: Boolean = false, // Some platforms will disappear
    var isMoving: Boolean = false,      // Some platforms will move
    var direction: Int = 1,             // Movement direction (-1 left, 1 right)
    var speed: Float = 0f               // Speed of movement
)

data class Coin(
    var x: Float,
    var y: Float,
    val size: Float = 30f // Coin size
)

data class Boost(
    var x: Float,
    var y: Float,
    val size: Float = 40f // Boost size
)

class PlatformManager(
    private val viewWidth: Int,
    private val viewHeight: Int
) {
    val platforms = mutableListOf<Platform>()
    val coins = mutableListOf<Coin>()   // Store all coins
    val boosts = mutableListOf<Boost>()

    private val platformWidth = 150f
    private val platformHeight = 20f
    private val baseMinGap = 100f
    private val baseMaxGap = 200f

    // Adjust total platform count based on score/level.
    // Level 1: calculated from score; Level 2: fixed at 5; Level 3: fixed at 3.
    private fun minPlatformCount(score: Float): Int {
        return when {
            score >= 40000 -> 3  // Level 3: spawn fewer platforms
            score >= 10000 -> 5   // Level 2: spawn fewer platforms than Level 1
            else -> {
                val count = 10 - (score / 300).toInt()
                count.coerceAtLeast(2)
            }
        }
    }

    // Increase chance for moving platforms in Level 2/3.
    private fun shouldBeMoving(score: Float): Boolean {
        // Use 50% chance if score is at least 5000 (Level 2 and 3); otherwise 30%.
        val chance = if (score >= 10000) 0.5f else 0.1f
        return score > 500 && Random.nextFloat() < chance
    }

    // Increase chance for breakable platforms in Level 2/3.
    private fun shouldBeBreakable(score: Float): Boolean {
        // Use 50% chance if score is at least 5000; otherwise 20%.
        val chance = if (score >= 10000) 0.5f else 0.0f
        return score > 800 && Random.nextFloat() < chance
    }

    private fun shouldSpawnCoin(): Boolean {
        return Random.nextFloat() < 0.1f // 10% chance to spawn a coin
    }

    private fun shouldSpawnBoost(): Boolean {
        return Random.nextFloat() < 0.03f // 3% chance to spawn a boost
    }

    // Increase moving platform speed for level 2/3.
    private fun generateSpeed(score: Float): Float {
        val base = 5f + Random.nextFloat() * 5f
        return if (score >= 20000) base * 2.0f else base
    }

    init {
        var currentY = viewHeight - 100f
        while (currentY > 0) {
            val xPos = Random.nextFloat() * (viewWidth - platformWidth)
            // At initialization score is 0 so these will be false most of the time.
            val isMoving = shouldBeMoving(0.0f)
            val isBreakable = shouldBeBreakable(0.0f)
            val speed = if (isMoving) generateSpeed(0.0f) else 0f

            val platform = Platform(xPos, currentY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed)
            platforms.add(platform)

            // Decide which bonus to spawn (if any) for this platform.
            val spawnCoin = shouldSpawnCoin()
            val spawnBoost = shouldSpawnBoost()
            if (spawnCoin && spawnBoost) {
                if (Random.nextBoolean()) {
                    val coinX = platform.x + (platform.width - 30f) / 2
                    val coinY = platform.y - 35f
                    coins.add(Coin(coinX, coinY))
                } else {
                    val boostX = platform.x + (platform.width - 40f) / 2
                    val boostY = platform.y - 60f
                    boosts.add(Boost(boostX, boostY))
                }
            } else if (spawnCoin) {
                val coinX = platform.x + (platform.width - 30f) / 2
                val coinY = platform.y - 35f
                coins.add(Coin(coinX, coinY))
            } else if (spawnBoost) {
                val boostX = platform.x + (platform.width - 40f) / 2
                val boostY = platform.y - 60f
                boosts.add(Boost(boostX, boostY))
            }

            currentY -= baseMinGap + Random.nextFloat() * (baseMaxGap - baseMinGap)
        }
    }

    fun update(offset: Float, score: Float) {
        val difficulty = 1f + score / 300f
        val minGap = baseMinGap * difficulty
        val maxGap = baseMaxGap * difficulty

        // Update moving platforms.
        platforms.forEach { platform ->
            if (platform.isMoving) {
                platform.x += platform.speed * platform.direction
                if (platform.x <= 0 || platform.x + platform.width >= viewWidth) {
                    platform.direction *= -1
                }
            }
        }

        // Scroll all platforms, coins, and boosts downward.
        platforms.forEachIndexed { i, platform ->
            platforms[i] = platform.copy(y = platform.y + offset)
        }
        coins.forEachIndexed { i, coin ->
            coins[i] = coin.copy(y = coin.y + offset)
        }
        boosts.forEachIndexed { i, boost ->
            boosts[i] = boost.copy(y = boost.y + offset)
        }

        // Remove any items that have scrolled off the screen.
        platforms.removeAll { it.y > viewHeight }
        coins.removeAll { it.y > viewHeight }
        boosts.removeAll { it.y > viewHeight }

        val requiredPlatforms = minPlatformCount(score)
        var highestY = platforms.minByOrNull { it.y }?.y ?: viewHeight.toFloat()

        // Generate new platforms until we have enough or until the highest platform is sufficiently high.
        while (platforms.size < requiredPlatforms || highestY > 150f) {
            val gap = minGap + Random.nextFloat() * (maxGap - minGap)
            val newY = (highestY - gap).coerceAtLeast(0f)
            val newX = Random.nextFloat() * (viewWidth - platformWidth)

            val isMoving = shouldBeMoving(score)
            val isBreakable = shouldBeBreakable(score)
            val speed = if (isMoving) generateSpeed(score) else 0f

            val platform = Platform(newX, newY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed)
            platforms.add(platform)

            // Decide which bonus to spawn (if any) for this new platform.
            val spawnCoin = shouldSpawnCoin()
            val spawnBoost = shouldSpawnBoost()
            if (spawnCoin && spawnBoost) {
                if (Random.nextBoolean()) {
                    val coinX = platform.x + (platform.width - 30f) / 2
                    val coinY = platform.y - 35f
                    coins.add(Coin(coinX, coinY))
                } else {
                    val boostX = platform.x + (platform.width - 40f) / 2
                    val boostY = platform.y - 50f
                    boosts.add(Boost(boostX, boostY))
                }
            } else if (spawnCoin) {
                val coinX = platform.x + (platform.width - 30f) / 2
                val coinY = platform.y - 35f
                coins.add(Coin(coinX, coinY))
            } else if (spawnBoost) {
                val boostX = platform.x + (platform.width - 40f) / 2
                val boostY = platform.y - 50f
                boosts.add(Boost(boostX, boostY))
            }

            highestY = platforms.minByOrNull { it.y }?.y ?: newY
        }
    }
}
