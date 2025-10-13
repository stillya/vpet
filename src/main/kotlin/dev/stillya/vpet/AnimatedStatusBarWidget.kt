package dev.stillya.vpet

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IconWidgetPresentation
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WidgetPresentation
import com.intellij.openapi.wm.WidgetPresentationDataContext
import com.intellij.openapi.wm.WidgetPresentationFactory
import com.jetbrains.rd.util.AtomicInteger
import dev.stillya.vpet.graphics.Animated
import dev.stillya.vpet.graphics.DefaultIconRenderer
import dev.stillya.vpet.graphics.PetAnimated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.annotations.Nls
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.Icon

class AnimatedStatusBarWidgetFactory : StatusBarWidgetFactory, WidgetPresentationFactory {
	override fun getId(): String = "AnimatedStatusBarWidget"

	@Nls
	override fun getDisplayName(): String = "Animated Status Bar Widget"

	override fun isAvailable(project: Project): Boolean = true

	override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

	override fun createPresentation(
		context: WidgetPresentationDataContext,
		scope: CoroutineScope
	): WidgetPresentation {
		return AnimatedStatusBarWidget()
	}
}

class AnimatedStatusBarWidget : IconWidgetPresentation {

	private val animation: Animated
		get() = service<PetAnimated>()

	private val iconRenderer: IconRenderer
		get() = service<DefaultIconRenderer>()

	private var curFrameIdx = 0
	private var curFrames = emptyList<Icon>()

	private var timer: Timer? = null

	private val counter = AtomicInteger(0)

	companion object {
		const val DEFAULT_SPRITE_SHEET_IMAGE = "/META-INF/spritesheets/cat.png"
		const val DEFAULT_SPRITE_SHEET_ATLAS = "/META-INF/spritesheets/cat_atlas.json"
		const val COUNTER_LIMIT = 10
		const val FRAME_RATE_MS = 100L
	}

	init {
		initAnimation()
	}

	override fun icon(): Flow<Icon?> =
		flow {
			while (true) {
				if (curFrames.isNotEmpty() && curFrameIdx < curFrames.size) {
					emit(curFrames[curFrameIdx])
					delay(FRAME_RATE_MS)
					curFrameIdx = (curFrameIdx + 1) % curFrames.size
					if (curFrameIdx == 0) {
						try {
							curFrames = iconRenderer.render()
							if (curFrames.isEmpty()) {
								stopAnimation()
								continue
							}
						} catch (_: Exception) {
							stopAnimation()
							continue
						}
					}
				}
			}
		}

	override fun getClickConsumer(): ((MouseEvent) -> Unit)? {
		return {
			if (counter.get() >= COUNTER_LIMIT) {
				counter.set(0)
				animation.onOccasion()
			} else {
				counter.incrementAndGet()
			}
		}
	}

	private fun initAnimation() {
		animation.init(
			Animated.Params(
				atlasPath = DEFAULT_SPRITE_SHEET_ATLAS,
				imgPath = DEFAULT_SPRITE_SHEET_IMAGE
			)
		)

		curFrames = iconRenderer.render()
	}

	private fun stopAnimation() {
		timer?.cancel()
		timer = null
	}
}