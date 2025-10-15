package dev.stillya.vpet.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransitionMatrixTest {

	private lateinit var matrix: TransitionMatrix

	@Before
	fun setup() {
		matrix = transitions {
			idle(AnimationState.IDLE, sequence {
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
		val steps = matrix.transitionTo(AnimationState.IDLE)

		assertEquals(1, steps.size)
		assertEquals("Idle", steps[0].animationTag)
		assertEquals(AnimationState.IDLE, matrix.currentState)
	}

	@Test
	fun testTransitionToNewState() {
		val steps = matrix.transitionTo(AnimationState.WALKING)

		assertEquals(1, steps.size)
		assertEquals("Walk", steps[0].animationTag)
		assertEquals(5, steps[0].loops)
		assertEquals(AnimationState.WALKING, matrix.currentState)
	}

	@Test
	fun testTransitionWithMultipleSteps() {
		val steps = matrix.transitionTo(AnimationState.RUNNING)

		assertEquals(2, steps.size)
		assertEquals("Walk_Run", steps[0].animationTag)
		assertTrue(steps[0].isTransition)
		assertEquals("Run", steps[1].animationTag)
		assertEquals(-1, steps[1].loops)
		assertEquals(AnimationState.RUNNING, matrix.currentState)
	}

	@Test
	fun testGetNonExistentTransition() {
		val steps = matrix.getTransition(AnimationState.RUNNING, AnimationState.WALKING)

		assertTrue(steps.isEmpty())
	}
}
