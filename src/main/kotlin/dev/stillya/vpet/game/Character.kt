package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction

interface Character : Spatial {
	fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent
}

data class TickContext(
	val transform: Transform,
	val velocity: Velocity,
	val isOnGround: Boolean,
	val sprite: SpriteState,
	val phase: GamePhase
)

data class CharacterIntent(
	val velocity: Velocity,
	val isGrounded: Boolean,
	val animation: Animation,
	val direction: Direction,
	val phase: GamePhase
)
