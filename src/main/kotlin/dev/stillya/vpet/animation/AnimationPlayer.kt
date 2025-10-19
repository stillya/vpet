package dev.stillya.vpet.animation

import com.intellij.openapi.diagnostic.Logger
import dev.stillya.vpet.animation.AnimationGuard.Companion.transitionGuard

class AnimationPlayer(
	private val bridges: List<Bridge>,
	private val planner: Planner = GreedyPlanner()
) {
	private val log = Logger.getInstance(AnimationPlayer::class.java)

	@Volatile
	private var currentEffect: StateEffect = StateEffect.NONE

	fun buildPlaylist(sequence: AnimationSequenceWithRequirement): List<AnimationStep> {
		val requirement = sequence.requirement

		log.trace("Building playlist for requirement: $requirement from effect: $currentEffect")

		if (requirement == SequenceRequirement.NONE || requirement.isSatisfiedBy(currentEffect)) {
			log.trace("No requirement or already satisfied, returning original steps")
			return sequence.steps
		}

		val planResult = planner.plan(currentEffect, requirement, bridges)

		sequence.steps.reversed().find { it.effect != StateEffect.NONE }?.let { lastStepWithEffect ->
			log.trace("Found last step with effect: ${lastStepWithEffect.animationTag} -> ${lastStepWithEffect.effect}")
			currentEffect = StateEffect(
				pose = lastStepWithEffect.effect.pose,
				speed = lastStepWithEffect.effect.speed,
				direction = lastStepWithEffect.effect.direction
			)
		} ?: run {
			log.trace("No step with effect found, using planned effect directly")
			currentEffect = planResult.finalEffect
		}

		val bridgeSteps = planResult.bridges.map { bridge ->
			AnimationStep(
				animationTag = bridge.animationTag,
				loops = 1,
				guard = transitionGuard(),
				isTransition = true,
				effect = bridge.effect
			)
		}

		val allSteps = bridgeSteps + sequence.steps

		log.trace(
			"Execution plan: ${bridgeSteps.size} bridge(s) + ${sequence.steps.size} sequence step(s)"
		)

		return allSteps
	}

	fun setInitialEffect(effect: StateEffect) {
		log.trace("Setting initial effect: $effect")
		currentEffect = effect
	}

}
