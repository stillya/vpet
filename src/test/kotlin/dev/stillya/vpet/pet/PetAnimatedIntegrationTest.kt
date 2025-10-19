package dev.stillya.vpet.pet

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationEpochManager
import dev.stillya.vpet.graphics.AnimationTrigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import javax.swing.Icon

class PetAnimatedIntegrationTest {

	private lateinit var rendererSpy: IconRendererSpy
	private lateinit var petAnimated: PetAnimated

	@Before
	fun setup() {
		rendererSpy = IconRendererSpy()
		val atlasLoader: AtlasLoader = AsepriteJsonAtlasLoader()
		petAnimated = PetAnimated(
			injectedRenderer = rendererSpy,
			injectedAtlasLoader = atlasLoader,
			randomSeed = 42L
		)

		petAnimated.init(
			Animated.Params(
				atlasPath = "/META-INF/spritesheets/cat_atlas.json",
				imgPath = "/META-INF/spritesheets/cat.png"
			)
		)
	}

	@Test
	fun testInitializationEnqueuesIdleAnimation() {
		assertEquals(1, rendererSpy.enqueuedAnimations.size)
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertTrue(
			"Should start with idle animation",
			listOf("Idle", "Sit", "Dream", "Rest").contains(firstAnimation.name)
		)
		assertEquals(-1, firstAnimation.loop)
	}

	@Test
	fun testBuildStartTransitionsToRunning() {
		rendererSpy.clear()

		petAnimated.onProgress()

		assertTrue(
			"Should have animations",
			rendererSpy.enqueuedAnimations.isNotEmpty()
		)

		val animations = rendererSpy.collectChain()
		val runIdx = animations.indexOfFirst { it.name == "Run" }
		assertTrue("Should have Run animation in chain", runIdx >= 0)
		assertEquals(-1, animations[runIdx].loop)
		assertTrue("Should have at least one animation before Run", runIdx > 0)
	}

	@Test
	fun testBuildSuccessTransitionsToCelebrating() {
		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onSuccess()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())

		val animations = rendererSpy.collectChain()
		assertTrue(
			"Should have celebration animation (J_1 or Paws)",
			animations.any { it.name == "J_1" || it.name == "Paws" }
		)
	}

	@Test
	fun testBuildFailedTransitionsToFailedState() {
		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onFail()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())

		val animations = rendererSpy.collectChain()
		assertTrue(
			"Should have FAILED animation",
			animations.any { listOf("Dmg", "Pooping").contains(it.name) }
		)
		assertTrue("FAILED sequence should have at least 3 steps", animations.size >= 3)
	}

	@Test
	fun testBuildCompletedTransitionsToCelebrating() {
		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onCompleted()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())

		val animations = rendererSpy.collectChain()
		assertTrue(
			"Should have celebration animation (J_1 or Paws)",
			animations.any { it.name == "J_1" || it.name == "Paws" }
		)
	}

	@Test
	fun testUserClickTransitionsToOccasion() {
		rendererSpy.clear()

		petAnimated.onOccasion()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertTrue(
			"Should have occasion animation",
			listOf("Paws", "Pac-Cat", "Goomba", "rook_around").contains(
				firstAnimation.name
			)
		)
		assertEquals(5, firstAnimation.loop)
	}

	@Test
	fun testRunningStateHasGuardedInfiniteLoop() {
		rendererSpy.clear()

		petAnimated.onProgress()

		val animations = rendererSpy.collectChain()
		val runAnimation = animations.find { it.name == "Run" }
		assertNotNull("Should have Run animation", runAnimation)
		assertEquals(-1, runAnimation?.loop)
		assertNotNull("Run animation should have guard", runAnimation?.guard)
	}

	@Test
	fun testAnimationContextIsProperlySet() {
		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onSuccess()

		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertNotNull("Animation should have context", firstAnimation.context)
		assertEquals("BUILD_SUCCESS", firstAnimation.context?.triggerEvent?.name)
	}

	@Test
	fun testAnimationChainingWorksCorrectly() {
		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onSuccess()

		val animations = rendererSpy.collectChain()

		for (i in 0 until animations.size - 1) {
			assertNotNull(
				"Animation ${animations[i].name} should chain to next",
				animations[i].nextAnimation
			)
			assertEquals(
				"Chain should be correct", animations[i + 1], animations[i].nextAnimation
			)
		}

		assertNull(
			"Last animation should not have next", animations.last().nextAnimation
		)
	}
}

class IconRendererSpy : IconRenderer {
	val enqueuedAnimations = mutableListOf<Animation>()

	override fun enqueue(animation: Animation) {
		enqueuedAnimations.add(animation)
	}

	override fun render(): List<Icon> {
		return emptyList()
	}

	override fun createAnimationContext(
		trigger: AnimationTrigger
	): AnimationContext {
		return AnimationEpochManager().createContext(trigger)
	}

	fun clear() {
		enqueuedAnimations.clear()
	}

	fun collectChain(): List<Animation> {
		if (enqueuedAnimations.isEmpty()) return emptyList()
		val chain = mutableListOf<Animation>()
		var current: Animation? = enqueuedAnimations[0]
		while (current != null) {
			chain.add(current)
			current = current.nextAnimation
		}
		return chain
	}
}
