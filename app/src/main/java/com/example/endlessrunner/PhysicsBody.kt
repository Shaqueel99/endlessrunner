package com.example.endlessrunner
/**
 * Represents a physics body with position, dimensions, velocity, and physical properties.
 *
 * @property x The x-coordinate of the body.
 * @property y The y-coordinate of the body.
 * @property width The width of the body.
 * @property height The height of the body.
 * @property vx The horizontal velocity.
 * @property vy The vertical velocity.
 * @property mass The mass of the body.
 * @property restitution The bounce factor (how much the body bounces on collision).
 */
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
