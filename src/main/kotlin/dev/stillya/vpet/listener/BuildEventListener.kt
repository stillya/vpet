package dev.stillya.vpet.listener

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.task.ProjectTaskContext
import com.intellij.task.ProjectTaskListener
import com.intellij.task.ProjectTaskManager
import dev.stillya.vpet.AnimationEventListener
import java.util.concurrent.atomic.AtomicBoolean

class BuildEventListener : ProjectTaskListener, ExecutionListener {

	private val inProgress = AtomicBoolean(false)
	private var lastEventTimestamp = 0L
	private var lastEventType: AnimationEventListener.AnimationEvent? = null

	fun isBuildInProgress(): Boolean = inProgress.get()
	
	private fun publishEvent(event: AnimationEventListener.AnimationEvent) {
		val now = System.currentTimeMillis()

		if (lastEventType == event && now - lastEventTimestamp < 500) {
			return
		}

		lastEventTimestamp = now
		lastEventType = event

		ApplicationManager.getApplication().messageBus
			.syncPublisher(AnimationEventListener.TOPIC)
			.onEvent(event)
	}

	override fun started(context: ProjectTaskContext) {
		if (inProgress.compareAndSet(false, true)) {
			publishEvent(AnimationEventListener.AnimationEvent.PROGRESS)
		}
	}

	override fun finished(result: ProjectTaskManager.Result) {
		if (inProgress.compareAndSet(true, false)) {
			if (result.hasErrors()) {
				publishEvent(AnimationEventListener.AnimationEvent.FAIL)
			} else {
				publishEvent(AnimationEventListener.AnimationEvent.SUCCESS)
			}
		}
	}

	override fun processStarting(
		executorId: String,
		env: ExecutionEnvironment,
		handler: ProcessHandler
	) {
		if (inProgress.compareAndSet(false, true)) {
			publishEvent(AnimationEventListener.AnimationEvent.PROGRESS)
		}
	}

	override fun processTerminated(
		executorId: String,
		env: ExecutionEnvironment,
		handler: ProcessHandler,
		exitCode: Int
	) {
		if (inProgress.compareAndSet(true, false)) {
			if (exitCode == 0) {
				publishEvent(AnimationEventListener.AnimationEvent.SUCCESS)
			} else {
				publishEvent(AnimationEventListener.AnimationEvent.FAIL)
			}
		}
	}
}