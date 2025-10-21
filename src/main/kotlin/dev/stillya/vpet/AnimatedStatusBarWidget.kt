package dev.stillya.vpet

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IconWidgetPresentation
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WidgetPresentation
import com.intellij.openapi.wm.WidgetPresentationDataContext
import com.intellij.openapi.wm.WidgetPresentationFactory
import dev.stillya.vpet.graphics.DefaultIconRenderer
import dev.stillya.vpet.pet.Animated
import dev.stillya.vpet.pet.PetAnimated
import dev.stillya.vpet.service.ActivityListener
import dev.stillya.vpet.service.ActivityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.annotations.Nls
import java.awt.MouseInfo
import java.awt.Toolkit
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
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
		return AnimatedStatusBarWidget(context.project)
	}
}

class AnimatedStatusBarWidget(private val project: Project) : IconWidgetPresentation, ActivityListener {

	private val animation: Animated
		get() = PetAnimated.getInstance(project)

	private val iconRenderer: IconRenderer
		get() = DefaultIconRenderer.getInstance(project)

	private var curFrameIdx = 0
	private var curFrames = emptyList<Icon>()

	private var timer: Timer? = null

	private val counter = AtomicInteger(0)

	@Volatile
	private var lastActivityTimeMs: Long = System.currentTimeMillis()

	private var cursorTrackingTimer: Timer? = null
	private var inactivityTimer: Timer? = null

	companion object {
		const val DEFAULT_SPRITE_SHEET_IMAGE = "/META-INF/spritesheets/cat.png"
		const val DEFAULT_SPRITE_SHEET_ATLAS = "/META-INF/spritesheets/cat_atlas.json"
		const val COUNTER_LIMIT = 10
		const val FRAME_RATE_MS = 100L
		const val INACTIVITY_THRESHOLD_MS = 10 * 60_000L // 10 minutes
		const val CURSOR_CHECK_INTERVAL_MS = 500L
		const val INACTIVITY_CHECK_INTERVAL_MS = 5_000L // Check every second
	}

	init {
		ActivityTracker.registerListener(this)
		initAnimation()
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

	private fun stopAnimation() {
		timer?.cancel()
		timer = null
	}

	private fun startCursorTracking() {
		cursorTrackingTimer?.cancel()
		cursorTrackingTimer = Timer().apply {
			scheduleAtFixedRate(object : TimerTask() {
				override fun run() {
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
				}
			}, CURSOR_CHECK_INTERVAL_MS, CURSOR_CHECK_INTERVAL_MS)
		}
	}

	private fun startInactivityMonitoring() {
		inactivityTimer?.cancel()
		inactivityTimer = Timer().apply {
			scheduleAtFixedRate(object : java.util.TimerTask() {
				override fun run() {
					val inactiveTimeMs = System.currentTimeMillis() - lastActivityTimeMs
					if (inactiveTimeMs >= INACTIVITY_THRESHOLD_MS) {
						animation.onStartObserving()
					}
				}
			}, INACTIVITY_CHECK_INTERVAL_MS, INACTIVITY_CHECK_INTERVAL_MS)
		}
	}

	override fun onActivity() {
		lastActivityTimeMs = System.currentTimeMillis()
	}
}