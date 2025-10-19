package dev.stillya.vpet.animation

data class Bridge(
	val name: String,
	val animationTag: String,
	val guard: BridgeGuard,
	val effect: StateEffect
)

interface BridgeGuard {
	fun canApply(effect: StateEffect): Boolean

	companion object {
		fun always(): BridgeGuard = object : BridgeGuard {
			override fun canApply(effect: StateEffect) = true
		}

		fun requirePose(pose: Pose): BridgeGuard = object : BridgeGuard {
			override fun canApply(effect: StateEffect) = effect.pose == pose
		}

		fun requireSpeed(speed: Float): BridgeGuard = object : BridgeGuard {
			override fun canApply(effect: StateEffect) = (effect.speed ?: 0f) == speed
		}

		fun and(vararg guards: BridgeGuard): BridgeGuard = object : BridgeGuard {
			override fun canApply(effect: StateEffect) =
				guards.all { it.canApply(effect) }
		}

		fun or(vararg guards: BridgeGuard): BridgeGuard = object : BridgeGuard {
			override fun canApply(effect: StateEffect) =
				guards.any { it.canApply(effect) }
		}
	}
}
