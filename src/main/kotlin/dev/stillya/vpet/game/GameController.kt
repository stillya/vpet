package dev.stillya.vpet.game

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.stillya.vpet.Animated
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import javax.swing.Timer

@Service(Service.Level.PROJECT)
class GameController(private val project: Project) {

	private var editor: Editor? = null
	private var renderer: GameRenderer? = null
	private var character: Character? = null
	private var gameTimer: Timer? = null
	private var gameDisposable: Disposable? = null
	private val keysHeld = mutableSetOf<Int>()

	private var world = World()
	private var lastTickNanos = 0L
	private var jumpWasPressed = false
	private var tileMapSyncer: TileMapSyncer? = null

	private val resizeListener = object : ComponentAdapter() {
		override fun componentResized(e: ComponentEvent) {
			val cc = e.component
			renderer?.setBounds(0, 0, cc.width, cc.height)
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

		val ch = project.service<Animated>() as Character
		character = ch

		val visibleArea = activeEditor.scrollingModel.visibleArea
		val firstVisibleLine = activeEditor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).line

		val caretPos = activeEditor.caretModel.logicalPosition
		world = World(
			transform = Transform(caretPos.column.toFloat(), firstVisibleLine.toFloat()),
			velocity = Velocity(0f, 0f),
			isOnGround = false,
			sprite = SpriteState(tag = "J_U_D"),
			phase = GamePhase.ENTRANCE
		)

		val syncer = TileMapSyncer(activeEditor)
		Disposer.register(disposable, syncer)
		syncer.start()
		tileMapSyncer = syncer

		val panel = GameRenderer(activeEditor)
		renderer = panel
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
			it.remove(renderer)
			it.removeComponentListener(resizeListener)
			it.repaint()
		}
		renderer = null
		character = null
		tileMapSyncer = null
		keysHeld.clear()
		jumpWasPressed = false
		gameDisposable?.let { Disposer.dispose(it) }
		gameDisposable = null
		editor?.contentComponent?.requestFocusInWindow()
		editor = null
		world = World()
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
		val panel = renderer ?: return
		val ch = character ?: return
		val tileMap = tileMapSyncer?.tileMap ?: return

		val now = System.nanoTime()
		val dt = ((now - lastTickNanos) / 1_000_000_000f).coerceAtMost(0.05f)
		lastTickNanos = now

		val input = gatherInput()

		val visibleArea = activeEditor.scrollingModel.visibleArea
		val firstVisibleLine = activeEditor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).line
		val lastVisibleLine = activeEditor.xyToLogicalPosition(
			java.awt.Point(0, visibleArea.y + visibleArea.height)
		).line.coerceAtMost(activeEditor.document.lineCount - 1)

		val frame = WorldUpdate.tick(world, input, dt, ch, tileMap, firstVisibleLine..lastVisibleLine)
		world = frame.world

		panel.update(frame, tileMap)
		panel.repaint()
	}
}
