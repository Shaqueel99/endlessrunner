package com.example.endlessrunner

object CollisionUtils {

    // **Checks if the player is colliding with a platform**
    // This function determines whether a given square (player) is overlapping with a platform.
    // It checks for horizontal overlap between the player's bounding box and the platform's bounding box.
    // Then, it verifies if the platform's y-position is within the previous and new bottom position of the player.
    fun isCollidingWithPlatform(square: PhysicsBody, platform: Platform, prevBottom: Float, newBottom: Float): Boolean {
        val horizontalOverlap = square.x + square.width > platform.x &&
                square.x < platform.x + platform.width
        val verticalOverlap = platform.y in prevBottom..newBottom
        return horizontalOverlap && verticalOverlap
    }

    // **Checks if the player is colliding with a coin**
    // This function checks whether a player's bounding box intersects with a coin's bounding box.
    // It verifies overlap in both the horizontal and vertical axes.
    fun isCollidingWithCoin(square: PhysicsBody, coin: Coin): Boolean {
        val overlapsHorizontally = square.x + square.width > coin.x &&
                square.x < coin.x + coin.size
        val overlapsVertically = square.y + square.height > coin.y &&
                square.y < coin.y + coin.size
        return overlapsHorizontally && overlapsVertically
    }
    // **Checks if the player is colliding with a boost item**
    // This function calculates the Euclidean distance between the center points of the player and the boost.
    // If the distance is less than the sum of the boost's radius and an adjusted portion of the player's width, a collision is detected.
    fun isCollidingWithBoost(player: PhysicsBody, boost: Boost): Boolean {
        val dx = (player.x + player.width / 2) - (boost.x + boost.size / 2)
        val dy = (player.y + player.height / 2) - (boost.y + boost.size / 2)
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)

        return distance < (boost.size / 2) + (player.width / 3) // Increased hitbox size for easier collision
    }
    // **Resolves collisions between a dynamic body and a static body**
    // This function determines the axis of minimum penetration and adjusts the position of the dynamic body accordingly.
    // If horizontal penetration is smaller, it adjusts the player's x-position and inverts its horizontal velocity.
    // Otherwise, it adjusts the y-position, ensuring the player lands on top of the static object if it's a platform.
    // Vertical velocity is also inverted with restitution to simulate bouncing.
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
    // **Checks if two physics bodies are colliding using AABB (Axis-Aligned Bounding Box) collision detection**
    // This function determines if two rectangular bodies overlap by checking their boundaries along both axes.
    // If their x and y ranges intersect, a collision is detected.
    fun aabbCollision(body1: PhysicsBody, body2: PhysicsBody): Boolean {
        return body1.x < body2.x + body2.width &&
                body1.x + body1.width > body2.x &&
                body1.y < body2.y + body2.height &&
                body1.y + body1.height > body2.y
    }
}

