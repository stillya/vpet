package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Direction

data class GameFrame(
	val world: World,
	val animation: dev.stillya.vpet.animation.Animation,
	val bounds: IntRange
)

object WorldUpdate {

	fun tick(
		world: World,
		input: InputState,
		dt: Float,
		character: Character,
		tileMap: VirtualTileMap,
		visibleRange: IntRange
	): GameFrame {
		val ctx = TickContext(
			transform = world.transform,
			velocity = world.velocity,
			isOnGround = world.isOnGround,
			sprite = world.sprite,
			phase = world.phase
		)
		val intent = character.update(input, ctx, dt)

		val physics = PhysicsBody(character.collider())
		val result = physics.moveAndSlide(
			ctx.transform, intent.velocity, intent.isGrounded, tileMap, visibleRange, dt
		)

		val sprite = advanceFrame(intent.animation.name, intent.direction, world.sprite, dt)

		val newWorld = World(
			transform = result.transform,
			velocity = result.velocity,
			isOnGround = result.isOnGround,
			sprite = sprite,
			phase = intent.phase
		)
		return GameFrame(newWorld, intent.animation, physics.boundsAt(result.transform))
	}

	private fun advanceFrame(tag: String, direction: Direction, prev: SpriteState, dt: Float): SpriteState {
		val tagChanged = tag != prev.tag
		val frameIndex = if (tagChanged) 0 else prev.frameIndex
		val frameTimer = if (tagChanged) 0f else prev.frameTimer
		val newTimer = frameTimer + dt
		return if (newTimer >= Physics.FRAME_ADVANCE_INTERVAL) {
			SpriteState(tag, frameIndex + 1, newTimer - Physics.FRAME_ADVANCE_INTERVAL, direction)
		} else {
			SpriteState(tag, frameIndex, newTimer, direction)
		}
	}
}
