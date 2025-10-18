package dev.stillya.vpet.graphics

data class StateEffect(
	val pose: Pose? = null,
	val speed: Float? = null,
	val speedDelta: Float? = null,
	val direction: Direction? = null,
	val flagsToAdd: Set<StateFlag> = emptySet(),
	val flagsToRemove: Set<StateFlag> = emptySet()
) {
	companion object {
		val NONE = StateEffect()

		fun setPose(pose: Pose) = StateEffect(pose = pose)
		fun setSpeed(speed: Float) = StateEffect(speed = speed)
		fun stop() = StateEffect(speed = 0f)
		fun accelerate(delta: Float) = StateEffect(speedDelta = delta)
		fun decelerate(delta: Float) = StateEffect(speedDelta = -delta)
		fun setDirection(direction: Direction) = StateEffect(direction = direction)
		fun flip() = StateEffect()
		fun addFlag(flag: StateFlag) = StateEffect(flagsToAdd = setOf(flag))
		fun removeFlag(flag: StateFlag) = StateEffect(flagsToRemove = setOf(flag))
	}

	operator fun plus(other: StateEffect): StateEffect {
		return StateEffect(
			pose = other.pose ?: this.pose,
			speed = other.speed ?: this.speed,
			speedDelta = when {
				this.speedDelta != null && other.speedDelta != null -> this.speedDelta + other.speedDelta
				else -> other.speedDelta ?: this.speedDelta
			},
			direction = other.direction ?: this.direction,
			flagsToAdd = this.flagsToAdd + other.flagsToAdd,
			flagsToRemove = this.flagsToRemove + other.flagsToRemove
		)
	}
}
