package dev.stillya.vpet.animation

import com.intellij.openapi.diagnostic.logger

data class PlanResult(
	val bridges: List<Bridge>,
	val finalEffect: StateEffect
)

interface Planner {
	fun plan(
		currentEffect: StateEffect,
		requirement: SequenceRequirement,
		bridges: List<Bridge>
	): PlanResult
}

class GreedyPlanner(
	private val maxBridges: Int = 10
) : Planner {
	private val log = logger<GreedyPlanner>()

	override fun plan(
		currentEffect: StateEffect,
		requirement: SequenceRequirement,
		bridges: List<Bridge>
	): PlanResult {
		log.trace("Planning from effect: $currentEffect to requirement: $requirement")

		if (requirement.isSatisfiedBy(currentEffect)) {
			log.trace("Current effect already satisfies requirement")
			return PlanResult(emptyList(), currentEffect)
		}

		val selectedBridges = mutableListOf<Bridge>()
		var effect = currentEffect

		for (step in 0 until maxBridges) {
			if (requirement.isSatisfiedBy(effect)) {
				log.trace("Requirement satisfied after $step bridge(s)")
				break
			}

			val nextBridge = selectNextBridge(effect, requirement, bridges)
			if (nextBridge == null) {
				log.trace("No applicable bridge found at step $step")
				break
			}

			log.trace("Step $step: Applying bridge '${nextBridge.name}'")
			selectedBridges.add(nextBridge)
			effect = applyBridgeEffect(effect, nextBridge.effect)
		}

		return PlanResult(selectedBridges, effect)
	}

	private fun selectNextBridge(
		effect: StateEffect,
		requirement: SequenceRequirement,
		bridges: List<Bridge>
	): Bridge? {
		val poseMismatch = requirement.pose != null && effect.pose != requirement.pose
		if (poseMismatch) {
			val directBridge = bridges.firstOrNull { bridge ->
				bridge.guard.canApply(effect) && bridge.effect.pose == requirement.pose
			}
			if (directBridge != null) return directBridge

			val anyPoseBridge = bridges.firstOrNull { bridge ->
				bridge.guard.canApply(effect) && bridge.effect.pose != null
			}
			if (anyPoseBridge != null) {
				log.trace("No direct pose bridge, taking intermediate: ${anyPoseBridge.name}")
				return anyPoseBridge
			}
		}

		val speedMismatch = requirement.speed != null && (effect.speed ?: 0f) != requirement.speed
		if (speedMismatch) {
			val speedBridge = bridges.firstOrNull { bridge ->
				bridge.guard.canApply(effect) && bridge.effect.speed == requirement.speed
			}
			if (speedBridge != null) return speedBridge
		}

		val directionMismatch = requirement.direction != null && effect.direction?.angle != requirement.direction.angle
		if (directionMismatch) {
			val dirBridge = bridges.firstOrNull { bridge ->
				bridge.guard.canApply(effect) && bridge.effect.direction == requirement.direction
			}
			if (dirBridge != null) return dirBridge
		}

		return null
	}

	private fun applyBridgeEffect(current: StateEffect, bridge: StateEffect): StateEffect {
		val newPose = bridge.pose ?: current.pose
		val newSpeed = bridge.speed ?: current.speed
		val newDirection = bridge.direction ?: current.direction

		log.trace("Applying bridge effect: $current + $bridge = StateEffect(pose=$newPose, speed=$newSpeed, direction=$newDirection)")

		return StateEffect(
			pose = newPose,
			speed = newSpeed,
			direction = newDirection
		)
	}
}
