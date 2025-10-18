package dev.stillya.vpet.graphics

class SequenceRegistry {
	private val sequences = mutableMapOf<String, AnimationSequenceWithRequirement>()

	fun register(name: String, sequenceWithRequirement: AnimationSequenceWithRequirement) {
		sequences[name] = sequenceWithRequirement
	}

	fun register(name: String, builder: AnimationSequenceBuilder.() -> Unit) {
		val seq = AnimationSequenceBuilder().apply(builder).buildWithRequirement()
		sequences[name] = seq
	}

	fun get(name: String): AnimationSequenceWithRequirement? {
		return sequences[name]
	}

	fun getOrThrow(name: String): AnimationSequenceWithRequirement {
		return sequences[name] ?: throw IllegalArgumentException("Sequence '$name' not found")
	}

	fun getAllNames(): Set<String> = sequences.keys.toSet()

	fun clear() {
		sequences.clear()
	}
}
