package dev.stillya.vpet.pet

import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.graphics.Animation
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationEpochManager
import dev.stillya.vpet.graphics.AnimationState
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
			injectedRenderer = rendererSpy, injectedAtlasLoader = atlasLoader
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
			"Should have at least Walk_Run animation",
			rendererSpy.enqueuedAnimations.isNotEmpty()
		)
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertEquals("Walk_Run", firstAnimation.name)
		assertEquals(1, firstAnimation.loop)

		val chainedAnimation = firstAnimation.nextAnimation
		assertNotNull("Should chain to Run animation", chainedAnimation)
		assertEquals("Run", chainedAnimation?.name)
		assertEquals(-1, chainedAnimation?.loop)
	}

	@Test
	fun testBuildSuccessTransitionsToCelebrating() {
		rendererSpy.clear()

		petAnimated.onSuccess()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertEquals("Paws", firstAnimation.name)
		assertEquals(2, firstAnimation.loop)

		val animations = rendererSpy.collectChain()
		assertEquals(
			"Celebrating should have 5 steps (added Stop transition)", 4, animations.size
		)
	}

	@Test
	fun testBuildFailedTransitionsToFailedState() {
		rendererSpy.clear()

		petAnimated.onFail()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())
		val firstAnimation = rendererSpy.enqueuedAnimations[0]

		assertTrue(
			"Should start with FAILED animation",
			listOf("Dmg", "Pooping").contains(firstAnimation.name)
		)

		val animations = rendererSpy.collectChain()
		assertTrue(
			"FAILED sequence should have 3 or 4 steps (from SITTING: Pooping+Dig+Sit_Up, from IDLE: Dmg+Death+Deat_End+Spawn_2)",
			animations.size == 3 || animations.size == 4
		)
	}

	@Test
	fun testBuildCompletedTransitionsToCelebrating() {
		rendererSpy.clear()

		petAnimated.onCompleted()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertEquals("Paws", firstAnimation.name)
		assertEquals(2, firstAnimation.loop)
	}

	@Test
	fun testUserClickTransitionsToOccasion() {
		rendererSpy.clear()

		petAnimated.onOccasion()

		assertTrue(rendererSpy.enqueuedAnimations.isNotEmpty())
		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertTrue(
			"Should have occasion animation",
			listOf("Paws", "Pac-Cat", "Goomba", "J_1", "rook_around").contains(
				firstAnimation.name
			)
		)
		assertEquals(3, firstAnimation.loop)
	}

	@Test
	fun testRunningStateHasGuardedInfiniteLoop() {
		rendererSpy.clear()

		petAnimated.onProgress()

		val runAnimation = rendererSpy.enqueuedAnimations[0].nextAnimation
		assertNotNull("Should have Run animation", runAnimation)
		assertEquals("Run", runAnimation?.name)
		assertEquals(-1, runAnimation?.loop)
		assertNotNull("Run animation should have guard", runAnimation?.guard)
	}

	@Test
	fun testCelebratingSequenceHasCorrectStructure() {
		rendererSpy.clear()

		petAnimated.onSuccess()

		val animations = rendererSpy.collectChain()

		assertEquals("Paws", animations[0].name)
		assertEquals(2, animations[0].loop)

		assertTrue(
			"Second should be jump animation",
			listOf("J_1", "J_U_D").contains(animations[1].name)
		)

		assertTrue(
			"Third should be attack animation", animations[2].name.startsWith("Attack_")
		)

		assertEquals("Walk", animations[3].name)
		assertEquals(3, animations[3].loop)
	}

	@Test
	fun testAnimationContextIsProperlySet() {
		rendererSpy.clear()

		petAnimated.onSuccess()

		val firstAnimation = rendererSpy.enqueuedAnimations[0]
		assertNotNull("Animation should have context", firstAnimation.context)
		assertEquals("CELEBRATING", firstAnimation.context?.targetState?.name)
		assertEquals("BUILD_SUCCESS", firstAnimation.context?.triggerEvent?.name)
	}

	@Test
	fun testAnimationChainingWorksCorrectly() {
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
		trigger: AnimationTrigger, targetState: AnimationState
	): AnimationContext {
		return AnimationEpochManager().createContext(trigger, targetState)
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
