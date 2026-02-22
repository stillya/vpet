package dev.stillya.vpet.game

import kotlin.math.floor

data class World(
	val transform: Transform = Transform(),
	val velocity: Velocity = Velocity(),
	val isOnGround: Boolean = true,
	val sprite: SpriteState = SpriteState(),
	val phase: GamePhase = GamePhase.PLAYING
) {
	val displayLine: Int get() = floor(transform.y).toInt()
}

enum class GamePhase { ENTRANCE, PLAYING }

data class InputState(
	val moveDirection: Int = 0,
	// TODO: Make input state more robust, i wanna have like a union for buttons
	val jumpJustPressed: Boolean = false
)

object Physics {
	const val GRAVITY = 75.0f
	const val MAX_FALL_SPEED = 30.0f
	const val VELOCITY_EPSILON = 0.1f
	const val FRAME_ADVANCE_INTERVAL = 0.096f
}
