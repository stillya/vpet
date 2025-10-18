package dev.stillya.vpet.graphics

data class Bridge(
	val name: String,
	val animationTag: String,
	val guard: BridgeGuard,
	val effect: StateEffect,
	val baseCost: Float
) {
	fun canApply(state: RuntimeState): Boolean = guard.canApply(state)

	fun computeCost(state: RuntimeState, target: SequenceRequirement, weights: CostWeights): Float {
		var cost = baseCost

		if (name.contains("instant", ignoreCase = true) || name.contains("flip", ignoreCase = true)) {
			cost += weights.instantFlipPenalty
		}

		if (name.contains("cinematic", ignoreCase = true) || name.contains("stumble", ignoreCase = true)) {
			cost += weights.cinematicBonus
		}

		return cost
	}
}

interface BridgeGuard {
	fun canApply(state: RuntimeState): Boolean

	companion object {
		fun always(): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) = true
		}

		fun requirePose(pose: Pose): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) = state.pose == pose
		}

		fun requireSpeed(min: Float, max: Float): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) =
				state.kinematics.speed in min..max
		}

		fun requireSpeedAbove(threshold: Float): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) =
				state.kinematics.speed > threshold
		}

		fun requireSpeedBelow(threshold: Float): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) =
				state.kinematics.speed < threshold
		}

		fun and(vararg guards: BridgeGuard): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) =
				guards.all { it.canApply(state) }
		}

		fun or(vararg guards: BridgeGuard): BridgeGuard = object : BridgeGuard {
			override fun canApply(state: RuntimeState) =
				guards.any { it.canApply(state) }
		}
	}
}

data class CostWeights(
	val durationWeight: Float = 1.0f,
	val hardCutPenalty: Float = 5.0f,
	val instantFlipPenalty: Float = 2.0f,
	val unnaturalPoseJumpPenalty: Float = 10.0f,
	val cinematicBonus: Float = -1.0f,
	val phaseAlignedBonus: Float = -0.5f
) {
	companion object {
		val DEFAULT = CostWeights()
	}
}
