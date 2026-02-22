package dev.stillya.vpet.game

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.swing.JComponent
import kotlin.math.floor

class GameRenderer(
	private val editor: Editor,
	private val character: Character
) : JComponent() {

	private var world = World()
	private var tileMap: VirtualTileMap? = null
	private val frameCache = mutableMapOf<String, List<BufferedImage>>()
	private val flippedFrameCache = mutableMapOf<String, List<BufferedImage>>()
	private var frameCount = 0
	private var lastFpsTime = System.nanoTime()
	private var fps = 0

	init {
		isOpaque = false
	}

	fun update(world: World, tileMap: VirtualTileMap) {
		this.world = world
		this.tileMap = tileMap
	}

	override fun contains(x: Int, y: Int): Boolean = false

	private fun logicalToX(line: Int, col: Int): Int =
		editor.logicalPositionToXY(LogicalPosition(line, col)).x

	private fun logicalToX(line: Int, col: Float): Int {
		val colInt = floor(col).toInt()
		val frac = col - colInt
		val x0 = logicalToX(line, colInt)
		val x1 = logicalToX(line, colInt + 1)
		return x0 + ((x1 - x0) * frac).toInt()
	}

	override fun paintComponent(g: Graphics) {
		val g2d = g as Graphics2D
		g2d.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
		)

		val lineHeight = editor.lineHeight
		val spriteSize = lineHeight * 2

		val groundLine = world.displayLine
		val lineFrac = world.transform.y - groundLine
		val groundYBase = editor.logicalPositionToXY(LogicalPosition(groundLine, 0)).y
		val groundY = groundYBase + (lineFrac * lineHeight).toInt()

		drawDebug(g2d, lineHeight, groundLine, groundY)

		val tag = world.sprite.tag
		val frames = if (world.sprite.direction.isLeft()) {
			flippedFrameCache.getOrPut(tag) {
				val animation = character.getAnimation(tag) ?: return
				animation.extractFrames().map { flipImage(it) }
			}
		} else {
			frameCache.getOrPut(tag) {
				val animation = character.getAnimation(tag) ?: return
				animation.extractFrames()
			}
		}
		if (frames.isEmpty()) return

		val idx = if (character.isLooping(world.sprite.tag)) world.sprite.frameIndex % frames.size
		else world.sprite.frameIndex.coerceAtMost(frames.size - 1)
		val frame = frames[idx]

		val bounds = character.debugBounds(world.transform)
		val hitboxX = logicalToX(groundLine, world.transform.x)
		val hitboxEndX = logicalToX(groundLine, world.transform.x + (bounds.last - bounds.first + 1))
		val hitboxCenterX = (hitboxX + hitboxEndX) / 2
		val pixelX = hitboxCenterX - spriteSize / 2
		val spriteY = groundY - spriteSize

		g2d.drawImage(frame, pixelX, spriteY, spriteSize, spriteSize, null)

		frameCount++
		val now = System.nanoTime()
		if (now - lastFpsTime >= 1_000_000_000L) {
			fps = frameCount
			frameCount = 0
			lastFpsTime = now
		}
		g2d.color = Color.WHITE
		g2d.font = g2d.font.deriveFont(11f)
		g2d.drawString("FPS: $fps", 4, 14)
	}

	private fun flipImage(image: BufferedImage): BufferedImage {
		val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
		tx.translate(-image.width.toDouble(), 0.0)
		return AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null)
	}

	private fun drawDebug(g2d: Graphics2D, lineHeight: Int, groundLine: Int, groundY: Int) {
		val map = tileMap ?: return
		val oldStroke = g2d.stroke

		g2d.stroke = BasicStroke(1f)
		map.forEachExtent { line, extent ->
			val y = editor.logicalPositionToXY(LogicalPosition(line, 0)).y
			val x = logicalToX(line, extent.first)
			val endX = logicalToX(line, extent.last + 1)
			g2d.color = Color(0, 255, 0, 30)
			g2d.fillRect(x, y, endX - x, lineHeight)
			g2d.color = Color(0, 255, 0, 120)
			g2d.drawRect(x, y, endX - x, lineHeight)
		}

		g2d.color = Color(255, 255, 0, 25)
		g2d.fillRect(0, groundY, width, lineHeight)

		val bounds = character.debugBounds(world.transform)
		val hitboxX = logicalToX(groundLine, bounds.first)
		val hitboxEndX = logicalToX(groundLine, bounds.first + (bounds.last - bounds.first + 1))
		g2d.color = if (world.isOnGround) Color(255, 0, 0, 180) else Color(255, 100, 0, 180)
		g2d.stroke = BasicStroke(2f)
		g2d.drawRect(hitboxX, groundY, hitboxEndX - hitboxX, lineHeight)

		val bodyLine = groundLine - 1
		if (bodyLine >= 0) {
			val bodyY = editor.logicalPositionToXY(LogicalPosition(bodyLine, 0)).y
			g2d.stroke = BasicStroke(1f)
			g2d.color = Color(0, 100, 255, 40)
			val bodyExtent = map.getExtent(bodyLine)
			if (bodyExtent != null) {
				val bx = logicalToX(bodyLine, bodyExtent.first)
				val bEndX = logicalToX(bodyLine, bodyExtent.last + 1)
				g2d.fillRect(bx, bodyY, bEndX - bx, lineHeight)
				g2d.color = Color(0, 100, 255, 100)
				g2d.drawRect(bx, bodyY, bEndX - bx, lineHeight)
			}
		}

		g2d.stroke = oldStroke
	}

}
