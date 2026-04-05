package dev.stillya.vpet.game.ecs.components

import org.junit.Assert.assertEquals
import org.junit.Test

class AnimationComponentTest {

    @Test
    fun testAnimationComponentReferencesResourceById() {
        val component = AnimationComponent(
            resourceId = "coin_idle",
            currentFrame = 0,
            elapsed = 0f
        )

        assertEquals("coin_idle", component.resourceId)
        assertEquals(0, component.currentFrame)
        assertEquals(0f, component.elapsed, 0.001f)
    }

    @Test
    fun testAnimationComponentWithAdvancedState() {
        val component = AnimationComponent(
            resourceId = "coin_idle",
            currentFrame = 3,
            elapsed = 0.15f
        )

        assertEquals("coin_idle", component.resourceId)
        assertEquals(3, component.currentFrame)
        assertEquals(0.15f, component.elapsed, 0.001f)
    }

    @Test
    fun testAnimationComponentDefaultValues() {
        val component = AnimationComponent(resourceId = "test_anim")

        assertEquals("test_anim", component.resourceId)
        assertEquals(0, component.currentFrame)
        assertEquals(0f, component.elapsed, 0.001f)
    }
}
