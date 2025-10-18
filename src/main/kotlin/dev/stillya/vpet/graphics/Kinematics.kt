package dev.stillya.vpet.graphics

data class Kinematics(
	val speed: Float = 0f,
	val direction: Direction = Direction.RIGHT
) {
	init {
		require(speed >= 0f) { "Speed must be non-negative, got $speed" }
	}

	companion object {
		val STOPPED = Kinematics(0f, Direction.RIGHT)
	}
}
