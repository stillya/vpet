package dev.stillya.vpet.game.resources

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.graphics.create
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

object AnimationCache {
    private val cache = ConcurrentHashMap<String, AnimationResource>()

    const val COIN_IDLE = "coin_idle"

    fun loadAnimation(
        atlasLoader: AtlasLoader,
        imagePath: String,
        atlasPath: String,
        animationTag: String,
        resourceId: String
    ): AnimationResource {
        return cache.getOrPut(resourceId) {
            val atlas = atlasLoader.load(atlasPath)
                ?: throw IllegalArgumentException("Atlas not found: $atlasPath")

            val imageStream = AnimationCache::class.java.getResourceAsStream(imagePath)
                ?: throw IllegalArgumentException("Image not found: $imagePath")

            val image = ImageIO.read(imageStream)
            val sheet = atlas.create(image, animationTag)

            val animation = Animation(
                name = animationTag,
                sheet = sheet,
                loop = 0,
                onFinish = {},
                context = null,
                state = dev.stillya.vpet.animation.AnimationState.IDLE
            )

            AnimationResource(
                id = resourceId,
                animation = animation,
                frames = animation.extractFrames()
            )
        }
    }

    fun loadCoinAnimation(atlasLoader: AtlasLoader): AnimationResource {
        return loadAnimation(
            atlasLoader = atlasLoader,
            imagePath = "/META-INF/spritesheets/coin/sprite.png",
            atlasPath = "/META-INF/spritesheets/coin/atlas.json",
            animationTag = "coin",
            resourceId = COIN_IDLE
        )
    }

    fun get(resourceId: String): AnimationResource? = cache[resourceId]

    fun clear() {
        cache.clear()
    }
}
