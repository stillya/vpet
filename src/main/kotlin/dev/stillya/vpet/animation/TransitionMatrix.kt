package dev.stillya.vpet.animation

import com.intellij.openapi.diagnostic.Logger
import kotlin.random.Random

class TransitionMatrix(
	private val random: Random = Random
) {
	private val log = Logger.getInstance(TransitionMatrix::class.java)
	private val transitions =
		mutableMapOf<Pair<AnimationState, AnimationState>, AnimationSequenceWithRequirement>()
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
			transitions[currentState to targetState] ?: AnimationSequenceWithRequirement(
				SequenceRequirement.NONE,
				emptyList()
			)
		}

		return transition to targetState
	}

	fun defineTransition(
		from: AnimationState,
		to: AnimationState,
		sequence: AnimationSequenceWithRequirement
	) {
		transitions[from to to] = sequence
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
