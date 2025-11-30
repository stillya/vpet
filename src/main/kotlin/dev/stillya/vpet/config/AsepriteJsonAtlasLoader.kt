package dev.stillya.vpet.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import dev.stillya.vpet.AtlasLoader
import java.io.InputStream

class AsepriteJsonAtlasLoader : AtlasLoader {
	private val log = logger<AsepriteJsonAtlasLoader>()

	private val mapper: ObjectMapper = ObjectMapper().configure(
		DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false,
	)

	companion object {
		@JvmStatic
		fun getInstance(): AsepriteJsonAtlasLoader = service<AsepriteJsonAtlasLoader>()
	}

	override fun load(path: String): SpriteSheetAtlas? {
		log.info("Loading atlas from classpath resource: $path")
		val inputStream =
			AsepriteJsonAtlasLoader::class.java.getResourceAsStream(path)
		if (inputStream == null) {
			log.warn("Resource not found: $path")
			return null
		}

		return inputStream.use { loadFromStream(it) }
	}

	private fun loadFromStream(inputStream: InputStream): SpriteSheetAtlas? {
		val atlas = mapper.readValue(inputStream, SpriteSheetAtlas::class.java)
		log.info("Atlas loaded successfully with ${atlas.frames.size} frames and ${atlas.meta.frameTags.size} tags")
		return atlas
	}
}