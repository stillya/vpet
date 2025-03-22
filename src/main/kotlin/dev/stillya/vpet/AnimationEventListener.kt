package dev.stillya.vpet

import com.intellij.util.messages.Topic

interface AnimationEventListener {

	fun onEvent(event: AnimationEvent)

	enum class AnimationEvent {
		FAIL,
		SUCCESS,
		PROGRESS,
	}

	companion object {
		val TOPIC = Topic.create("Animation Event", AnimationEventListener::class.java)
	}
}