package dev.stillya.vpet.graphics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationSequenceTest {

	@Test
	fun testStepSequence() {
		var steps = sequence {
			play("Walk", loops = 5)
		}

		assertEquals(1, steps.size)
		assertEquals("Walk", steps[0].animationTag)
		assertEquals(5, steps[0].loops)
		assertFalse(steps[0].isTransition)
		assertTrue(steps[0].variants.isEmpty())

		steps = sequence {
			transition("Walk_Run")
		}

		assertEquals(1, steps.size)
		assertEquals("Walk_Run", steps[0].animationTag)
		assertTrue(steps[0].isTransition)
	}

	@Test
	fun testStepsVariants() {
		val steps = sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = 3)
		}

		assertEquals(1, steps.size)
		assertEquals("Idle", steps[0].animationTag)
		assertEquals(4, steps[0].variants.size)
		assertTrue(steps[0].variants.contains("Idle"))
		assertTrue(steps[0].variants.contains("Sit"))
		assertTrue(steps[0].variants.contains("Dream"))
		assertTrue(steps[0].variants.contains("Rest"))
		assertEquals(3, steps[0].loops)
	}

	@Test
	fun testComplexSequence() {
		val steps = sequence {
			play("Paws", loops = 2)
			playRandom("J_1", "J_U_D", loops = 5)
			playRandom(
				"Attack_1",
				"Attack_2",
				"Attack_3",
				"Attack_4",
				"Attack_5",
				loops = 3
			)
			play("Walk", loops = 3)
		}

		assertEquals(4, steps.size)

		assertEquals("Paws", steps[0].animationTag)
		assertEquals(2, steps[0].loops)

		assertEquals(2, steps[1].variants.size)
		assertEquals(5, steps[1].loops)

		assertEquals(5, steps[2].variants.size)
		assertEquals(3, steps[2].loops)

		assertEquals("Walk", steps[3].animationTag)
		assertEquals(3, steps[3].loops)
	}
}
