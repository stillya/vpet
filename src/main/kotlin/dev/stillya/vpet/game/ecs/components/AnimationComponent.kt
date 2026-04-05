package dev.stillya.vpet.game.ecs.components

data class AnimationComponent(
    val resourceId: String,
    val currentFrame: Int = 0,
    val elapsed: Float = 0f
)
