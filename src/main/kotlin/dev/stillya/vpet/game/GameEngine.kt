package dev.stillya.vpet.game

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Disposer
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import javax.swing.Timer

class GameEngine(
	private val editor: Editor?,
	private val character: Character?,
	private val renderer: GameRenderer?,
	private val onExit: () -> Unit,
) {
	private var world: World = World()
	private var timer: Timer? = null
	private var tileMapSyncer: TileMapSyncer? = null

	private var bugsSpawned = false
	private var lastTickNanos = 0L
	private var jumpWasPressed = false
	private val keysHeld = mutableSetOf<Int>()

	private val resizeListener = object : ComponentAdapter() {
		override fun componentResized(e: ComponentEvent) {
			val cc = e.component
			renderer?.setBounds(0, 0, cc.width, cc.height)
		}
	}

	companion object {
		private const val TICK_MS = 16
		private val LOG = logger<GameEngine>()
	}

	fun start(initialWorld: World, disposable: Disposable) {
		require(timer == null) { "GameEngine already started" }
		requireNotNull(editor) { "GameEngine requires a non-null editor" }
		requireNotNull(renderer) { "GameEngine requires a non-null renderer" }
		requireNotNull(character) { "GameEngine requires a non-null character" }

		world = initialWorld

		val syncer = TileMapSyncer(editor)
		Disposer.register(disposable, syncer)
		syncer.start()
		tileMapSyncer = syncer

		val cc = editor.contentComponent
		cc.add(renderer)
		renderer.setBounds(0, 0, cc.width, cc.height)
		cc.addComponentListener(resizeListener)

		registerKeyDispatcher(disposable)

		lastTickNanos = System.nanoTime()
		timer = Timer(TICK_MS) { tick() }
		timer?.start()
	}

	fun stop() {
		timer?.stop()
		timer = null
		keysHeld.clear()
		jumpWasPressed = false
		bugsSpawned = false
		tileMapSyncer = null
		val cc = editor?.contentComponent ?: return
		cc.remove(renderer)
		cc.removeComponentListener(resizeListener)
		cc.repaint()
		cc.requestFocusInWindow()
	}

	private fun tick() {
		val tileMap = tileMapSyncer?.tileMap ?: return

		val now = System.nanoTime()
		val dt = ((now - lastTickNanos) / 1_000_000_000f).coerceAtMost(0.05f)
		lastTickNanos = now

		val input = gatherInput()

		val lastDocumentLine = ((editor?.document?.lineCount ?: 1) - 1).coerceAtLeast(0)
		val visibleRange = 0..lastDocumentLine

		if (!bugsSpawned) {
			BugSpawner.spawnBugs(world.registry, tileMap, visibleRange)
			bugsSpawned = true
		}

		try {
			val (frame, intent) = WorldUpdate.tick(world, input, dt, character ?: return, tileMap, visibleRange)
			world = frame.world

			renderer?.update(frame, intent.animation, tileMap)
			renderer?.repaint()
		} catch (e: Exception) {
			LOG.error("Game tick failed, stopping game loop", e)
			onExit()
		}
	}

	fun gatherInput(): InputState {
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

	private fun registerKeyDispatcher(disposable: Disposable) {
		val dispatcher = IdeEventQueue.EventDispatcher { event ->
			if (timer == null) return@EventDispatcher false
			if (event !is KeyEvent) return@EventDispatcher false

			when (event.keyCode) {
				KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
				KeyEvent.VK_SPACE, KeyEvent.VK_ESCAPE -> {
					when (event.id) {
						KeyEvent.KEY_PRESSED -> {
							if (event.keyCode == KeyEvent.VK_ESCAPE) {
								onExit()
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
}
