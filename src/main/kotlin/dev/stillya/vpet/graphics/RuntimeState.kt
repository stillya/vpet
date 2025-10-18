package dev.stillya.vpet.graphics

data class RuntimeState(
	val pose: Pose = Pose.STAND,
	val kinematics: Kinematics = Kinematics.STOPPED,
	val flags: Set<StateFlag> = emptySet()
) {
	fun withPose(newPose: Pose): RuntimeState = copy(pose = newPose)
	fun withKinematics(newKinematics: Kinematics): RuntimeState = copy(kinematics = newKinematics)
	fun withSpeed(newSpeed: Float): RuntimeState = copy(kinematics = kinematics.copy(speed = newSpeed))
	fun withDirection(newDirection: Direction): RuntimeState = copy(kinematics = kinematics.copy(direction = newDirection))
	fun withFlags(newFlags: Set<StateFlag>): RuntimeState = copy(flags = newFlags)
	fun addFlag(flag: StateFlag): RuntimeState = copy(flags = flags + flag)
	fun removeFlag(flag: StateFlag): RuntimeState = copy(flags = flags - flag)

	fun hashKey(): String {
		return "${pose.name}_${kinematics.speed}_${kinematics.direction.angle}_${flags.sorted().joinToString(",")}"
	}

	companion object {
		val DEFAULT = RuntimeState()
	}
}

enum class StateFlag {
	AIRBORNE,
	CARRIED
}
