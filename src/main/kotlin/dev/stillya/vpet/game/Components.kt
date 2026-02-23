package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Direction

data class Transform(val x: Float = 0f, val y: Float = 0f)

data class Velocity(val x: Float = 0f, val y: Float = 0f)

data class SpriteState(
	val tag: String = "Idle",
	val frameIndex: Int = 0,
	val frameTimer: Float = 0f,
	val direction: Direction = Direction.RIGHT
)
