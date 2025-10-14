package dev.stillya.vpet.graphics

data class AnimationGuard(
	val canStart: (AnimationContext, Long) -> Boolean = { _, _ -> true },
	val canContinue: (AnimationContext, Long) -> Boolean = { ctx, currentEpoch ->
		ctx.isValid(
			currentEpoch
		)
	},
	val isInterruptible: Boolean = true
) {
	companion object {
		val ALWAYS_VALID = AnimationGuard()

		val NON_INTERRUPTIBLE = AnimationGuard(
			isInterruptible = false
		)

		fun epochGuard(isInterruptible: Boolean = true) = AnimationGuard(
			canStart = { ctx, currentEpoch -> ctx.isValid(currentEpoch) && !ctx.isExpired() },
			canContinue = { ctx, currentEpoch -> ctx.isValid(currentEpoch) },
			isInterruptible = isInterruptible
		)

		fun buildGuard() = AnimationGuard(
			canStart = { ctx, _ ->
				ctx.triggerEvent in setOf(
					AnimationTrigger.BUILD_START,
					AnimationTrigger.BUILD_SUCCESS,
					AnimationTrigger.BUILD_FAIL
				)
			},
			canContinue = { ctx, currentEpoch -> ctx.isValid(currentEpoch) },
			isInterruptible = true
		)

		fun transitionGuard() = AnimationGuard(
			canStart = { _, _ -> true },
			canContinue = { _, _ -> true },
			isInterruptible = false
		)
	}
}
