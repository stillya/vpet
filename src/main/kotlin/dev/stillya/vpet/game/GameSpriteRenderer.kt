package dev.stillya.vpet.game

import com.intellij.openapi.components.service
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import dev.stillya.vpet.settings.CatVariant
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class GameSpriteRenderer(variant: CatVariant) {

	private val atlas: SpriteSheetAtlas
	private val image: BufferedImage
	private val frameCache = mutableMapOf<Pair<String, Boolean>, List<BufferedImage>>()

	init {
		val atlasLoader = service<AtlasLoader>()
		atlas = atlasLoader.load(variant.atlasPath)
			?: throw IllegalStateException("Failed to load atlas: ${variant.atlasPath}")

		val imageStream = javaClass.getResourceAsStream(variant.imagePath)
			?: throw IllegalStateException("Failed to load image: ${variant.imagePath}")
		image = ImageIO.read(imageStream)
	}

	fun getFrames(tag: String, flipped: Boolean): List<BufferedImage> {
		return frameCache.getOrPut(tag to flipped) {
			val frameTag = atlas.meta.frameTags.find { it.name == tag }
				?: return@getOrPut emptyList()

			val frames = atlas.frames.subList(frameTag.from, frameTag.to + 1)
			frames.map { atlasFrame ->
				val f = atlasFrame.frame
				val frameImage = image.getSubimage(f.x, f.y, f.width, f.height)

				if (flipped) {
					val tx = AffineTransform.getScaleInstance(-1.0, 1.0)
					tx.translate(-frameImage.width.toDouble(), 0.0)
					AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(frameImage, null)
				} else {
					frameImage
				}
			}
		}
	}
}
