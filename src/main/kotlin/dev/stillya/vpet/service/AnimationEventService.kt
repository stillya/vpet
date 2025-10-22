package dev.stillya.vpet.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.stillya.vpet.Animated
import dev.stillya.vpet.AnimationEventListener

class AnimationEventService(private val project: Project) : AnimationEventListener {
	private val animated: Animated
		get() = project.service<Animated>()

	override fun onEvent(event: AnimationEventListener.AnimationEvent) {
		ActivityTracker.getInstance(project).notifyActivity()
		when (event) {
			AnimationEventListener.AnimationEvent.FAIL -> animated.onFail()
			AnimationEventListener.AnimationEvent.SUCCESS -> animated.onSuccess()
			AnimationEventListener.AnimationEvent.PROGRESS -> animated.onProgress()
		}
	}
}

@Service(Service.Level.PROJECT)
class ActivityTracker {
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

	companion object {
		@JvmStatic
		fun getInstance(project: Project): ActivityTracker = project.service<ActivityTracker>()
	}
}

interface ActivityListener {
	fun onActivity()
}