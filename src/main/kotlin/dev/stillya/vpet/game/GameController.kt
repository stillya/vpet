package dev.stillya.vpet.game

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import dev.stillya.vpet.Animated

@Service(Service.Level.PROJECT)
class GameController(private val project: Project) {

	private var engine: GameEngine? = null
	private var gameDisposable: Disposable? = null
	private var activeGame: Game? = null

	var isGameActive: Boolean = false
		private set

	companion object {
		fun getInstance(project: Project): GameController = project.service()
	}

	fun enterGameMode() {
		if (isGameActive) return

		val activeEditor = FileEditorManager.getInstance(project).selectedTextEditor ?: return

		val disposable = Disposer.newDisposable("vpet-game")
		gameDisposable = disposable

		val animated = project.service<Animated>()
		if (animated !is Character || animated !is Game) {
			Disposer.dispose(disposable)
			gameDisposable = null
			return
		}

		val world = buildInitialWorld(activeEditor)

		val renderer = GameRenderer(activeEditor)

		val gameEngine = GameEngine(
			editor = activeEditor,
			character = animated,
			renderer = renderer,
			onExit = { exitGameMode() },
		)

		engine = gameEngine
		activeGame = animated
		var gameStarted = false
		try {
			animated.onGameStart()
			gameStarted = true
			gameEngine.start(world, disposable)
			isGameActive = true
		} catch (e: Exception) {
			gameEngine.stop()
			if (gameStarted) animated.onGameStop()
			engine = null
			activeGame = null
			Disposer.dispose(disposable)
			gameDisposable = null
			throw e
		}
	}

	fun exitGameMode() {
		if (!isGameActive) return
		isGameActive = false
		val engineToStop = engine
		val gameToStop = activeGame
		engine = null
		activeGame = null
		try {
			engineToStop?.stop()
		} finally {
			try {
				gameToStop?.onGameStop()
			} finally {
				gameDisposable?.let { Disposer.dispose(it) }
				gameDisposable = null
			}
		}
	}

	private fun buildInitialWorld(editor: Editor): World {
		val visibleArea = editor.scrollingModel.visibleArea
		val firstVisibleLine = editor.xyToLogicalPosition(java.awt.Point(0, visibleArea.y)).line

		val caretPos = editor.caretModel.logicalPosition
		val mapper = VisualColumnMapper(editor)
		val caretPixelX = editor.logicalPositionToXY(
			LogicalPosition(caretPos.line, caretPos.column)
		).x
		val visualCol = mapper.toVisualColF(caretPixelX)

		val registry = EntityRegistry()
		val player = registry.create()
		registry.add(player, Transform(visualCol, firstVisibleLine.toFloat()))
		registry.add(player, Velocity(0f, 0f))
		registry.add(player, PhysicsState(isOnGround = false))
		registry.add(player, SpriteState())
		registry.add(player, PhaseState(GamePhase.ENTRANCE))
		registry.add(player, AABB(2, 2))
		return World(registry = registry, player = player)
	}
}
