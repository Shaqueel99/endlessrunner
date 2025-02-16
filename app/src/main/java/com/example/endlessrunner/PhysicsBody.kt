package com.example.endlessrunner

data class PhysicsBody(
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val mass: Float = 1f,          // For dynamic objects; platforms can be treated as immovable.
    val restitution: Float = 0.0f    // Bounce factor.
) {
    val right: Float get() = x + width
    val bottom: Float get() = y + height
}
