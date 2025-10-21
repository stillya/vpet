package dev.stillya.vpet.config

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AsepriteJsonAtlasLoaderTest {

	private lateinit var loader: AsepriteJsonAtlasLoader

	@Before
	fun setup() {
		loader = AsepriteJsonAtlasLoader()
	}

	@Test
	fun testLoadedAtlasHasSuccessfullyLoaded() {
		val atlas = loader.load("/META-INF/spritesheets/cat_atlas.json")

		assertNotNull("Atlas should be loaded", atlas)
		assertTrue(
			"Atlas should have multiple frames",
			atlas!!.frames.isNotEmpty()
		)

		val tagNames = atlas.meta.frameTags.map { it.name }

		assertTrue("Should have Idle animation", tagNames.contains("Idle"))
		assertTrue("Should have Run animation", tagNames.contains("Run"))
		assertTrue("Should have Walk animation", tagNames.contains("Walk"))
		assertTrue("Should have Sit animation", tagNames.contains("Sit"))
	}
}
