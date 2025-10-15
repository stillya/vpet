package dev.stillya.vpet.graphics

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AnimationEpochManagerTest {

	private lateinit var manager: AnimationEpochManager

	@Before
	fun setup() {
		manager = AnimationEpochManager()
	}

	@Test
	fun testIncrementEpochIncreases() {
		assertEquals(0, manager.getCurrentEpoch())

		val epoch1 = manager.incrementEpoch()
		assertEquals(1, epoch1)
		assertEquals(1, manager.getCurrentEpoch())

		val epoch2 = manager.incrementEpoch()
		assertEquals(2, epoch2)
		assertEquals(2, manager.getCurrentEpoch())

		val context1 = manager.createContext(
			AnimationTrigger.IDLE_BEHAVIOR,
			AnimationState.IDLE
		)

		assertEquals(3, context1.epoch)
		assertEquals(3, manager.getCurrentEpoch())

		val context2 = manager.createContext(
			AnimationTrigger.BUILD_START,
			AnimationState.RUNNING
		)

		assertEquals(4, context2.epoch)
		assertEquals(4, manager.getCurrentEpoch())
	}
}
