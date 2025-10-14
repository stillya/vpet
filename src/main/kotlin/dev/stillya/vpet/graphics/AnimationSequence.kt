package dev.stillya.vpet.graphics

data class AnimationStep(
	val animationTag: String,
	val loops: Int = 1,
	val variants: List<String> = emptyList(),
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
	val isTransition: Boolean = false
)

class AnimationSequenceBuilder {
	private val steps = mutableListOf<AnimationStep>()

	fun play(tag: String, loops: Int = 1, guard: AnimationGuard = AnimationGuard.ALWAYS_VALID) {
		steps.add(AnimationStep(tag, loops, guard = guard))
	}

	fun playRandom(vararg tags: String, loops: Int = 1, guard: AnimationGuard = AnimationGuard.ALWAYS_VALID) {
		steps.add(AnimationStep(tags.first(), loops, tags.toList(), guard = guard))
	}

	fun playInfinite(tag: String, guard: AnimationGuard = AnimationGuard.epochGuard(isInterruptible = true)) {
		steps.add(AnimationStep(tag, INFINITE, guard = guard))
	}

	fun transition(tag: String) {
		steps.add(AnimationStep(tag, loops = 1, guard = AnimationGuard.transitionGuard(), isTransition = true))
	}

	fun build(): List<AnimationStep> = steps.toList()
}

fun sequence(builder: AnimationSequenceBuilder.() -> Unit): List<AnimationStep> {
	return AnimationSequenceBuilder().apply(builder).build()
}
