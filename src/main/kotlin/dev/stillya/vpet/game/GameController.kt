package dev.stillya.vpet.game

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import javax.swing.Timer

@Service(Service.Level.PROJECT)
class GameController(private val project: Project) {

	private var editor: Editor? = null
	private var overlay: GameOverlayPanel? = null
	private var spriteRenderer: GameSpriteRenderer? = null
	private var gameTimer: Timer? = null
	private var gameDisposable: Disposable? = null
	private val keysHeld = mutableSetOf<Int>()

	private var state = GameState()
	private var lastTickNanos = 0L
	private var jumpWasPressed = false
	private val tileMap = GameTileMap()

	private val resizeListener = object : ComponentAdapter() {
		override fun componentResized(e: ComponentEvent) {
			val cc = e.component
			overlay?.setBounds(0, 0, cc.width, cc.height)
		}
	}

	var isGameActive: Boolean = false
		private set

	companion object {
		private const val TICK_MS = 16

		fun getInstance(project: Project): GameController = project.service()
	}

	fun enterGameMode() {
		if (isGameActive) return

		val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
		editor = activeEditor

		val disposable = Disposer.newDisposable("vpet-game")
		gameDisposable = disposable

		val variant = dev.stillya.vpet.settings.VPetSettings.getInstance().catVariant
		spriteRenderer = GameSpriteRenderer(variant)

		val caretPos = activeEditor.caretModel.logicalPosition
		state = GameState(
			colX = caretPos.column.toFloat(),
			lineY = caretPos.line.toFloat(),
			isOnGround = true
		)

		val panel = GameOverlayPanel(activeEditor, spriteRenderer!!)
		overlay = panel
		val cc = activeEditor.contentComponent
		cc.add(panel)
		panel.setBounds(0, 0, cc.width, cc.height)
		cc.addComponentListener(resizeListener)

		registerKeyDispatcher(disposable)

		isGameActive = true
		lastTickNanos = System.nanoTime()

		gameTimer = Timer(TICK_MS) { gameTick() }
		gameTimer?.start()
	}

	fun exitGameMode() {
		isGameActive = false
		gameTimer?.stop()
		gameTimer = null
		editor?.contentComponent?.let {
			it.remove(overlay)
			it.removeComponentListener(resizeListener)
			it.repaint()
		}
		overlay = null
		spriteRenderer = null
		keysHeld.clear()
		jumpWasPressed = false
		gameDisposable?.let { Disposer.dispose(it) }
		gameDisposable = null
		editor?.contentComponent?.requestFocusInWindow()
		editor = null
		state = GameState()
	}

	private fun registerKeyDispatcher(disposable: Disposable) {
		val dispatcher = IdeEventQueue.EventDispatcher { event ->
			if (!isGameActive) return@EventDispatcher false
			if (event !is KeyEvent) return@EventDispatcher false

			when (event.keyCode) {
				KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
				KeyEvent.VK_SPACE, KeyEvent.VK_ESCAPE -> {
					when (event.id) {
						KeyEvent.KEY_PRESSED -> {
							if (event.keyCode == KeyEvent.VK_ESCAPE) {
								exitGameMode()
							} else {
								keysHeld.add(event.keyCode)
							}
						}
						KeyEvent.KEY_RELEASED -> {
							keysHeld.remove(event.keyCode)
						}
					}
					event.consume()
					true
				}
				else -> false
			}
		}
		IdeEventQueue.getInstance().addDispatcher(dispatcher, disposable)
	}

	private fun gatherInput(): InputState {
		val move = when {
			KeyEvent.VK_LEFT in keysHeld && KeyEvent.VK_RIGHT !in keysHeld -> -1
			KeyEvent.VK_RIGHT in keysHeld && KeyEvent.VK_LEFT !in keysHeld -> 1
			else -> 0
		}
		val jumpPressed = KeyEvent.VK_UP in keysHeld || KeyEvent.VK_SPACE in keysHeld
		val justPressed = jumpPressed && !jumpWasPressed
		jumpWasPressed = jumpPressed
		return InputState(move, justPressed)
	}

	private fun gameTick() {
		val activeEditor = editor ?: return
		val panel = overlay ?: return
		val renderer = spriteRenderer ?: return

		val now = System.nanoTime()
		val dt = ((now - lastTickNanos) / 1_000_000_000f).coerceAtMost(0.05f)
		lastTickNanos = now

		val input = gatherInput()

		val visibleArea = activeEditor.scrollingModel.visibleArea
		val firstVisibleLine = activeEditor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).line
		val lastVisibleLine = activeEditor.xyToLogicalPosition(
			java.awt.Point(0, visibleArea.y + visibleArea.height)
		).line.coerceAtMost(activeEditor.document.lineCount - 1)

		tileMap.rebuild(activeEditor, firstVisibleLine, lastVisibleLine)
		state = GameUpdate.update(state, input, dt, tileMap, firstVisibleLine, lastVisibleLine)

		if (state.animState == GameAnimationState.LAND) {
			val frames = renderer.getFrames("Stop", state.facingLeft)
			if (frames.isNotEmpty() && state.frameIndex >= frames.size) {
				state = state.copy(animState = GameAnimationState.IDLE, frameIndex = 0, frameTimer = 0f)
			}
		}

		panel.update(state, tileMap)
		panel.repaint()
	}
}
