package dev.stillya.vpet.graphics

data class Direction(val angle: Float) {
	companion object {
		val LEFT = Direction(180f)
		val RIGHT = Direction(0f)

		fun fromAngle(angle: Float): Direction = Direction(angle)
	}

	fun isLeft(): Boolean = angle > 90f && angle < 270f
	fun isRight(): Boolean = !isLeft()

	fun opposite(): Direction = Direction((angle + 180f) % 360f)
}
