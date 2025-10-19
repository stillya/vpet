package dev.stillya.vpet.graphics

enum class AnimationTrigger {
	BUILD_START,
	BUILD_SUCCESS,
	BUILD_FAIL,
	USER_CLICK,
	IDLE_BEHAVIOR,
}

data class AnimationContext(
	val epoch: Long,
	val triggerEvent: AnimationTrigger,
	val timestamp: Long = System.currentTimeMillis()
) {
	fun isValid(currentEpoch: Long): Boolean {
		return epoch == currentEpoch
	}

	fun isExpired(maxAgeMs: Long = 30000): Boolean {
		return System.currentTimeMillis() - timestamp > maxAgeMs
	}
}
