package dev.stillya.vpet.graphics

import com.intellij.openapi.diagnostic.Logger

data class PlanResult(
	val bridges: List<Bridge>,
	val finalState: RuntimeState,
	val totalCost: Float
)

interface Planner {
	fun plan(
		currentState: RuntimeState,
		requirement: SequenceRequirement,
		bridgeSet: BridgeSet,
		costWeights: CostWeights = CostWeights.DEFAULT
	): PlanResult
}

class GreedyPlanner(
	private val maxBridges: Int = 4
) : Planner {
	private val log = Logger.getInstance(GreedyPlanner::class.java)

	override fun plan(
		currentState: RuntimeState,
		requirement: SequenceRequirement,
		bridgeSet: BridgeSet,
		costWeights: CostWeights
	): PlanResult {
		log.trace("Planning from state: $currentState to requirement: $requirement")

		if (requirement.isSatisfiedBy(currentState)) {
			log.trace("Current state already satisfies requirement")
			return PlanResult(emptyList(), currentState, 0f)
		}

		val bridges = mutableListOf<Bridge>()
		var state = currentState
		var totalCost = 0f

		for (step in 0 until maxBridges) {
			if (requirement.isSatisfiedBy(state)) {
				log.trace("Requirement satisfied after $step bridge(s)")
				break
			}

			val nextBridge = selectNextBridge(state, requirement, bridgeSet, costWeights)
			if (nextBridge == null) {
				log.trace("No applicable bridge found at step $step, using fallback")
				return fallbackPlan(state, requirement, bridgeSet, costWeights)
			}

			log.trace("Step $step: Applying bridge '${nextBridge.name}'")
			bridges.add(nextBridge)
			state = applyBridgeEffect(state, nextBridge.effect)
			totalCost += nextBridge.computeCost(state, requirement, costWeights)
		}

		if (!requirement.isSatisfiedBy(state)) {
			log.trace("Max bridges reached without satisfying requirement, using fallback")
			return fallbackPlan(currentState, requirement, bridgeSet, costWeights)
		}

		return PlanResult(bridges, state, totalCost)
	}

	private fun selectNextBridge(
		state: RuntimeState,
		requirement: SequenceRequirement,
		bridgeSet: BridgeSet,
		costWeights: CostWeights
	): Bridge? {
		val speedMismatch = computeSpeedMismatch(state, requirement)
		if (speedMismatch > 0.1f) {
			val stopBridge = bridgeSet.findApplicable(state)
				.firstOrNull { it.name == "Stop" }
			if (stopBridge != null) return stopBridge
		}

		val poseMismatch = requirement.pose != null && state.pose != requirement.pose
		if (poseMismatch) {
			val poseBridge = findPoseBridge(state, requirement.pose!!, bridgeSet)
			if (poseBridge != null) return poseBridge
		}

		val applicable = bridgeSet.findApplicable(state)
		return applicable.minByOrNull { it.computeCost(state, requirement, costWeights) }
	}

	private fun findPoseBridge(
		state: RuntimeState,
		targetPose: Pose,
		bridgeSet: BridgeSet
	): Bridge? {
		val directBridge = bridgeSet.findApplicable(state)
			.firstOrNull { bridge ->
				bridge.effect.pose == targetPose
			}

		if (directBridge != null) return directBridge

		if (state.pose == Pose.LIE && targetPose == Pose.STAND) {
			return bridgeSet.findApplicable(state)
				.firstOrNull { it.effect.pose == Pose.SIT }
		}

		return null
	}

	private fun computeSpeedMismatch(
		state: RuntimeState,
		requirement: SequenceRequirement
	): Float {
		val speed = state.kinematics.speed
		return when {
			requirement.speedMax != null && speed > requirement.speedMax -> speed - requirement.speedMax
			requirement.speedMin != null && speed < requirement.speedMin -> requirement.speedMin - speed
			else -> 0f
		}
	}

	private fun fallbackPlan(
		currentState: RuntimeState,
		requirement: SequenceRequirement,
		bridgeSet: BridgeSet,
		costWeights: CostWeights
	): PlanResult {
		log.trace("Executing fallback plan")
		val bridges = mutableListOf<Bridge>()
		var state = currentState
		var totalCost = 0f

		if (state.kinematics.speed > 0f) {
			val stopBridge = bridgeSet.getAllBridges()
				.firstOrNull { it.name == "Stop" }
			if (stopBridge != null && stopBridge.canApply(state)) {
				bridges.add(stopBridge)
				state = applyBridgeEffect(state, stopBridge.effect)
				totalCost += stopBridge.baseCost
			}
		}

		requirement.pose?.let { targetPose ->
			if (state.pose != targetPose) {
				val path = findPosePath(state.pose, targetPose, bridgeSet)
				for (bridge in path) {
					if (bridge.canApply(state)) {
						bridges.add(bridge)
						state = applyBridgeEffect(state, bridge.effect)
						totalCost += bridge.baseCost
					}
				}
			}
		}

		return PlanResult(bridges, state, totalCost + costWeights.hardCutPenalty)
	}

	private fun findPosePath(
		from: Pose,
		to: Pose,
		bridgeSet: BridgeSet
	): List<Bridge> {
		if (from == to) return emptyList()

		val allBridges = bridgeSet.getAllBridges()

		val direct = allBridges.firstOrNull { bridge ->
			bridge.guard.canApply(RuntimeState(pose = from)) &&
				bridge.effect.pose == to
		}
		if (direct != null) return listOf(direct)

		val viaStand = mutableListOf<Bridge>()
		if (from != Pose.STAND) {
			allBridges.firstOrNull { bridge ->
				bridge.guard.canApply(RuntimeState(pose = from)) &&
					bridge.effect.pose == Pose.STAND
			}?.let { viaStand.add(it) }
		}
		if (to != Pose.STAND) {
			allBridges.firstOrNull { bridge ->
				bridge.guard.canApply(RuntimeState(pose = Pose.STAND)) &&
					bridge.effect.pose == to
			}?.let { viaStand.add(it) }
		}

		return if (viaStand.isNotEmpty()) viaStand else emptyList()
	}

	private fun applyBridgeEffect(state: RuntimeState, effect: StateEffect): RuntimeState {
		var newState = state
		effect.pose?.let { newState = newState.withPose(it) }
		effect.speed?.let { newState = newState.withSpeed(it) }
		effect.speedDelta?.let {
			newState = newState.withSpeed((newState.kinematics.speed + it).coerceAtLeast(0f))
		}
		effect.direction?.let { newState = newState.withDirection(it) }
		effect.flagsToAdd.forEach { newState = newState.addFlag(it) }
		effect.flagsToRemove.forEach { newState = newState.removeFlag(it) }
		return newState
	}
}
