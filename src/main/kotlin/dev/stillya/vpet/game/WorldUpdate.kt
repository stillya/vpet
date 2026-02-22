package dev.stillya.vpet.game

object WorldUpdate {

	fun tick(
		world: World,
		input: InputState,
		dt: Float,
		character: Character,
		tileMap: VirtualTileMap,
		visibleRange: IntRange
	): World {
		val ctx = TickContext(
			transform = world.transform,
			velocity = world.velocity,
			isOnGround = world.isOnGround,
			sprite = world.sprite,
			phase = world.phase,
			tileMap = tileMap,
			visibleRange = visibleRange
		)
		val frame = character.update(input, ctx, dt)
		return World(
			transform = frame.transform,
			velocity = frame.velocity,
			isOnGround = frame.isOnGround,
			sprite = frame.sprite,
			phase = frame.phase
		)
	}
}
