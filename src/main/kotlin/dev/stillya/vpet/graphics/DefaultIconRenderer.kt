package dev.stillya.vpet.graphics

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.INFINITE
import java.awt.Image
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Icon
import javax.swing.ImageIcon
import kotlin.math.roundToInt

// TODO: Add caching
@Service(Service.Level.PROJECT)
class DefaultIconRenderer(project: Project) : IconRenderer {
	private val animationQueue: Queue<Animation> = LinkedBlockingQueue()
	private var lastStableAnimation: Animation? = null

	@Volatile
	private var currentAnimation: Animation? = null
	private val currentLoopCount: AtomicInteger = AtomicInteger(0)
	private var scaleValue: Double = 1.2
	private var isFlipped: Boolean = false
	private var verticalOffset: Int = -8

	private val epochManager = AnimationEpochManager()
	
	companion object {
		private val log = logger<DefaultIconRenderer>()

		@JvmStatic
		fun getInstance(project: Project): DefaultIconRenderer {
			return project.getService(DefaultIconRenderer::class.java)
		}
	}

	override fun createAnimationContext(
		trigger: AnimationTrigger,
	): AnimationContext {
		return epochManager.createContext(trigger)
	}

	override fun enqueue(animation: Animation) {
		log.trace("Enqueueing animation: ${animation.name} (loops=${animation.loop}, queueSize=${animationQueue.size + 1})")
		animationQueue.add(animation)
	}

	override fun render(): List<Icon> {
		currentAnimation?.let { current ->
			if (!validateAnimation(current)) {
				log.trace("Animation '${current.name}' no longer valid, finding next")
				currentAnimation = findNextValidAnimation()
				currentAnimation?.let { anim ->
					log.trace("Switched to animation: ${anim.name}")
					currentLoopCount.set(anim.loop)
				}
			} else {
				val count = currentLoopCount.get()
				when {
					count > 0 -> {
						val newCount = currentLoopCount.decrementAndGet()
						if (newCount == 0) {
							log.trace("Animation '${current.name}' loop completed")
							currentAnimation = processNextAnimation(current)
							currentAnimation?.let { nextAnim ->
								log.trace("Next animation: ${nextAnim.name}")
								currentLoopCount.set(nextAnim.loop)
							}
						}
					}

					count == 0 -> {
						log.trace("Animation '${current.name}' finished, processing next")
						currentAnimation = processNextAnimation(current)
						currentAnimation?.let { nextAnim ->
							log.trace("Next animation: ${nextAnim.name}")
							currentLoopCount.set(nextAnim.loop)
						}
					}
					// count < 0 (INFINITE): do nothing, let it loop forever
				}
			}
		} ?: run {
			log.trace("No current animation, finding next from queue")
			currentAnimation = findNextValidAnimation()
			currentAnimation?.let { anim ->
				log.trace("Starting animation: ${anim.name} (loops=${anim.loop})")
				currentLoopCount.set(anim.loop)
			}
		}

		return currentAnimation?.let { doRender(it.sheet) } ?: emptyList()
	}

	override fun setFlipped(flipped: Boolean) {
		isFlipped = flipped
	}

	private fun validateAnimation(animation: Animation): Boolean {
		val context = animation.context ?: return true
		val guard = animation.guard
		val currentEpoch = epochManager.getCurrentEpoch()
		return guard.canContinue(context, currentEpoch)
	}

	private fun canStartAnimation(animation: Animation): Boolean {
		val context = animation.context ?: return true
		val guard = animation.guard
		val currentEpoch = epochManager.getCurrentEpoch()
		return guard.canStart(context, currentEpoch)
	}

	private fun findNextValidAnimation(): Animation? {
		while (animationQueue.isNotEmpty()) {
			val candidate = doPoll()
			if (candidate != null && canStartAnimation(candidate)) {
				log.trace("Found valid animation from queue: ${candidate.name}")
				return candidate
			} else {
				log.trace("Skipped invalid animation: ${candidate?.name}")
			}
		}

		lastStableAnimation?.let { stable ->
			if (canStartAnimation(stable)) {
				log.trace("Returning to stable animation: ${stable.name}")
				return stable
			} else {
				log.trace("Stable animation no longer valid, clearing")
				lastStableAnimation = null
			}
		}

		return null
	}

	private fun processNextAnimation(animation: Animation): Animation? {
		return animation.nextAnimation?.also {
			log.trace("Chain: ${animation.name} → ${it.name}")
		} ?: run {
			log.trace("Invoking onFinish for ${animation.name}")
			animation.onFinish.invoke()
			doPoll()
		}
	}

	private fun doPoll(): Animation? {
		val head = animationQueue.poll()
		head?.let {
			if (it.loop == INFINITE) {
				lastStableAnimation = it
			}
		}

		return head ?: lastStableAnimation
	}

	private fun doRender(sheet: SpriteSheet): List<Icon> {
		return sheet.frames.map {
			val f = it.frame

			val sourceImage = sheet.image

			val frameImage = if (sourceImage is BufferedImage) {
				sourceImage.getSubimage(f.x, f.y, f.width, f.height)
			} else {
				val tempImage =
					BufferedImage(f.width, f.height, BufferedImage.TYPE_INT_ARGB)
				val g = tempImage.createGraphics()
				g.drawImage(
					sourceImage,
					0,
					0,
					f.width,
					f.height,
					f.x,
					f.y,
					f.x + f.width,
					f.y + f.height,
					null
				)
				g.dispose()
				tempImage
			}

			val scaledWidth = (f.width * scaleValue).roundToInt()
			val scaledHeight = (f.height * scaleValue).roundToInt()

			val processedImage = if (isFlipped) {
				val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
				tx.translate(-frameImage.width.toDouble(), 0.0)
				val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)

				val flippedImage = op.filter(frameImage, null)

				flippedImage.getScaledInstance(
					scaledWidth,
					scaledHeight,
					Image.SCALE_DEFAULT
				)
			} else {
				frameImage.getScaledInstance(
					scaledWidth,
					scaledHeight,
					Image.SCALE_DEFAULT
				)
			}
			object : ImageIcon(processedImage) {
				override fun paintIcon(
					c: java.awt.Component?,
					g: java.awt.Graphics,
					x: Int,
					y: Int
				) {
					super.paintIcon(c, g, x, y + verticalOffset)
				}
			}
		}
	}
}