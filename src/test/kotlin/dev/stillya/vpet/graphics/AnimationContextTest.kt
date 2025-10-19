package dev.stillya.vpet.graphics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnimationContextTest {

	@Test
	fun testContextIsValidWithMatchingEpoch() {
		val context = AnimationContext(
			epoch = 5,
			triggerEvent = AnimationTrigger.IDLE_BEHAVIOR
		)

		assertTrue(context.isValid(5))
	}

	@Test
	fun testContextIsInvalidWithDifferentEpoch() {
		val context = AnimationContext(
			epoch = 5,
			triggerEvent = AnimationTrigger.IDLE_BEHAVIOR
		)

		assertFalse(context.isValid(6))
		assertFalse(context.isValid(4))
	}

	@Test
	fun testContextIsNotExpiredImmediately() {
		val context = AnimationContext(
			epoch = 1,
			triggerEvent = AnimationTrigger.IDLE_BEHAVIOR
		)

		assertFalse(context.isExpired())
	}

	@Test
	fun testContextIsExpiredWithOldTimestamp() {
		val oldTimestamp = System.currentTimeMillis() - 60000
		val context = AnimationContext(
			epoch = 1,
			triggerEvent = AnimationTrigger.IDLE_BEHAVIOR,
			timestamp = oldTimestamp
		)

		assertTrue(context.isExpired(30000))
	}
}
