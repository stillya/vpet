package dev.stillya.vpet.game

import kotlin.math.floor

data class GameState(
	val colX: Float = 0f,
	val lineY: Float = 0f,
	val velocityX: Float = 0f,
	val velocityY: Float = 0f,
	val isOnGround: Boolean = true,
	val facingLeft: Boolean = false,
	val animState: GameAnimationState = GameAnimationState.IDLE,
	val frameIndex: Int = 0,
	val frameTimer: Float = 0f
) {
	val displayLine: Int get() = floor(lineY).toInt()
	val catLeft: Int get() = floor(colX).toInt()
	val catRight: Int get() = catLeft + Physics.CAT_WIDTH - 1
}

data class InputState(
	val moveDirection: Int = 0,
	val jumpJustPressed: Boolean = false
)

object Physics {
	const val CAT_WIDTH = 2
	const val WALK_SPEED = 9.0f
	const val GRAVITY = 75.0f
	const val JUMP_VELOCITY = -15.6f
	const val MAX_FALL_SPEED = 30.0f
	const val GROUND_DAMPING = 18.0f
	const val AIR_DAMPING = 3.0f
	const val AIR_CONTROL = 0.3f
	const val VELOCITY_EPSILON = 0.1f
	const val FRAME_ADVANCE_INTERVAL = 0.096f
}
