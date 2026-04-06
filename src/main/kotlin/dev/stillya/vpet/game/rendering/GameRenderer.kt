package dev.stillya.vpet.game.rendering

import com.intellij.openapi.editor.Editor
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.game.GameFrame
import dev.stillya.vpet.game.VirtualTileMap
import dev.stillya.vpet.game.ecs.World
import dev.stillya.vpet.settings.VPetSettings
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JComponent

class GameRenderer(
	editor: Editor
) : JComponent() {

	private var world = World()
	private var tileMap: VirtualTileMap? = null
	private var currentBounds: IntRange = 0..0
	private var currentAnimation: Animation? = null
	private val renderSystem = RenderSystem(editor)

	init {
		isOpaque = false
	}

	fun update(frame: GameFrame, animation: Animation, tileMap: VirtualTileMap) {
		this.world = frame.world
		this.tileMap = tileMap
		this.currentBounds = frame.bounds
		this.currentAnimation = animation
	}

	override fun contains(x: Int, y: Int): Boolean = false

	override fun paintComponent(g: Graphics) {
		val g2d = g as Graphics2D
		val animation = currentAnimation ?: return
		val map = tileMap ?: return

		renderSystem.render(g2d, world, animation, currentBounds)
		if (VPetSettings.getInstance().debugRenderEnabled) {
			renderSystem.renderDebug(g2d, world, map, currentBounds)
		}
	}
}
