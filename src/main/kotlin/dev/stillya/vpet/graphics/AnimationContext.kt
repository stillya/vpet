package dev.stillya.vpet.graphics

import java.util.concurrent.atomic.AtomicLong

enum class AnimationTrigger {
	BUILD_START,
	BUILD_SUCCESS,
	BUILD_FAIL,
	USER_CLICK,
	IDLE_BEHAVIOR,
	STATE_TRANSITION
}

data class AnimationContext(
	val epoch: Long,
	val triggerEvent: AnimationTrigger,
	val targetState: AnimationState,
	val timestamp: Long = System.currentTimeMillis()
) {
	fun isValid(currentEpoch: Long): Boolean {
		return epoch == currentEpoch
	}

	fun isExpired(maxAgeMs: Long = 30000): Boolean {
		return System.currentTimeMillis() - timestamp > maxAgeMs
	}
}

class AnimationEpochManager {
	private val currentEpoch = AtomicLong(0)

	fun getCurrentEpoch(): Long = currentEpoch.get()

	fun incrementEpoch(): Long = currentEpoch.incrementAndGet()

	fun createContext(
		trigger: AnimationTrigger,
		targetState: AnimationState
	): AnimationContext {
		return AnimationContext(
			epoch = incrementEpoch(),
			triggerEvent = trigger,
			targetState = targetState
		)
	}

	companion object {
		private val instance = AnimationEpochManager()

		fun currentEpoch(): Long = instance.getCurrentEpoch()
		fun increment(): Long = instance.incrementEpoch()
	}
}
