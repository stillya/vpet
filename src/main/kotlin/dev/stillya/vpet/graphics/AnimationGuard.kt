package dev.stillya.vpet.graphics

interface AnimationGuard {
	fun canStart(context: AnimationContext, currentEpoch: Long): Boolean
	fun canContinue(context: AnimationContext, currentEpoch: Long): Boolean
	val isInterruptible: Boolean

	companion object {
		val ALWAYS_VALID: AnimationGuard = AlwaysValidGuard

		val NON_INTERRUPTIBLE: AnimationGuard = NonInterruptibleGuard

		fun epochGuard(isInterruptible: Boolean = true): AnimationGuard =
			EpochGuard(isInterruptible)

		fun buildGuard(): AnimationGuard = BuildAnimationGuard

		fun transitionGuard(): AnimationGuard = TransitionGuard
	}
}

object AlwaysValidGuard : AnimationGuard {
	override fun canStart(context: AnimationContext, currentEpoch: Long) = true
	override fun canContinue(context: AnimationContext, currentEpoch: Long) =
		context.isValid(currentEpoch)

	override val isInterruptible = true
}

object NonInterruptibleGuard : AnimationGuard {
	override fun canStart(context: AnimationContext, currentEpoch: Long) = true
	override fun canContinue(context: AnimationContext, currentEpoch: Long) =
		context.isValid(currentEpoch)

	override val isInterruptible = false
}

class EpochGuard(override val isInterruptible: Boolean = true) : AnimationGuard {
	override fun canStart(context: AnimationContext, currentEpoch: Long) =
		context.isValid(currentEpoch) && !context.isExpired()

	override fun canContinue(context: AnimationContext, currentEpoch: Long) =
		context.isValid(currentEpoch)
}

object TransitionGuard : AnimationGuard {
	override fun canStart(context: AnimationContext, currentEpoch: Long) = true
	override fun canContinue(context: AnimationContext, currentEpoch: Long) = true
	override val isInterruptible = false
}

object BuildAnimationGuard :
	AnimationGuard {
	override fun canStart(context: AnimationContext, currentEpoch: Long): Boolean {
		return context.triggerEvent == AnimationTrigger.BUILD_START
	}

	override fun canContinue(context: AnimationContext, currentEpoch: Long): Boolean {
		return context.isValid(currentEpoch)
	}

	override val isInterruptible = true
}
