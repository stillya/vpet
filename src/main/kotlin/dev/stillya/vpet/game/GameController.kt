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
	private var physics = GamePhysics()
	private var gameTimer: Timer? = null
	private var gameDisposable: Disposable? = null
	private val keysHeld = mutableSetOf<Int>()
	private var jumpConsumed = false

	private var currentAnimState = GameAnimationState.IDLE
	private var frameIndex = 0
	private var frameTick = 0

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
		private const val FRAME_ADVANCE_TICKS = 6

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

		physics = GamePhysics()

		val caretPos = activeEditor.caretModel.logicalPosition
		physics.colX = caretPos.column.toFloat()
		physics.lineY = caretPos.line.toFloat()
		physics.isOnGround = true

		val panel = GameOverlayPanel(activeEditor, spriteRenderer!!)
		overlay = panel
		val cc = activeEditor.contentComponent
		cc.add(panel)
		panel.setBounds(0, 0, cc.width, cc.height)
		cc.addComponentListener(resizeListener)

		registerKeyDispatcher(disposable)

		isGameActive = true

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
		jumpConsumed = false
		gameDisposable?.let { Disposer.dispose(it) }
		gameDisposable = null
		editor?.contentComponent?.requestFocusInWindow()
		editor = null
		currentAnimState = GameAnimationState.IDLE
		frameIndex = 0
		frameTick = 0
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

	private fun gameTick() {
		val activeEditor = editor ?: return
		val panel = overlay ?: return

		processInput()

		val visibleArea = activeEditor.scrollingModel.visibleArea
		val firstVisibleLine = activeEditor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).line
		val lastVisibleLine = activeEditor.xyToLogicalPosition(
			java.awt.Point(0, visibleArea.y + visibleArea.height)
		).line.coerceAtMost(activeEditor.document.lineCount - 1)

		physics.update(activeEditor, firstVisibleLine, lastVisibleLine)

		updateAnimationState()
		advanceFrame()

		panel.colX = physics.colX
		panel.lineY = physics.lineY
		panel.facingLeft = physics.facingLeft
		panel.animState = currentAnimState
		panel.frameIndex = frameIndex
		panel.tileMap = physics.tileMap
		panel.isOnGround = physics.isOnGround
		panel.repaint()
	}

	private fun processInput() {
		physics.inputDirection = when {
			KeyEvent.VK_LEFT in keysHeld && KeyEvent.VK_RIGHT !in keysHeld -> -1
			KeyEvent.VK_RIGHT in keysHeld && KeyEvent.VK_LEFT !in keysHeld -> 1
			else -> 0
		}
		val jumpPressed = KeyEvent.VK_UP in keysHeld || KeyEvent.VK_SPACE in keysHeld
		if (jumpPressed && !jumpConsumed && physics.isOnGround) {
			physics.jump()
			jumpConsumed = true
		}
		if (!jumpPressed) {
			jumpConsumed = false
		}
	}

	private fun updateAnimationState() {
		val newState = when {
			!physics.isOnGround && physics.velocityY < 0 -> GameAnimationState.JUMP_UP
			!physics.isOnGround && physics.velocityY >= 0 -> GameAnimationState.JUMP_DOWN
			currentAnimState == GameAnimationState.JUMP_DOWN && physics.isOnGround -> GameAnimationState.LAND
			kotlin.math.abs(physics.velocityX) > 0.05f -> GameAnimationState.WALK
			else -> GameAnimationState.IDLE
		}

		if (newState != currentAnimState) {
			currentAnimState = newState
			frameIndex = 0
			frameTick = 0
		}
	}

	private fun advanceFrame() {
		frameTick++
		if (frameTick >= FRAME_ADVANCE_TICKS) {
			frameTick = 0
			val frames = spriteRenderer?.getFrames(currentAnimState.spriteTag, physics.facingLeft)
			if (!frames.isNullOrEmpty()) {
				if (currentAnimState.loops) {
					frameIndex = (frameIndex + 1) % frames.size
				} else {
					if (frameIndex < frames.size - 1) {
						frameIndex++
					} else if (currentAnimState == GameAnimationState.LAND) {
						currentAnimState = GameAnimationState.IDLE
						frameIndex = 0
					}
				}
			}
		}
	}
}
