package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation

interface Character {
	fun update(input: InputState, ctx: TickContext, dt: Float): CharacterFrame

	fun getAnimation(tag: String): Animation?
	fun isLooping(tag: String): Boolean

	fun debugBounds(transform: Transform): IntRange
}

data class TickContext(
	val transform: Transform,
	val velocity: Velocity,
	val isOnGround: Boolean,
	val sprite: SpriteState,
	val phase: GamePhase,
	val tileMap: VirtualTileMap,
	val visibleRange: IntRange
)

data class CharacterFrame(
	val transform: Transform,
	val velocity: Velocity,
	val isOnGround: Boolean,
	val sprite: SpriteState,
	val phase: GamePhase
)
