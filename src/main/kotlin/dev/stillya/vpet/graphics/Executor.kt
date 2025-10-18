package dev.stillya.vpet.graphics

import com.intellij.openapi.diagnostic.Logger

class Executor(
	private val stateTracker: StateTracker,
	private val bridgeSet: BridgeSet,
	private val planner: Planner = GreedyPlanner(),
	private val costWeights: CostWeights = CostWeights.DEFAULT
) {
	private val log = Logger.getInstance(Executor::class.java)
	private val planCache = mutableMapOf<String, PlanResult>()

	fun buildPlaylist(
		sequenceWithRequirement: AnimationSequenceWithRequirement,
		context: AnimationContext?
	): ExecutionPlan {
		val currentState = stateTracker.getCurrentState()
		val requirement = sequenceWithRequirement.requirement

		log.trace("Building playlist for requirement: $requirement from state: $currentState")

		if (!validateContext(context)) {
			log.trace("Context validation failed, rejecting execution")
			return ExecutionPlan.EMPTY
		}

		val planResult = if (requirement == SequenceRequirement.NONE) {
			PlanResult(emptyList(), currentState, 0f)
		} else {
			getCachedOrPlan(currentState, requirement)
		}

		if (!requirement.isSatisfiedBy(planResult.finalState)) {
			log.trace("Warning: Final state does not satisfy requirement after planning")
		}

		val bridgeSteps = planResult.bridges.map { bridge ->
			AnimationStep(
				animationTag = bridge.animationTag,
				loops = 1,
				guard = AnimationGuard.ALWAYS_VALID,
				isTransition = true,
				effect = bridge.effect
			)
		}

		val allSteps = bridgeSteps + sequenceWithRequirement.steps

		log.trace(
			"Execution plan: ${bridgeSteps.size} bridge(s) + ${sequenceWithRequirement.steps.size} sequence step(s)"
		)

		return ExecutionPlan(
			steps = allSteps,
			totalCost = planResult.totalCost,
			context = context
		)
	}

	fun applyStepEffect(step: AnimationStep) {
		if (step.effect != StateEffect.NONE) {
			log.trace("Applying effect from step '${step.animationTag}': ${step.effect}")
			stateTracker.applyEffect(step.effect)
		}
	}

	fun getCurrentState(): RuntimeState = stateTracker.getCurrentState()

	fun invalidateCache() {
		log.trace("Invalidating plan cache")
		planCache.clear()
	}

	private fun getCachedOrPlan(
		currentState: RuntimeState,
		requirement: SequenceRequirement
	): PlanResult {
		val cacheKey = "${currentState.hashKey()}_to_${requirement.hashKey()}"

		return planCache.getOrPut(cacheKey) {
			log.trace("Cache miss for key: $cacheKey, planning...")
			planner.plan(currentState, requirement, bridgeSet, costWeights)
		}.also {
			if (planCache.containsKey(cacheKey)) {
				log.trace("Cache hit for key: $cacheKey")
			}
		}
	}

	private fun validateContext(context: AnimationContext?): Boolean {
		if (context == null) return true

		val currentEpoch = AnimationEpochManager.currentEpoch()
		val isValid = context.epoch == currentEpoch

		if (!isValid) {
			log.trace("Context epoch ${context.epoch} does not match current epoch $currentEpoch")
		}

		return isValid
	}
}

data class ExecutionPlan(
	val steps: List<AnimationStep>,
	val totalCost: Float,
	val context: AnimationContext?
) {
	companion object {
		val EMPTY = ExecutionPlan(emptyList(), 0f, null)
	}
}
