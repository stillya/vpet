package dev.stillya.vpet

import dev.stillya.vpet.config.SpriteSheetAtlas

interface AtlasLoader {
	fun load(path: String): SpriteSheetAtlas?
}