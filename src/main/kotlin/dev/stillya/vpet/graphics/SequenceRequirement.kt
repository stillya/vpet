package dev.stillya.vpet.graphics

data class SequenceRequirement(
	val pose: Pose? = null,
	val speedMin: Float? = null,
	val speedMax: Float? = null,
	val direction: Direction? = null,
	val requiredFlags: Set<StateFlag> = emptySet(),
	val forbiddenFlags: Set<StateFlag> = emptySet()
) {
	fun isSatisfiedBy(state: RuntimeState): Boolean {
		if (pose != null && state.pose != pose) return false
		if (speedMin != null && state.kinematics.speed < speedMin) return false
		if (speedMax != null && state.kinematics.speed > speedMax) return false
		if (direction != null && state.kinematics.direction.angle != direction.angle) return false
		if (!state.flags.containsAll(requiredFlags)) return false
		if (state.flags.any { it in forbiddenFlags }) return false
		return true
	}

	fun hashKey(): String {
		val poseStr = pose?.name ?: "any"
		val speedStr = "${speedMin ?: 0f}-${speedMax ?: Float.MAX_VALUE}"
		val dirStr = direction?.angle?.toString() ?: "any"
		val reqFlagsStr = requiredFlags.sorted().joinToString(",")
		val forbFlagsStr = forbiddenFlags.sorted().joinToString(",")
		return "${poseStr}_${speedStr}_${dirStr}_req:${reqFlagsStr}_forb:${forbFlagsStr}"
	}

	companion object {
		val NONE = SequenceRequirement()
	}
}

class SequenceRequirementBuilder {
	var pose: Pose? = null
	var speedMin: Float? = null
	var speedMax: Float? = null
	var direction: Direction? = null
	private val requiredFlags = mutableSetOf<StateFlag>()
	private val forbiddenFlags = mutableSetOf<StateFlag>()

	fun requireFlag(flag: StateFlag) {
		requiredFlags.add(flag)
	}

	fun forbidFlag(flag: StateFlag) {
		forbiddenFlags.add(flag)
	}

	fun build(): SequenceRequirement {
		return SequenceRequirement(
			pose = pose,
			speedMin = speedMin,
			speedMax = speedMax,
			direction = direction,
			requiredFlags = requiredFlags.toSet(),
			forbiddenFlags = forbiddenFlags.toSet()
		)
	}
}

fun requirement(builder: SequenceRequirementBuilder.() -> Unit): SequenceRequirement {
	return SequenceRequirementBuilder().apply(builder).build()
}
