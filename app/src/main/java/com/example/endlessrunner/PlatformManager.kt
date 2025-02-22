package com.example.endlessrunner

import kotlin.random.Random

// Added 'spawnLevel' to record which image set to use.
data class Platform(
    var x: Float, var y: Float,
    val width: Float, val height: Float,
    var isBreakable: Boolean = false,
    var isMoving: Boolean = false,
    var direction: Int = 1,
    var speed: Float = 0f,
    val spawnLevel: Int = 1  // 1 = level1 images, 2 = level2 images, 3 = level3 images
)

data class Coin(
    var x: Float,
    var y: Float,
    val size: Float = 30f
)

data class Boost(
    var x: Float,
    var y: Float,
    val size: Float = 40f
)

class PlatformManager(
    private val viewWidth: Int,
    private val viewHeight: Int
) {
    val platforms = mutableListOf<Platform>()
    val coins = mutableListOf<Coin>()
    val boosts = mutableListOf<Boost>()

    private val platformWidth = 150f
    private val platformHeight = 20f
    private val baseMinGap = 100f
    private val baseMaxGap = 200f

    private fun difficultyMultiplier(score: Float): Float {
        return 1f + score / 300f
    }

    // Fewer platforms spawn as levels increase.
    private fun minPlatformCount(score: Float): Int {
        return when {
            score >= 40000 -> 3  // Level 3
            score >= 10000 -> 5   // Level 2
            else -> {
                val count = 10 - (score / 300).toInt()
                count.coerceAtLeast(2)
            }
        }
    }

    private fun shouldBeMoving(score: Float): Boolean {
        val chance =
            if (score >= 40000) { 1.0f }
        else if(score >= 10000) { 0.5f }
        else { 0.3f }

        return score > 500 && Random.nextFloat() < chance
    }

    private fun shouldBeBreakable(score: Float): Boolean {
        val chance =
            if (score >= 40000) { 0.7f }
            else if(score >= 15000) { 0.3f }
            else { 0.1f }
        return score > 800 && Random.nextFloat() < chance
    }

    private fun shouldSpawnCoin(): Boolean {
        return Random.nextFloat() < 0.1f
    }

    private fun shouldSpawnBoost(): Boolean {
        return Random.nextFloat() < 0.03f
    }

    private fun generateSpeed(score: Float): Float {
        val base = 5f + Random.nextFloat() * 5f
        return if (score >= 40000) base * 1.5f else base
    }

    init {
        var currentY = viewHeight - 100f
        // Initial platforms spawn at level 1.
        while (currentY > 0) {
            val xPos = Random.nextFloat() * (viewWidth - platformWidth)
            val isMoving = shouldBeMoving(0.0f)
            val isBreakable = shouldBeBreakable(0.0f)
            val speed = if (isMoving) generateSpeed(0.0f) else 0f

            val platform = Platform(xPos, currentY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed, spawnLevel = 1)
            platforms.add(platform)

            // Spawn coin/boost exclusively.
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
        val difficulty = difficultyMultiplier(score)
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

        platforms.forEachIndexed { i, platform ->
            platforms[i] = platform.copy(y = platform.y + offset)
        }
        coins.forEachIndexed { i, coin ->
            coins[i] = coin.copy(y = coin.y + offset)
        }
        boosts.forEachIndexed { i, boost ->
            boosts[i] = boost.copy(y = boost.y + offset)
        }

        platforms.removeAll { it.y > viewHeight }
        coins.removeAll { it.y > viewHeight }
        boosts.removeAll { it.y > viewHeight }

        val requiredPlatforms = minPlatformCount(score)
        var highestY = platforms.minByOrNull { it.y }?.y ?: viewHeight.toFloat()

        // New platforms are spawned with a spawnLevel that depends on the current score.
        while (platforms.size < requiredPlatforms || highestY > 150f) {
            val gap = minGap + Random.nextFloat() * (maxGap - minGap)
            val newY = (highestY - gap).coerceAtLeast(0f)
            val newX = Random.nextFloat() * (viewWidth - platformWidth)
            val isMoving = shouldBeMoving(score)
            val isBreakable = shouldBeBreakable(score)
            val speed = if (isMoving) generateSpeed(score) else 0f

            val spawnLevel = when {
                score < 10000f -> 1
                score >= 10000f && score < 40000f -> 2
                else -> 3
            }

            val platform = Platform(newX, newY, platformWidth, platformHeight, isBreakable, isMoving, 1, speed, spawnLevel = spawnLevel)
            platforms.add(platform)

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
