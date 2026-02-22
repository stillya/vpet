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
import dev.stillya.vpet.settings.VPetSettings
import dev.stillya.vpet.settings.VPetSettingsListener
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
) : IconWidgetPresentation, ActivityListener, VPetSettingsListener, Disposable {

	private val animation: Animated
		get() = project.service<Animated>()

	private val iconRenderer: IconRenderer
		get() = project.service<IconRenderer>()

	// NOTE: afaik icon flow of widget presentation is always collected from a coroutine backed by single thread executor(cause UI)
	// so no need to synchronize access to curFrameIdx and curFrames, but it could flake out
	private var curFrameIdx = 0
	private var curFrames = emptyList<Icon>()

	private val counter = AtomicInteger(0)

	@Volatile
	private var lastActivityTimeMs: Long = System.currentTimeMillis()

	companion object {
		const val COUNTER_LIMIT = 10
		const val FRAME_RATE_MS = 100L
		const val INACTIVITY_THRESHOLD_MS = 10 * 60_000L // 10 minutes
		const val CURSOR_CHECK_INTERVAL_MS = 500L
		const val INACTIVITY_CHECK_INTERVAL_MS = 5_000L // 5 seconds
	}

	init {
		ActivityTracker.getInstance(project).registerListener(this)
		project.messageBus.connect(this).subscribe(VPetSettings.TOPIC, this)
		initAnimation()
		curFrames = iconRenderer.render()
		startCursorTracking()
		startInactivityMonitoring()
	}

	override fun icon(): Flow<Icon?> = flow {
		while (true) {
			val controller = dev.stillya.vpet.game.GameController.getInstance(project)
			if (controller.isGameActive) {
				emit(null)
				delay(FRAME_RATE_MS)
				continue
			}

			emit(curFrames[curFrameIdx])
			delay(FRAME_RATE_MS)

			curFrameIdx = (curFrameIdx + 1) % curFrames.size

			if (curFrameIdx == 0) {
				curFrames = awaitNonEmptyFrames()
			}
		}
	}

	private suspend fun awaitNonEmptyFrames(): List<Icon> {
		while (true) {
			try {
				val frames = iconRenderer.render()
				if (frames.isNotEmpty()) {
					return frames
				}
			} catch (_: Exception) {
			}
			delay(50L) // actually there should be no more than a one iteration but just in case to prevent tight loop
		}
	}

	override fun getClickConsumer(): (MouseEvent) -> Unit {
		return { event ->
			if (event.isMetaDown || event.isControlDown) {
				val controller = dev.stillya.vpet.game.GameController.getInstance(project)
				if (controller.isGameActive) controller.exitGameMode()
				else controller.enterGameMode()
			} else {
				if (counter.get() >= COUNTER_LIMIT) {
					counter.set(0)
					animation.onOccasion()
				} else {
					counter.incrementAndGet()
				}
			}
		}
	}

	private fun initAnimation() {
		val settings = VPetSettings.getInstance()
		val variant = settings.catVariant

		animation.init(
			Animated.Params(
				atlasPath = variant.atlasPath,
				imgPath = variant.imagePath
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

	override fun settingsChanged(settings: VPetSettings) {
		initAnimation()
		curFrames = iconRenderer.render()
		curFrameIdx = 0
	}

	override fun onActivity() {
		lastActivityTimeMs = System.currentTimeMillis()
	}

	override fun dispose() {
		ActivityTracker.getInstance(project).unregisterListener(this)
	}
}