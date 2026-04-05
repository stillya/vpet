package dev.stillya.vpet.game.resources

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.config.AtlasFrame
import dev.stillya.vpet.config.AtlasMeta
import dev.stillya.vpet.config.FrameRect
import dev.stillya.vpet.config.FrameTag
import dev.stillya.vpet.config.SpriteSheetAtlas
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockAtlasLoaderTest {

    private lateinit var mockLoader: MockAtlasLoader

    @Before
    fun setup() {
        mockLoader = MockAtlasLoader()
        AnimationCache.clear()
    }

    @After
    fun cleanup() {
        AnimationCache.clear()
    }

    @Test
    fun testMockAtlasLoaderReturnsConfiguredAtlas() {
        val mockAtlas = SpriteSheetAtlas(
            meta = AtlasMeta(
                frameTags = listOf(
                    FrameTag(name = "test_anim", from = 0, to = 0, duration = 100)
                )
            ),
            frames = listOf(
                AtlasFrame(
                    frame = FrameRect(x = 0, y = 0, width = 16, height = 16)
                )
            )
        )

        mockLoader.registerAtlas("/test/atlas.json", mockAtlas)

        val result = mockLoader.load("/test/atlas.json")
        assertNotNull(result)
        assertEquals(1, result?.frames?.size)
        assertEquals("test_anim", result?.meta?.frameTags?.get(0)?.name)
    }

    @Test
    fun testMockAtlasLoaderReturnsNullForUnregisteredPath() {
        val result = mockLoader.load("/unregistered/path.json")
        assertNull(result)
    }

    @Test
    fun testAnimationCacheWorksWithMockLoader() {
        val mockAtlas = SpriteSheetAtlas(
            meta = AtlasMeta(
                frameTags = listOf(
                    FrameTag(name = "mock_anim", from = 0, to = 0, duration = 100)
                )
            ),
            frames = listOf(
                AtlasFrame(
                    frame = FrameRect(x = 0, y = 0, width = 16, height = 16)
                )
            )
        )

        mockLoader.registerAtlas("/test/mock_sprite.json", mockAtlas)

        val resource = AnimationCache.loadAnimation(
            atlasLoader = mockLoader,
            imagePath = "/META-INF/spritesheets/coin/sprite.png",
            atlasPath = "/test/mock_sprite.json",
            animationTag = "mock_anim",
            resourceId = "test_mock"
        )

        assertNotNull(resource)
        assertEquals("test_mock", resource.id)
        assertEquals("mock_anim", resource.animation.name)
    }
}

class MockAtlasLoader : AtlasLoader {
    private val atlases = mutableMapOf<String, SpriteSheetAtlas>()

    fun registerAtlas(path: String, atlas: SpriteSheetAtlas) {
        atlases[path] = atlas
    }

    override fun load(path: String): SpriteSheetAtlas? {
        return atlases[path]
    }
}
