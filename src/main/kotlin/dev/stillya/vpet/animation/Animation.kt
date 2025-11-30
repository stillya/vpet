package dev.stillya.vpet.animation

import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.SpriteSheet

data class Animation(
	val name: String,
	val sheet: SpriteSheet,
	val loop: Int = 0,
	val onFinish: () -> Unit,
	var nextAnimation: Animation? = null,
	val context: AnimationContext? = null,
	val guard: AnimationGuard = AnimationGuard.ALWAYS_VALID,
	val state: AnimationState
) {
	companion object {
		fun empty(onFinish: () -> Unit = {}) = Animation(
			name = "empty",
			sheet = SpriteSheet(
				image = EmptyImage,
				frames = emptyList()
			),
			loop = 0,
			onFinish = onFinish,
			nextAnimation = null,
			context = null,
			guard = AnimationGuard.ALWAYS_VALID,
			state = AnimationState.IDLE
		)

		internal object EmptyImage : java.awt.Image() {
			override fun getWidth(observer: java.awt.image.ImageObserver?): Int = 1
			override fun getHeight(observer: java.awt.image.ImageObserver?): Int = 1
			override fun getSource(): java.awt.image.ImageProducer = throw UnsupportedOperationException()
			override fun getGraphics(): java.awt.Graphics = throw UnsupportedOperationException()
			override fun getProperty(name: String?, observer: java.awt.image.ImageObserver?): Any? = null
		}
	}
}

enum class AnimationState {
	IDLE,
	WALKING,
	RUNNING,
	CELEBRATING,
	FAILED,
	OCCASION,
	OBSERVING
}
