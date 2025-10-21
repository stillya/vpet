package dev.stillya.vpet.service

import com.intellij.openapi.project.Project
import dev.stillya.vpet.AnimationEventListener
import dev.stillya.vpet.pet.Animated
import dev.stillya.vpet.pet.PetAnimated

class AnimationEventService(private val project: Project) : AnimationEventListener {
	// TODO: Don't hardcode specific implementation
	private val animated: Animated
		get() = PetAnimated.getInstance(project)

	override fun onEvent(event: AnimationEventListener.AnimationEvent) {
		ActivityTracker.notifyActivity()
		when (event) {
			AnimationEventListener.AnimationEvent.FAIL -> animated.onFail()
			AnimationEventListener.AnimationEvent.SUCCESS -> animated.onSuccess()
			AnimationEventListener.AnimationEvent.PROGRESS -> animated.onProgress()
		}
	}
}

object ActivityTracker {
	private val listeners = mutableListOf<ActivityListener>()

	fun registerListener(listener: ActivityListener) {
		listeners.add(listener)
	}

	fun unregisterListener(listener: ActivityListener) {
		listeners.remove(listener)
	}

	fun notifyActivity() {
		listeners.forEach { it.onActivity() }
	}
}

interface ActivityListener {
	fun onActivity()
}