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
		when (event) {
			AnimationEventListener.AnimationEvent.FAIL -> {
				notifyActivity()
				animated.onFail()
			}

			AnimationEventListener.AnimationEvent.SUCCESS -> {
				notifyActivity()
				animated.onSuccess()
			}

			AnimationEventListener.AnimationEvent.PROGRESS -> {
				notifyActivity()
				animated.onProgress()
			}

			AnimationEventListener.AnimationEvent.INDEXING_START -> animated.onIndexingStart()
			AnimationEventListener.AnimationEvent.INDEXING_FINISH -> animated.onIndexingFinish()
		}
	}

	private fun notifyActivity() = ActivityTracker.getInstance(project).notifyActivity()
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