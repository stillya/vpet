package dev.stillya.vpet.game

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.INFINITE
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.graphics.create
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.JComponent

class GameRenderer(
	private val editor: Editor
) : JComponent() {

	private var world = World()
	private val columnMapper = VisualColumnMapper(editor)
	private var tileMap: VirtualTileMap? = null
	private var currentBounds: IntRange = 0..0
	private var currentAnimation: Animation? = null
	private val frameCache = mutableMapOf<String, List<BufferedImage>>()
	private val flippedFrameCache = mutableMapOf<String, List<BufferedImage>>()
	private val atlasLoader = AsepriteJsonAtlasLoader()

	private val coinFrames: List<BufferedImage> by lazy {
		try {
			val atlas = atlasLoader.load("/META-INF/spritesheets/coin/atlas.json") ?: return@lazy emptyList()
			val imgStream =
				javaClass.getResourceAsStream("/META-INF/spritesheets/coin/sprite.png") ?: return@lazy emptyList()
			val image = imgStream.use { ImageIO.read(it) }
			val spriteSheet = atlas.create(image, "coin")
			spriteSheet.frames.map { frame ->
				val f = frame.frame
				image.getSubimage(f.x, f.y, f.width, f.height)
			}
		} catch (e: Exception) {
			thisLogger().error("Failed to load coin sprite frames, falling back to colored squares", e)
			emptyList()
		}
	}

	private var coinFrameCounter = 0
	private var frameCount = 0
	private var lastFpsTime = System.nanoTime()
	private var fps = 0

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

		// TODO: Enable debug render with a toggle
//		drawDebug(g2d, lineHeight, groundLine, groundY)

		renderCoins(g2d, lineHeight)

		val animation = currentAnimation ?: return
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

		val bounds = currentBounds
		val hitboxX = colToPixelX(world.transform.x)
		val hitboxEndX = colToPixelX(world.transform.x + (bounds.last - bounds.first + 1))
		val hitboxCenterX = (hitboxX + hitboxEndX) / 2
		val pixelX = hitboxCenterX - spriteSize / 2
		val spriteY = groundY - spriteSize

		g2d.drawImage(frame, pixelX, spriteY, spriteSize, spriteSize, null)

		// TODO: Make it part of debug render and toggleable
//		frameCount++
//		val now = System.nanoTime()
//		if (now - lastFpsTime >= 1_000_000_000L) {
//			fps = frameCount
//			frameCount = 0
//			lastFpsTime = now
//		}
//		g2d.color = JBColor.BLACK
//		g2d.font = g2d.font.deriveFont(11f)
//		g2d.drawString("FPS: $fps", 4, 14)
	}

	private fun colToPixelX(col: Int): Int = columnMapper.toPixelX(col)

	private fun colToPixelX(col: Float): Int = columnMapper.toPixelX(col)

	private fun flipImage(image: BufferedImage): BufferedImage {
		val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
		tx.translate(-image.width.toDouble(), 0.0)
		return AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null)
	}

	private fun renderCoins(g2d: Graphics2D, lineHeight: Int) {
		val reg = world.registry
		val coins = reg.allWith(CoinVisual::class, Transform::class)

		for (id in coins) {
			val t = reg.get<Transform>(id) ?: continue
			val visual = reg.get<CoinVisual>(id) ?: continue

			val coinLine = kotlin.math.floor(t.y).toInt()
			val lineFrac = t.y - coinLine
			val baseY = editor.logicalPositionToXY(LogicalPosition(coinLine, 0)).y
			val pixelY = baseY + (lineFrac * lineHeight).toInt()
			val pixelX = colToPixelX(t.x)
			val cellW = colToPixelX(t.x + 1) - pixelX

			if (coinFrames.isNotEmpty()) {
				val frameIndex = if (coinFrames.size > 1) {
					coinFrameCounter % coinFrames.size
				} else {
					0
				}
				val frame = coinFrames[frameIndex]
				val size = lineHeight
				val cx = pixelX + cellW / 2 - size / 2
				val cy = pixelY + lineHeight / 2 - size / 2
				g2d.drawImage(frame, cx, cy, size, size, null)
			} else {
				val color = Color(255, 215, 0)
				val cx = pixelX + cellW / 2
				val cy = pixelY + lineHeight / 2
				val r = (lineHeight.coerceAtMost(cellW) / 2 - 1).coerceAtLeast(2)
				g2d.color = color
				g2d.fillRect(cx - r, cy - r, r * 2, r * 2)
			}
		}

		coinFrameCounter++
	}

	private fun drawDebug(g2d: Graphics2D, lineHeight: Int, groundLine: Int, groundY: Int) {
		val map = tileMap ?: return
		val oldStroke = g2d.stroke

		g2d.stroke = BasicStroke(1f)
		map.forEachExtent { line, extent ->
			val y = editor.logicalPositionToXY(LogicalPosition(line, 0)).y
			val x = colToPixelX(extent.first)
			val endX = colToPixelX(extent.last + 1)
			g2d.color = Color(0, 255, 0, 30)
			g2d.fillRect(x, y, endX - x, lineHeight)
			g2d.color = Color(0, 255, 0, 120)
			g2d.drawRect(x, y, endX - x, lineHeight)
		}

		g2d.color = Color(255, 255, 0, 25)
		g2d.fillRect(0, groundY, width, lineHeight)

		val bounds = currentBounds
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
			val bodyExtent = map.getExtent(bodyLine)
			if (bodyExtent != null) {
				val bx = colToPixelX(bodyExtent.first)
				val bEndX = colToPixelX(bodyExtent.last + 1)
				g2d.fillRect(bx, bodyY, bEndX - bx, lineHeight)
				g2d.color = Color(0, 100, 255, 100)
				g2d.drawRect(bx, bodyY, bEndX - bx, lineHeight)
			}
		}

		g2d.stroke = oldStroke
	}

}
