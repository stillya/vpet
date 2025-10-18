package dev.stillya.vpet.graphics

data class AnimationStep(
	val animationTag: String,
	val loops: Int = 1,
	val variants: List<String> = emptyList(),
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
	val isTransition: Boolean = false,
	val effect: StateEffect = StateEffect.NONE
)

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
		steps.add(AnimationStep(tags.first(), loops, tags.toList(), guard = guard, effect = effect))
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
				isTransition = true,
				effect = effect
			)
		)
	}

	fun build(): List<AnimationStep> = steps.toList()

	fun buildWithRequirement(): AnimationSequenceWithRequirement {
		return AnimationSequenceWithRequirement(requirement, steps.toList())
	}
}

fun sequence(builder: AnimationSequenceBuilder.() -> Unit): List<AnimationStep> {
	return AnimationSequenceBuilder().apply(builder).build()
}
