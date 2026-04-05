package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction
import dev.stillya.vpet.game.ecs.GamePhase
import dev.stillya.vpet.game.ecs.Spatial
import dev.stillya.vpet.game.ecs.components.SpriteState
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.ecs.components.Velocity
import dev.stillya.vpet.game.input.InputState

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
	val animation: Animation,
	val direction: Direction,
	val phase: GamePhase
)
