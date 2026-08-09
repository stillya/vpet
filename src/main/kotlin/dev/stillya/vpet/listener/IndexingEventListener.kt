package dev.stillya.vpet.listener

import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import dev.stillya.vpet.AnimationEventListener

class IndexingEventListener(private val project: Project) : DumbService.DumbModeListener {

	override fun enteredDumbMode() = publish(AnimationEventListener.AnimationEvent.INDEXING_START)

	override fun exitDumbMode() = publish(AnimationEventListener.AnimationEvent.INDEXING_FINISH)

	private fun publish(event: AnimationEventListener.AnimationEvent) =
		project.messageBus.syncPublisher(AnimationEventListener.TOPIC).onEvent(event)
}
