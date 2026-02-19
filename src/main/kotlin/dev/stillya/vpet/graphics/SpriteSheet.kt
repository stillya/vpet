package dev.stillya.vpet.graphics

import dev.stillya.vpet.config.AtlasFrame
import dev.stillya.vpet.config.SpriteSheetAtlas
import java.awt.Image

data class SpriteSheet(
	val image: Image,
	val frames: List<AtlasFrame>
)

fun SpriteSheetAtlas.create(img: Image, animationTag: String): SpriteSheet {
	val frameTag = this.meta.frameTags.find { it.name == animationTag }
		?: throw IllegalArgumentException("Animation tag not found: $animationTag")

	return SpriteSheet(
		image = img,
		frames = this.frames.subList(frameTag.from, frameTag.to + 1)
	)
}
