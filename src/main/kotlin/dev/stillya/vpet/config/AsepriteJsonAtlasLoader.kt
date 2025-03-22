package dev.stillya.vpet.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import dev.stillya.vpet.AtlasLoader
import java.io.InputStream

@Service
class AsepriteJsonAtlasLoader : AtlasLoader {
	private val LOG = logger<AsepriteJsonAtlasLoader>()

	private val mapper: ObjectMapper = ObjectMapper().configure(
		DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false,
	)

	override fun load(path: String): SpriteSheetAtlas? {
		try {
			LOG.info("Loading atlas from classpath resource: $path")
			val inputStream =
				AsepriteJsonAtlasLoader::class.java.getResourceAsStream(path)
			if (inputStream == null) {
				LOG.warn("Resource not found: $path")
				return null
			}

			return inputStream.use { loadFromStream(it) }
		} catch (e: Exception) {
			LOG.warn("Error loading atlas from classpath: $path", e)
			return null
		}
	}

	private fun loadFromStream(inputStream: InputStream): SpriteSheetAtlas? {
		try {
			LOG.info("Loading atlas from input stream")
			val atlas = mapper.readValue(inputStream, SpriteSheetAtlas::class.java)
			LOG.info("Atlas loaded successfully with ${atlas.frames.size} frames and ${atlas.meta.frameTags.size} tags")
			return atlas
		} catch (e: Exception) {
			LOG.warn("Error parsing atlas JSON", e)
			return null
		}
	}
}