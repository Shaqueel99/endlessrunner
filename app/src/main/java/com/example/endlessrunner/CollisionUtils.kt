package com.example.endlessrunner

object CollisionUtils {

    // **Check collision between player and platform**
    fun isCollidingWithPlatform(square: PhysicsBody, platform: Platform, prevBottom: Float, newBottom: Float): Boolean {
        val horizontalOverlap = square.x + square.width > platform.x &&
                square.x < platform.x + platform.width
        val verticalOverlap = platform.y in prevBottom..newBottom
        return horizontalOverlap && verticalOverlap
    }

    // **Check collision between player and coin**
    fun isCollidingWithCoin(square: PhysicsBody, coin: Coin): Boolean {
        val overlapsHorizontally = square.x + square.width > coin.x &&
                square.x < coin.x + coin.size
        val overlapsVertically = square.y + square.height > coin.y &&
                square.y < coin.y + coin.size
        return overlapsHorizontally && overlapsVertically
    }


    fun resolveCollision(dynamic: PhysicsBody, static: PhysicsBody) {
        // Calculate overlap distances in both axes.
        val overlapX = if (dynamic.x < static.x)
            dynamic.right - static.x
        else
            static.right - dynamic.x

        val overlapY = if (dynamic.y < static.y)
            dynamic.bottom - static.y
        else
            static.bottom - dynamic.y

        // Resolve along the axis with the minimal penetration.
        if (overlapX < overlapY) {
            // Adjust position along X.
            if (dynamic.x < static.x) {
                dynamic.x -= overlapX
            } else {
                dynamic.x += overlapX
            }
            // Invert horizontal velocity with restitution.
            dynamic.vx = -dynamic.vx * dynamic.restitution
        } else {
            // Adjust position along Y.
            if (dynamic.y < static.y) {
                // Collision from above: place runner on top of platform.
                dynamic.y = static.y - dynamic.height
            } else {
                dynamic.y = static.bottom
            }
            // Invert vertical velocity with restitution.
            dynamic.vy = -dynamic.vy * dynamic.restitution
        }
    }

    fun aabbCollision(body1: PhysicsBody, body2: PhysicsBody): Boolean {
        return body1.x < body2.x + body2.width &&
                body1.x + body1.width > body2.x &&
                body1.y < body2.y + body2.height &&
                body1.y + body1.height > body2.y
    }
}

