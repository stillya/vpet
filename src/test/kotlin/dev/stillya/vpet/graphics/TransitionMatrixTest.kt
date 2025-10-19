package dev.stillya.vpet.graphics

import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.animation.TransitionMatrix
import dev.stillya.vpet.animation.sequence
import dev.stillya.vpet.animation.transitions
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransitionMatrixTest {

	private lateinit var matrix: TransitionMatrix

	@Before
	fun setup() {
		matrix = transitions {
			idle(sequence {
				play("Idle", loops = -1)
			})

			from(AnimationState.IDLE) to AnimationState.WALKING via sequence {
				play("Walk", loops = 5)
			}

			from(AnimationState.WALKING) to AnimationState.IDLE via sequence {
				play("Idle", loops = -1)
			}

			from(AnimationState.IDLE) to AnimationState.RUNNING via sequence {
				transition("Walk_Run")
				play("Run", loops = -1)
			}
		}
	}

	@Test
	fun testTransitionToSameState() {
		val (sequence, targetState) = matrix.transitionTo(AnimationState.IDLE, AnimationState.IDLE)

		assertEquals(1, sequence.steps.size)
		assertEquals("Idle", sequence.steps[0].animationTag)
		assertEquals(AnimationState.IDLE, targetState)
	}

	@Test
	fun testTransitionToNewState() {
		val (sequence, targetState) = matrix.transitionTo(AnimationState.IDLE, AnimationState.WALKING)

		assertEquals(1, sequence.steps.size)
		assertEquals("Walk", sequence.steps[0].animationTag)
		assertEquals(5, sequence.steps[0].loops)
		assertEquals(AnimationState.WALKING, targetState)
	}

	@Test
	fun testTransitionWithMultipleSteps() {
		val (sequence, targetState) = matrix.transitionTo(AnimationState.IDLE, AnimationState.RUNNING)

		assertEquals(2, sequence.steps.size)
		assertEquals("Walk_Run", sequence.steps[0].animationTag)
		assertEquals("Run", sequence.steps[1].animationTag)
		assertEquals(-1, sequence.steps[1].loops)
		assertEquals(AnimationState.RUNNING, targetState)
	}

	@Test
	fun testMultipleVariantsForSameTransition() {
		val matrixWithVariants = transitions(kotlin.random.Random(42)) {
			from(AnimationState.IDLE) to AnimationState.CELEBRATING via sequence {
				play("Paws", loops = 2)
				play("Jump")
			}

			from(AnimationState.IDLE) to AnimationState.CELEBRATING via sequence {
				play("Attack", loops = 3)
			}

			from(AnimationState.IDLE) to AnimationState.CELEBRATING via sequence {
				play("Dance")
				play("Walk", loops = 5)
			}
		}

		val selectedSequences = mutableSetOf<String>()
		repeat(20) {
			val (sequence, _) = matrixWithVariants.transitionTo(
				AnimationState.IDLE,
				AnimationState.CELEBRATING
			)
			selectedSequences.add(sequence.steps.joinToString(",") { it.animationTag })
		}

		assertEquals(
			"Should select from multiple variants",
			true,
			selectedSequences.size > 1
		)
	}
}
