package dev.stillya.vpet.animation

data class AnimationStep(
	val animationTag: String,
	val loops: Int = 1,
	val variants: List<String> = emptyList(),
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
	val effect: StateEffect = StateEffect.NONE
)

const val INFINITE = -1
const val SHORT_LOOP = 5
const val MEDIUM_LOOP = 8

const val NO_SPEED = 0.0f
const val WALKING_SPEED = 1.0f
const val RUNNING_SPEED = 3.0f

data class StateEffect(
	val pose: Pose? = null,
	val speed: Float? = null,
	val direction: Direction? = null,
) {
	companion object {
		val NONE = StateEffect()

		fun setPose(pose: Pose) = StateEffect(pose = pose)
		fun setSpeed(speed: Float) = StateEffect(speed = speed)
		fun stop() = StateEffect(speed = 0f)
		fun flip() = StateEffect()
	}
}


data class AnimationSequenceWithRequirement(
	val requirement: SequenceRequirement,
	val steps: List<AnimationStep>
)

class AnimationSequenceBuilder {
	private val steps = mutableListOf<AnimationStep>()
	private var requirement: SequenceRequirement = SequenceRequirement.NONE

	fun require(builder: SequenceRequirementBuilder.() -> Unit) {
		requirement = SequenceRequirementBuilder().apply(builder).build()
	}

	fun play(
		tag: String,
		loops: Int = 1,
		guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
		effect: StateEffect = StateEffect.NONE
	) {
		steps.add(AnimationStep(tag, loops, guard = guard, effect = effect))
	}

	fun playRandom(
		vararg tags: String,
		loops: Int = 1,
		guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
		effect: StateEffect = StateEffect.NONE
	) {
		steps.add(
			AnimationStep(
				tags.first(),
				loops,
				tags.toList(),
				guard = guard,
				effect = effect
			)
		)
	}

	fun playInfinite(
		tag: String,
		guard: AnimationGuard = AnimationGuard.epochGuard(isInterruptible = true),
		effect: StateEffect = StateEffect.NONE
	) {
		steps.add(AnimationStep(tag, INFINITE, guard = guard, effect = effect))
	}

	fun transition(tag: String, effect: StateEffect = StateEffect.NONE) {
		steps.add(
			AnimationStep(
				tag,
				loops = 1,
				guard = AnimationGuard.transitionGuard(),
				effect = effect
			)
		)
	}

	fun buildWithRequirement(): AnimationSequenceWithRequirement {
		return AnimationSequenceWithRequirement(requirement, steps.toList())
	}
}

fun sequence(builder: AnimationSequenceBuilder.() -> Unit): AnimationSequenceWithRequirement {
	return AnimationSequenceBuilder().apply(builder).buildWithRequirement()
}
