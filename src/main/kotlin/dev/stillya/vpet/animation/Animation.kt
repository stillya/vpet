package dev.stillya.vpet.animation

import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.SpriteSheet
import java.awt.image.BufferedImage

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
	fun extractFrames(): List<BufferedImage> = sheet.frames.map { atlasFrame ->
		val f = atlasFrame.frame
		val sourceImage = sheet.image
		if (sourceImage is BufferedImage) {
			sourceImage.getSubimage(f.x, f.y, f.width, f.height)
		} else {
			val tempImage = BufferedImage(f.width, f.height, BufferedImage.TYPE_INT_ARGB)
			val g = tempImage.createGraphics()
			g.drawImage(
				sourceImage, 0, 0, f.width, f.height,
				f.x, f.y, f.x + f.width, f.y + f.height, null
			)
			g.dispose()
			tempImage
		}
	}

	val frameCount: Int get() = sheet.frames.size

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
