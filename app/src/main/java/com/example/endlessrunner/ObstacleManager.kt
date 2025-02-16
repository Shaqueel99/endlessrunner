package com.example.endlessrunner

data class Obstacle(var x: Float, var y: Float, val width: Float, val height: Float)

class ObstacleManager(
    private var viewWidth: Int,
    private var viewHeight: Int,
    private var groundHeight: Float,
    private var runnerSize: Float,
    private var groundSpeed: Float
) {
    val obstacles = mutableListOf<Obstacle>()
    private var spawnCounter = 0
    private val spawnInterval = 60  // spawn an obstacle every 60 frames

    fun update() {
        // Move obstacles left.
        obstacles.forEach { it.x -= groundSpeed }
        // Remove obstacles that have moved off screen.
        obstacles.removeAll { it.x + it.width < 0 }

        // Spawn new obstacle after the spawn interval.
        spawnCounter++
        if (spawnCounter >= spawnInterval) {
            spawnObstacle()
            spawnCounter = 0
        }
    }

    private fun spawnObstacle() {
        // Fixed obstacle size; adjust as needed.
        val obstacleWidth = 50f
        val obstacleHeight = 100f
        // Position obstacle so that its bottom aligns with the ground.
        val yPos = viewHeight - groundHeight - obstacleHeight
        obstacles.add(Obstacle(viewWidth.toFloat(), yPos, obstacleWidth, obstacleHeight))
    }

    fun checkCollision(runnerX: Float, runnerY: Float, runnerSize: Float): Boolean {
        for (obstacle in obstacles) {
            if (rectIntersect(runnerX, runnerY, runnerSize, runnerSize,
                    obstacle.x, obstacle.y, obstacle.width, obstacle.height)) {
                return true
            }
        }
        return false
    }

    private fun rectIntersect(
        x1: Float, y1: Float, w1: Float, h1: Float,
        x2: Float, y2: Float, w2: Float, h2: Float
    ): Boolean {
        return x1 < x2 + w2 && x1 + w1 > x2 &&
                y1 < y2 + h2 && y1 + h1 > y2
    }

    // Optional: update parameters if view dimensions or speed changes.
    fun updateDimensions(viewWidth: Int, viewHeight: Int, groundHeight: Float, runnerSize: Float, groundSpeed: Float) {
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
        this.groundHeight = groundHeight
        this.runnerSize = runnerSize
        this.groundSpeed = groundSpeed
    }
}
