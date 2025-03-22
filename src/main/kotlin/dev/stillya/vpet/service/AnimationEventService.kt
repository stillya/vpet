package dev.stillya.vpet.service

import com.intellij.openapi.components.service
import dev.stillya.vpet.AnimationEventListener
import dev.stillya.vpet.graphics.Animated
import dev.stillya.vpet.graphics.PetAnimated

class AnimationEventService : AnimationEventListener {
	private val animated: Animated
		get() = service<PetAnimated>()

	override fun onEvent(event: AnimationEventListener.AnimationEvent) {
		when (event) {
			AnimationEventListener.AnimationEvent.FAIL -> animated.onFail()
			AnimationEventListener.AnimationEvent.SUCCESS -> animated.onSuccess()
			AnimationEventListener.AnimationEvent.PROGRESS -> animated.onProgress()
		}
	}
}