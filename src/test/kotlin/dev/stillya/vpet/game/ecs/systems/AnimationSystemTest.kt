package dev.stillya.vpet.game.ecs.systems

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.game.ecs.EntityRegistry
import dev.stillya.vpet.game.ecs.Physics
import dev.stillya.vpet.game.ecs.components.AnimationComponent
import dev.stillya.vpet.game.resources.AnimationCache
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnimationSystemTest {

    private lateinit var loader: AtlasLoader
    private lateinit var registry: EntityRegistry

    @Before
    fun setup() {
        loader = AsepriteJsonAtlasLoader()
        registry = EntityRegistry()
        AnimationCache.clear()
        AnimationCache.loadCoinAnimation(loader)
    }

    @After
    fun cleanup() {
        AnimationCache.clear()
    }

    @Test
    fun `updateAnimations accumulates elapsed time when under threshold`() {
        val entity = registry.create()
        registry.add(entity, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = 0f))

        val dt = Physics.FRAME_ADVANCE_INTERVAL - 0.001f
        AnimationSystem.updateAnimations(registry, dt)

        val updated = registry.get<AnimationComponent>(entity)!!
        assertEquals(0, updated.currentFrame)
        assertEquals(dt, updated.elapsed, 0.0001f)
    }

    @Test
    fun `updateAnimations advances frame and resets elapsed when over threshold`() {
        val entity = registry.create()
        registry.add(entity, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = 0f))

        val dt = Physics.FRAME_ADVANCE_INTERVAL + 0.001f
        AnimationSystem.updateAnimations(registry, dt)

        val updated = registry.get<AnimationComponent>(entity)!!
        val resource = AnimationCache.get(AnimationCache.COIN_IDLE)!!
        val expectedFrame = (0 + 1) % resource.animation.frameCount
        assertEquals(expectedFrame, updated.currentFrame)
        assertTrue(updated.elapsed < Physics.FRAME_ADVANCE_INTERVAL)
    }

    @Test
    fun `updateAnimations wraps around to first frame after last frame`() {
        val resource = AnimationCache.get(AnimationCache.COIN_IDLE)!!
        val lastFrame = resource.animation.frameCount - 1

        val entity = registry.create()
        registry.add(entity, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = lastFrame, elapsed = 0f))

        val dt = Physics.FRAME_ADVANCE_INTERVAL + 0.001f
        AnimationSystem.updateAnimations(registry, dt)

        val updated = registry.get<AnimationComponent>(entity)!!
        assertEquals(0, updated.currentFrame)
    }

    @Test
    fun `updateAnimations processes multiple entities independently`() {
        val entity1 = registry.create()
        val entity2 = registry.create()

        registry.add(entity1, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = 0f))
        registry.add(entity2, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = Physics.FRAME_ADVANCE_INTERVAL - 0.01f))

        val dt = 0.02f
        AnimationSystem.updateAnimations(registry, dt)

        val updated1 = registry.get<AnimationComponent>(entity1)!!
        val updated2 = registry.get<AnimationComponent>(entity2)!!

        assertEquals(0, updated1.currentFrame)
        assertEquals(0.02f, updated1.elapsed, 0.0001f)

        val resource = AnimationCache.get(AnimationCache.COIN_IDLE)!!
        val expected2Frame = (0 + 1) % resource.animation.frameCount
        assertEquals(expected2Frame, updated2.currentFrame)
    }

    @Test
    fun `updateAnimations accumulates elapsed time across multiple ticks`() {
        val entity = registry.create()
        registry.add(entity, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = 0f))

        val smallDt = 0.01f
        for (i in 0 until 10) {
            AnimationSystem.updateAnimations(registry, smallDt)
        }

        val updated = registry.get<AnimationComponent>(entity)!!
        val resource = AnimationCache.get(AnimationCache.COIN_IDLE)!!
        val expectedFrame = (0 + 1) % resource.animation.frameCount
        assertEquals(expectedFrame, updated.currentFrame)
    }

    @Test
    fun `updateAnimations skips entities without AnimationComponent`() {
        val entityWithAnimation = registry.create()
        val entityWithoutAnimation = registry.create()

        registry.add(entityWithAnimation, AnimationComponent(AnimationCache.COIN_IDLE, currentFrame = 0, elapsed = 0f))

        val dt = Physics.FRAME_ADVANCE_INTERVAL + 0.001f
        AnimationSystem.updateAnimations(registry, dt)

        val updated = registry.get<AnimationComponent>(entityWithAnimation)!!
        val resource = AnimationCache.get(AnimationCache.COIN_IDLE)!!
        val expectedFrame = (0 + 1) % resource.animation.frameCount
        assertEquals(expectedFrame, updated.currentFrame)
    }

    @Test
    fun `updateAnimations handles missing resource gracefully`() {
        val entity = registry.create()
        registry.add(entity, AnimationComponent("non_existent_resource", currentFrame = 0, elapsed = 0f))

        val dt = Physics.FRAME_ADVANCE_INTERVAL + 0.001f
        AnimationSystem.updateAnimations(registry, dt)

        val updated = registry.get<AnimationComponent>(entity)!!
        assertEquals(0, updated.currentFrame)
        assertEquals(0f, updated.elapsed)
    }
}
