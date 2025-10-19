package dev.stillya.vpet.graphics

import dev.stillya.vpet.animation.sequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationSequenceTest {

	@Test
	fun testStepSequence() {
		var seq = sequence {
			play("Walk", loops = 5)
		}

		assertEquals(1, seq.steps.size)
		assertEquals("Walk", seq.steps[0].animationTag)
		assertEquals(5, seq.steps[0].loops)
		assertTrue(seq.steps[0].variants.isEmpty())

		seq = sequence {
			transition("Walk_Run")
		}

		assertEquals(1, seq.steps.size)
		assertEquals("Walk_Run", seq.steps[0].animationTag)
	}

	@Test
	fun testStepsVariants() {
		val seq = sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = 3)
		}

		assertEquals(1, seq.steps.size)
		assertEquals("Idle", seq.steps[0].animationTag)
		assertEquals(4, seq.steps[0].variants.size)
		assertTrue(seq.steps[0].variants.contains("Idle"))
		assertTrue(seq.steps[0].variants.contains("Sit"))
		assertTrue(seq.steps[0].variants.contains("Dream"))
		assertTrue(seq.steps[0].variants.contains("Rest"))
		assertEquals(3, seq.steps[0].loops)
	}

	@Test
	fun testComplexSequence() {
		val seq = sequence {
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

		assertEquals(4, seq.steps.size)

		assertEquals("Paws", seq.steps[0].animationTag)
		assertEquals(2, seq.steps[0].loops)

		assertEquals(2, seq.steps[1].variants.size)
		assertEquals(5, seq.steps[1].loops)

		assertEquals(5, seq.steps[2].variants.size)
		assertEquals(3, seq.steps[2].loops)

		assertEquals("Walk", seq.steps[3].animationTag)
		assertEquals(3, seq.steps[3].loops)
	}
}
