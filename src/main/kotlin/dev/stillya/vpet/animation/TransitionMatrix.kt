package dev.stillya.vpet.animation

import com.intellij.openapi.diagnostic.logger
import kotlin.random.Random

class TransitionMatrix(
	private val random: Random = Random
) {
	private val log = logger<TransitionMatrix>()
	private val transitions =
		mutableMapOf<Pair<AnimationState, AnimationState>, MutableList<AnimationSequenceWithRequirement>>()
	private val idleVariants = mutableListOf<AnimationSequenceWithRequirement>()

	fun transitionTo(
		currentState: AnimationState,
		targetState: AnimationState
	): Pair<AnimationSequenceWithRequirement, AnimationState> {
		val transition = if (targetState == AnimationState.IDLE) {
			log.trace("Transitioning to IDLE state, selecting random idle variant")
			idleVariants.random(random)
		} else {
			log.trace("State transition: $currentState → $targetState")
			val variants = transitions[currentState to targetState]
			if (variants.isNullOrEmpty()) {
				log.trace("No transition found for $currentState → $targetState")
				AnimationSequenceWithRequirement(SequenceRequirement.NONE, emptyList())
			} else {
				val selected = variants.random(random)
				if (variants.size > 1) {
					log.trace("Selected variant ${variants.indexOf(selected) + 1}/${variants.size}")
				}
				selected
			}
		}

		return transition to targetState
	}

	fun defineTransition(
		from: AnimationState,
		to: AnimationState,
		sequence: AnimationSequenceWithRequirement
	) {
		transitions.getOrPut(from to to) { mutableListOf() }.add(sequence)
	}

	fun defineIdleVariant(vararg sequences: AnimationSequenceWithRequirement) {
		idleVariants += sequences
	}
}

class TransitionMatrixBuilder(private val matrix: TransitionMatrix) {

	inner class TransitionBuilder(private val from: AnimationState) {
		infix fun to(target: AnimationState): TransitionStepBuilder {
			return TransitionStepBuilder(from, target)
		}
	}

	inner class TransitionStepBuilder(
		private val from: AnimationState,
		private val to: AnimationState
	) {
		infix fun via(sequence: AnimationSequenceWithRequirement) {
			matrix.defineTransition(from, to, sequence)
		}
	}

	fun from(state: AnimationState): TransitionBuilder {
		return TransitionBuilder(state)
	}

	fun idle(vararg sequences: AnimationSequenceWithRequirement) {
		matrix.defineIdleVariant(*sequences)
	}
}

fun transitions(
	random: Random = Random,
	builder: TransitionMatrixBuilder.() -> Unit
): TransitionMatrix {
	val matrix = TransitionMatrix(random)
	TransitionMatrixBuilder(matrix).apply(builder)
	return matrix
}
