package dev.stillya.vpet.game

import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.Direction

data class GameFrame(
	val world: World,
	val animation: Animation,
	val bounds: IntRange
)

object WorldUpdate {

	private val spatialGrid = SpatialGrid()

	fun tick(
		world: World,
		input: InputState,
		dt: Float,
		character: Character,
		tileMap: VirtualTileMap,
		visibleRange: IntRange
	): GameFrame {
		val reg = world.registry
		val playerId = world.player

		val transform = reg.get<Transform>(playerId) ?: Transform()
		val velocity = reg.get<Velocity>(playerId) ?: Velocity()
		val physState = reg.get<PhysicsState>(playerId) ?: PhysicsState()
		val sprite = reg.get<SpriteState>(playerId) ?: SpriteState()
		val phaseState = reg.get<PhaseState>(playerId) ?: PhaseState()

		val ctx = TickContext(
			transform = transform,
			velocity = velocity,
			isOnGround = physState.isOnGround,
			sprite = sprite,
			phase = phaseState.phase
		)
		val intent = character.update(input, ctx, dt)

		val physics = PhysicsBody(character.collider())
		val result = physics.moveAndSlide(
			transform, intent.velocity, tileMap, visibleRange, dt
		)

		val newSprite = advanceFrame(intent.animation.name, intent.direction, sprite, dt)

		reg.add(playerId, result.transform)
		reg.add(playerId, result.velocity)
		reg.add(playerId, PhysicsState(result.isOnGround))
		reg.add(playerId, newSprite)
		reg.add(playerId, PhaseState(intent.phase))

		spatialGrid.rebuild(reg)
		val collected = CollisionSystem.detectCollections(reg, playerId, spatialGrid)
		var scoreGain = 0
		for (id in collected) {
			val collectible = reg.get<Collectible>(id)
			if (collectible != null) scoreGain += collectible.value
			reg.markForRemoval(id)
		}
		reg.flushRemovals()

		val newWorld = world.copy(score = world.score + scoreGain)
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
