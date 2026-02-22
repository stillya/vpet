package dev.stillya.vpet.editor

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.markup.TextAttributes
import dev.stillya.vpet.graphics.SpriteSheet
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.SwingUtilities
import javax.swing.Timer

@Suppress("UnstableApiUsage")
class PetAnimatedPresentation(
	private val spriteSheet: SpriteSheet,
	private val factory: PresentationFactory,
	private val editor: Editor
) : InlayPresentation {

	private val currentFrameIndex = AtomicInteger(0)
	private val frameImages: List<Image>
	private var animationTimer: Timer? = null

	override val width: Int
	override val height: Int

	companion object {
		private const val FRAME_RATE_MS = 100
	}

	init {
		frameImages = extractFrames()
		if (frameImages.isNotEmpty()) {
			val firstFrame = frameImages[0]
			width = firstFrame.getWidth(null)
			height = firstFrame.getHeight(null)
		} else {
			width = 16
			height = 16
		}
		startAnimation()
	}

	private fun extractFrames(): List<Image> {
		return spriteSheet.frames.map { atlasFrame ->
			val f = atlasFrame.frame
			val sourceImage = spriteSheet.image

			if (sourceImage is BufferedImage) {
				sourceImage.getSubimage(f.x, f.y, f.width, f.height)
			} else {
				val tempImage = BufferedImage(f.width, f.height, BufferedImage.TYPE_INT_ARGB)
				val g = tempImage.createGraphics()
				g.drawImage(
					sourceImage,
					0, 0, f.width, f.height,
					f.x, f.y, f.x + f.width, f.y + f.height,
					null
				)
				g.dispose()
				tempImage
			}
		}
	}

	private fun startAnimation() {
		if (animationTimer != null) return

		animationTimer = Timer(FRAME_RATE_MS) {
			if (frameImages.isNotEmpty()) {
				currentFrameIndex.set((currentFrameIndex.get() + 1) % frameImages.size)
				SwingUtilities.invokeLater {
					fireContentChanged(Rectangle(0, 0, width, height))
				}
			}
		}
		animationTimer?.start()
	}

	override fun paint(g: Graphics2D, attributes: TextAttributes) {
		return

//		val frameIndex = currentFrameIndex.get()
//		if (frameIndex >= frameImages.size) return
//
//		val frame = frameImages[frameIndex]
//		g.drawImage(frame, 0, 0, width, height, null)
	}

	override fun toString(): String = "VPet Animation"

	private val listeners = mutableListOf<PresentationListener>()

	override fun addListener(listener: PresentationListener) {
		listeners.add(listener)
	}

	override fun removeListener(listener: PresentationListener) {
		listeners.remove(listener)
	}

	override fun fireContentChanged(area: Rectangle) {
		listeners.forEach { it.contentChanged(area) }
	}

	override fun fireSizeChanged(previous: Dimension, current: Dimension) {
		listeners.forEach { it.sizeChanged(previous, current) }
	}

	override fun mouseClicked(event: MouseEvent, translated: Point) {}
	override fun mouseMoved(event: MouseEvent, translated: Point) {}
	override fun mouseExited() {}
}
