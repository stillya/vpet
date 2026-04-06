package dev.stillya.vpet.game.rendering

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.INFINITE
import dev.stillya.vpet.game.VirtualTileMap
import dev.stillya.vpet.game.ecs.World
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.ecs.components.Transform
import dev.stillya.vpet.game.resources.AnimationCache
import dev.stillya.vpet.game.utils.toTileInt
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

class RenderSystem(
	private val editor: Editor
) {
	private val columnMapper = VisualColumnMapper(editor)
	private val frameCache = mutableMapOf<String, List<BufferedImage>>()
	private val flippedFrameCache = mutableMapOf<String, List<BufferedImage>>()

	fun render(
		g2d: Graphics2D,
		world: World,
		animation: Animation,
		bounds: IntRange
	) {
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

		renderCoins(g2d, world, lineHeight)
		renderPlayer(g2d, world, animation, spriteSize, groundY, bounds)
	}

	private fun renderPlayer(
		g2d: Graphics2D,
		world: World,
		animation: Animation,
		spriteSize: Int,
		groundY: Int,
		bounds: IntRange
	) {
		val tag = world.sprite.tag
		val frames = if (world.sprite.direction.isLeft()) {
			flippedFrameCache.getOrPut(tag) {
				animation.extractFrames().map { flipImage(it) }
			}
		} else {
			frameCache.getOrPut(tag) {
				animation.extractFrames()
			}
		}
		if (frames.isEmpty()) return

		val looping = animation.loop == INFINITE
		val idx = if (looping) world.sprite.frameIndex % frames.size
		else world.sprite.frameIndex.coerceAtMost(frames.size - 1)
		val frame = frames[idx]

		val hitboxX = colToPixelX(world.transform.x)
		val hitboxEndX = colToPixelX(world.transform.x + (bounds.last - bounds.first + 1))
		val hitboxCenterX = (hitboxX + hitboxEndX) / 2
		val pixelX = hitboxCenterX - spriteSize / 2
		val spriteY = groundY - spriteSize

		g2d.drawImage(frame, pixelX, spriteY, spriteSize, spriteSize, null)
	}

	private fun renderCoins(g2d: Graphics2D, world: World, lineHeight: Int) {
		val reg = world.registry
		val coins = reg.allWith(AnimationComponent::class, Transform::class)

		for (id in coins) {
			val t = reg.get<Transform>(id) ?: continue
			val animComp = reg.get<AnimationComponent>(id) ?: continue

			val resource = AnimationCache.get(animComp.resourceId) ?: continue
			val frames = resource.frames
			if (frames.isEmpty()) continue

			val coinLine = t.y.toTileInt()
			if (coinLine < 0) continue

			val lineFrac = t.y - coinLine
			val baseY = editor.logicalPositionToXY(LogicalPosition(coinLine, 0)).y
			val pixelY = baseY + (lineFrac * lineHeight).toInt()
			val pixelX = colToPixelX(t.x)
			val cellW = colToPixelX(t.x + 1) - pixelX

			val frameIndex = animComp.currentFrame.coerceIn(0, frames.size - 1)
			val frame = frames[frameIndex]
			val size = lineHeight
			val cx = pixelX + cellW / 2 - size / 2
			val cy = pixelY + lineHeight / 2 - size / 2
			g2d.drawImage(frame, cx, cy, size, size, null)
		}
	}

	fun renderDebug(
		g2d: Graphics2D,
		world: World,
		tileMap: VirtualTileMap,
		bounds: IntRange,
		fps: Int
	) {
		val lineHeight = editor.lineHeight
		val groundLine = world.displayLine
		val lineFrac = world.transform.y - groundLine
		val groundYBase = editor.logicalPositionToXY(LogicalPosition(groundLine, 0)).y
		val groundY = groundYBase + (lineFrac * lineHeight).toInt()

		val oldStroke = g2d.stroke

		g2d.stroke = BasicStroke(1f)
		tileMap.forEachExtent { line, extent ->
			val y = editor.logicalPositionToXY(LogicalPosition(line, 0)).y
			val x = colToPixelX(extent.first)
			val endX = colToPixelX(extent.last + 1)
			g2d.color = Color(0, 255, 0, 30)
			g2d.fillRect(x, y, endX - x, lineHeight)
			g2d.color = Color(0, 255, 0, 120)
			g2d.drawRect(x, y, endX - x, lineHeight)
		}

		g2d.color = Color(255, 255, 0, 25)
		g2d.fillRect(0, groundY, 10000, lineHeight)

		val hitboxX = colToPixelX(bounds.first)
		val hitboxEndX = colToPixelX(bounds.first + (bounds.last - bounds.first + 1))
		g2d.color = if (world.isOnGround) Color(255, 0, 0, 180) else Color(255, 100, 0, 180)
		g2d.stroke = BasicStroke(2f)
		g2d.drawRect(hitboxX, groundY, hitboxEndX - hitboxX, lineHeight)

		val bodyLine = groundLine - 1
		if (bodyLine >= 0) {
			val bodyY = editor.logicalPositionToXY(LogicalPosition(bodyLine, 0)).y
			g2d.stroke = BasicStroke(1f)
			g2d.color = Color(0, 100, 255, 40)
			val bodyExtent = tileMap.getExtent(bodyLine)
			if (bodyExtent != null) {
				val bx = colToPixelX(bodyExtent.first)
				val bEndX = colToPixelX(bodyExtent.last + 1)
				g2d.fillRect(bx, bodyY, bEndX - bx, lineHeight)
				g2d.color = Color(0, 100, 255, 100)
				g2d.drawRect(bx, bodyY, bEndX - bx, lineHeight)
			}
		}

		g2d.color = Color(0, 0, 0, 170)
		g2d.fillRoundRect(8, 8, 72, 24, 8, 8)
		g2d.color = Color(255, 255, 255, 220)
		g2d.font = g2d.font.deriveFont(Font.BOLD, 12f)
		g2d.drawString("FPS: $fps", 16, 24)

		g2d.stroke = oldStroke
	}

	private fun colToPixelX(col: Int): Int = columnMapper.toPixelX(col)

	private fun colToPixelX(col: Float): Int = columnMapper.toPixelX(col)

	private fun flipImage(image: BufferedImage): BufferedImage {
		val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
		tx.translate(-image.width.toDouble(), 0.0)
		return AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null)
	}
}
