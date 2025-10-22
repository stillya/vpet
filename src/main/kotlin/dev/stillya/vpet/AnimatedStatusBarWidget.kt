package dev.stillya.vpet

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IconWidgetPresentation
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WidgetPresentation
import com.intellij.openapi.wm.WidgetPresentationDataContext
import com.intellij.openapi.wm.WidgetPresentationFactory
import dev.stillya.vpet.service.ActivityListener
import dev.stillya.vpet.service.ActivityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Nls
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.Icon

@ApiStatus.Internal
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
		return AnimatedStatusBarWidget(context.project, scope)
	}
}

class AnimatedStatusBarWidget(
	private val project: Project,
	private val scope: CoroutineScope
) : IconWidgetPresentation, ActivityListener, Disposable {

	private val animation: Animated
		get() = project.service<Animated>()

	private val iconRenderer: IconRenderer
		get() = project.service<IconRenderer>()

	private var curFrameIdx = 0
	private var curFrames = emptyList<Icon>()

	private val counter = AtomicInteger(0)

	@Volatile
	private var lastActivityTimeMs: Long = System.currentTimeMillis()

	companion object {
		const val DEFAULT_SPRITE_SHEET_IMAGE = "/META-INF/spritesheets/cat.png"
		const val DEFAULT_SPRITE_SHEET_ATLAS = "/META-INF/spritesheets/cat_atlas.json"
		const val COUNTER_LIMIT = 10
		const val FRAME_RATE_MS = 100L
		const val INACTIVITY_THRESHOLD_MS = 10 * 60_000L // 10 minutes
		const val CURSOR_CHECK_INTERVAL_MS = 500L
		const val INACTIVITY_CHECK_INTERVAL_MS = 5_000L // 5 seconds
	}

	init {
		ActivityTracker.getInstance(project).registerListener(this)
		initAnimation()
		curFrames = iconRenderer.render()
		startCursorTracking()
		startInactivityMonitoring()
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
								continue
							}
						} catch (_: Exception) {
							continue
						}
					}
				}
			}
		}

	override fun getClickConsumer(): (MouseEvent) -> Unit {
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

	private fun startCursorTracking() {
		scope.launch {
			while (isActive) {
				try {
					val mouseLocation = MouseInfo.getPointerInfo()?.location
					if (mouseLocation != null) {
						val screenSize = Toolkit.getDefaultToolkit().screenSize
						val isOnLeftSide = mouseLocation.x < screenSize.width / 2
						animation.onCursorMove(isOnLeftSide)
					}
				} catch (_: Exception) {
					// Ignore cursor tracking errors
				}
				delay(CURSOR_CHECK_INTERVAL_MS)
			}
		}
	}

	private fun startInactivityMonitoring() {
		scope.launch {
			while (isActive) {
				val inactiveTimeMs = System.currentTimeMillis() - lastActivityTimeMs
				if (inactiveTimeMs >= INACTIVITY_THRESHOLD_MS) {
					animation.onStartObserving()
				}
				delay(INACTIVITY_CHECK_INTERVAL_MS)
			}
		}
	}

	override fun onActivity() {
		lastActivityTimeMs = System.currentTimeMillis()
	}

	override fun dispose() {
		ActivityTracker.getInstance(project).unregisterListener(this)
	}
}