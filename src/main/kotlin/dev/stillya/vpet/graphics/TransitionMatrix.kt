package dev.stillya.vpet.graphics

import com.intellij.openapi.diagnostic.Logger

class TransitionMatrix {
	private val log = Logger.getInstance(TransitionMatrix::class.java)
	private val transitions =
		mutableMapOf<Pair<AnimationState, AnimationState>, List<AnimationStep>>()
	private val idleVariants = mutableMapOf<AnimationState, List<AnimationStep>>()

	@Volatile
	var currentState: AnimationState = AnimationState.IDLE
		private set

	fun transitionTo(targetState: AnimationState): List<AnimationStep> {
		if (currentState == targetState) {
			log.trace("Already in state $targetState, returning idle variant")
			return idleVariants[targetState] ?: emptyList()
		}

		val transition = transitions[currentState to targetState]
			?: transitions[AnimationState.IDLE to targetState]
			?: emptyList()

		log.trace("State transition: $currentState → $targetState (${transition.size} steps)")
		currentState = targetState
		return transition
	}

	fun defineTransition(
		from: AnimationState,
		to: AnimationState,
		steps: List<AnimationStep>
	) {
		transitions[from to to] = steps
	}

	fun defineIdleVariant(state: AnimationState, steps: List<AnimationStep>) {
		idleVariants[state] = steps
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
		infix fun via(steps: List<AnimationStep>) {
			matrix.defineTransition(from, to, steps)
		}
	}

	fun from(state: AnimationState): TransitionBuilder {
		return TransitionBuilder(state)
	}

	fun idle(state: AnimationState, steps: List<AnimationStep>) {
		matrix.defineIdleVariant(state, steps)
	}
}

fun transitions(builder: TransitionMatrixBuilder.() -> Unit): TransitionMatrix {
	val matrix = TransitionMatrix()
	TransitionMatrixBuilder(matrix).apply(builder)
	return matrix
}
