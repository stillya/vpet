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

	override fun started(context: ProjectTaskContext) {
		inProgress.set(true)
		ApplicationManager.getApplication().messageBus
			.syncPublisher(AnimationEventListener.TOPIC)
			.onEvent(AnimationEventListener.AnimationEvent.PROGRESS)
	}

	override fun finished(result: ProjectTaskManager.Result) {
		inProgress.set(false)

		if (result.hasErrors()) {
			ApplicationManager.getApplication().messageBus
				.syncPublisher(AnimationEventListener.TOPIC)
				.onEvent(AnimationEventListener.AnimationEvent.FAIL)
		} else {
			ApplicationManager.getApplication().messageBus
				.syncPublisher(AnimationEventListener.TOPIC)
				.onEvent(AnimationEventListener.AnimationEvent.SUCCESS)
		}
	}

	override fun processStarting(
		executorId: String,
		env: ExecutionEnvironment,
		handler: ProcessHandler
	) {
		inProgress.set(true)
		ApplicationManager.getApplication().messageBus
			.syncPublisher(AnimationEventListener.TOPIC)
			.onEvent(AnimationEventListener.AnimationEvent.PROGRESS)
	}

	override fun processTerminated(
		executorId: String,
		env: ExecutionEnvironment,
		handler: ProcessHandler,
		exitCode: Int
	) {
		inProgress.set(false)

		if (exitCode == 0) {
			ApplicationManager.getApplication().messageBus
				.syncPublisher(AnimationEventListener.TOPIC)
				.onEvent(AnimationEventListener.AnimationEvent.SUCCESS)
		} else {
			ApplicationManager.getApplication().messageBus
				.syncPublisher(AnimationEventListener.TOPIC)
				.onEvent(AnimationEventListener.AnimationEvent.FAIL)
		}
	}
}