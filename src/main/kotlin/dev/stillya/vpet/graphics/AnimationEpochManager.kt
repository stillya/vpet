package dev.stillya.vpet.graphics

import java.util.concurrent.atomic.AtomicLong

class AnimationEpochManager {
	private val currentEpoch = AtomicLong(0)

	fun getCurrentEpoch(): Long = currentEpoch.get()

	fun incrementEpoch(): Long = currentEpoch.incrementAndGet()

	fun createContext(
		trigger: AnimationTrigger,
	): AnimationContext {
		return AnimationContext(
			epoch = incrementEpoch(),
			triggerEvent = trigger,
		)
	}
}