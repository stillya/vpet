package dev.stillya.vpet.graphics

class BridgeSet {
	private val bridges = mutableListOf<Bridge>()

	fun add(bridge: Bridge) {
		bridges.add(bridge)
	}

	fun findApplicable(state: RuntimeState): List<Bridge> {
		return bridges.filter { it.canApply(state) }
	}

	fun getAllBridges(): List<Bridge> = bridges.toList()

	companion object {
		fun createDefaultCatBridges(): BridgeSet {
			val set = BridgeSet()

			set.add(
				Bridge(
					name = "Stop",
					animationTag = "Stop",
					guard = BridgeGuard.requireSpeedAbove(0f),
					effect = StateEffect.stop(),
					baseCost = 1.0f
				)
			)

			set.add(
				Bridge(
					name = "Stand_to_Sit",
					animationTag = "Sit_Down",
					guard = BridgeGuard.and(
						BridgeGuard.requirePose(Pose.STAND),
						BridgeGuard.requireSpeedBelow(0.1f)
					),
					effect = StateEffect.setPose(Pose.SIT),
					baseCost = 1.0f
				)
			)

			set.add(
				Bridge(
					name = "Sit_to_Stand",
					animationTag = "Sit_Up",
					guard = BridgeGuard.requirePose(Pose.SIT),
					effect = StateEffect.setPose(Pose.STAND),
					baseCost = 1.0f
				)
			)

			set.add(
				Bridge(
					name = "Lie_to_Sit",
					animationTag = "Dream_Sit",
					guard = BridgeGuard.requirePose(Pose.LIE),
					effect = StateEffect.setPose(Pose.SIT),
					baseCost = 1.5f
				)
			)

			set.add(
				Bridge(
					name = "Sit_to_Lie",
					animationTag = "Sit_Rest",
					guard = BridgeGuard.requirePose(Pose.SIT),
					effect = StateEffect.setPose(Pose.LIE),
					baseCost = 1.5f
				)
			)

			set.add(
				Bridge(
					name = "Walk_to_Run",
					animationTag = "Walk_Run",
					guard = BridgeGuard.and(
						BridgeGuard.requirePose(Pose.STAND),
						BridgeGuard.requireSpeed(0.3f, 0.7f)
					),
					effect = StateEffect.accelerate(0.3f),
					baseCost = 0.5f
				)
			)

			set.add(
				Bridge(
					name = "Start_Walk",
					animationTag = "Walk",
					guard = BridgeGuard.and(
						BridgeGuard.requirePose(Pose.STAND),
						BridgeGuard.requireSpeedBelow(0.3f)
					),
					effect = StateEffect.setSpeed(0.5f),
					baseCost = 0.8f
				)
			)

			set.add(
				Bridge(
					name = "Instant_Flip",
					animationTag = "",
					guard = BridgeGuard.always(),
					effect = StateEffect.flip(),
					baseCost = 0.1f
				)
			)

			return set
		}
	}
}
