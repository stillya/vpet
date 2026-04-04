package dev.stillya.vpet.game

import kotlin.math.floor

data class World(
	val registry: EntityRegistry = EntityRegistry(),
	val player: EntityID = registry.create(),
	val score: Int = 0
) {
	val transform: Transform get() = playerTransform()
	val velocity: Velocity get() = playerVelocity()
	val isOnGround: Boolean get() = playerPhysicsState().isOnGround
	val sprite: SpriteState get() = playerSprite()
	val phase: GamePhase get() = playerPhaseState().phase
	val displayLine: Int get() = floor(transform.y).toInt()
}

fun World.playerTransform(): Transform =
	registry.get<Transform>(player) ?: Transform()

fun World.playerVelocity(): Velocity =
	registry.get<Velocity>(player) ?: Velocity()

fun World.playerSprite(): SpriteState =
	registry.get<SpriteState>(player) ?: SpriteState()

fun World.playerPhysicsState(): PhysicsState =
	registry.get<PhysicsState>(player) ?: PhysicsState()

fun World.playerPhaseState(): PhaseState =
	registry.get<PhaseState>(player) ?: PhaseState()

enum class GamePhase { ENTRANCE, PLAYING }

data class InputState(
	val moveDirection: Int = 0,
	// TODO: Make input state more robust, i wanna have like a union for buttons
	val jumpJustPressed: Boolean = false
)

object Physics {
	const val GRAVITY = 70.0f
	const val MAX_FALL_SPEED = 30.0f
	const val VELOCITY_EPSILON = 0.1f
	const val FRAME_ADVANCE_INTERVAL = 0.096f
}
