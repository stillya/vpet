package dev.stillya.vpet.animation

data class SequenceRequirement(
	val pose: Pose? = null,
	val speed: Float? = null,
	val direction: Direction? = null
) {
	fun isSatisfiedBy(state: StateEffect): Boolean {
		if (pose != null && state.pose != pose) return false
		if (speed != null && (state.speed ?: 0f) != speed) return false
		if (direction != null && state.direction?.angle != direction.angle) return false

		return true
	}

	fun hashKey(): String {
		val poseStr = pose?.name ?: "any"
		val speedStr = "${speed ?: 0f}"
		val dirStr = direction?.angle?.toString() ?: "any"
		return "${poseStr}_${speedStr}_${dirStr}"
	}

	companion object {
		val NONE = SequenceRequirement()
	}
}

class SequenceRequirementBuilder {
	var pose: Pose? = null
	var speed: Float? = null
	var direction: Direction? = null

	fun build(): SequenceRequirement {
		return SequenceRequirement(
			pose = pose,
			speed = speed,
			direction = direction,
		)
	}
}

fun requirement(builder: SequenceRequirementBuilder.() -> Unit): SequenceRequirement {
	return SequenceRequirementBuilder().apply(builder).build()
}
