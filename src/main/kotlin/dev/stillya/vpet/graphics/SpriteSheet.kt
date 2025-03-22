package dev.stillya.vpet.graphics

import dev.stillya.vpet.config.AtlasFrame
import java.awt.Image

data class SpriteSheet(
	val image: Image,
	val frames: List<AtlasFrame>
)
