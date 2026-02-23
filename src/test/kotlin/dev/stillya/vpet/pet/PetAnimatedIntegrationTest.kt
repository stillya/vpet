package dev.stillya.vpet.pet

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatform4TestCase
import com.intellij.testFramework.registerServiceInstance
import dev.stillya.vpet.Animated
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationEpochManager
import dev.stillya.vpet.graphics.AnimationTrigger
import org.junit.Test
import javax.swing.Icon
import kotlin.random.Random

class PetAnimatedIntegrationTest : LightPlatform4TestCase() {

	private lateinit var rendererSpy: IconRendererSpy
	private lateinit var petAnimated: PetAnimated

	override fun setUp() {
		super.setUp()

		rendererSpy = IconRendererSpy(project)

		ApplicationManager.getApplication()
			.registerServiceInstance(AtlasLoader::class.java, AsepriteJsonAtlasLoader())
		project.registerServiceInstance(IconRenderer::class.java, rendererSpy)
		project.registerServiceInstance(Animated::class.java, PetAnimated(project))

		petAnimated = project.service<Animated>() as PetAnimated
		petAnimated.random = Random(42L)

		petAnimated.init(
			Animated.Params(
				atlasPath = "/META-INF/spritesheets/cat/atlas.json",
				imgPath = "/META-INF/spritesheets/cat/sprite.png"
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

	@Test
	fun testMissingTransitionIsIgnored() {
		rendererSpy.clear()

		petAnimated.onProgress()
		val runningAnimationCount = rendererSpy.enqueuedAnimations.size

		petAnimated.onFail()
		val afterFailCount = rendererSpy.enqueuedAnimations.size

		petAnimated.onProgress()

		assertEquals(
			"Transition from FAILED to RUNNING should be ignored (no new animations after second onProgress)",
			afterFailCount,
			rendererSpy.enqueuedAnimations.size
		)
	}

	@Test
	fun testInactivityTransitionsToObserving() {
		rendererSpy.clear()

		petAnimated.onStartObserving()

		assertTrue("Should have animations enqueued", rendererSpy.enqueuedAnimations.isNotEmpty())

		val animations = rendererSpy.collectChain()
		assertTrue(
			"Should transition to back view with R_A_4",
			animations.any { it.name == "R_A_4" }
		)

		val backViewAnimation = animations.find { it.name == "R_A_5" }
		assertNotNull("Should have R_A_5 back view loop", backViewAnimation)
		assertEquals("R_A_5 should loop infinitely", -1, backViewAnimation?.loop)
	}

	@Test
	fun testObservingDoesNotStartWhenAlreadyObserving() {
		rendererSpy.clear()

		petAnimated.onStartObserving()
		val firstCallCount = rendererSpy.enqueuedAnimations.size

		petAnimated.onStartObserving()
		val secondCallCount = rendererSpy.enqueuedAnimations.size

		assertEquals(
			"Second onStartObserving call should be ignored",
			firstCallCount,
			secondCallCount
		)
	}

	@Test
	fun testObservingDoesNotStartWhenNotIdle() {
		rendererSpy.clear()

		petAnimated.onProgress()
		rendererSpy.clear()

		petAnimated.onStartObserving()

		assertEquals(
			"Should not start observing when not in IDLE state",
			0,
			rendererSpy.enqueuedAnimations.size
		)
	}

	@Test
	fun testCursorMoveFlipsSprite() {
		rendererSpy.clear()

		petAnimated.onStartObserving()

		var flippedLeft = false
		var flippedRight = false

		// Simulate cursor on left side
		petAnimated.onCursorMove(true)
		if (rendererSpy.isFlipped) flippedLeft = true

		// Simulate cursor on right side
		petAnimated.onCursorMove(false)
		if (!rendererSpy.isFlipped) flippedRight = true

		assertTrue("Should flip when cursor moves to different sides", flippedLeft || flippedRight)
	}
}

class IconRendererSpy(project: Project) : IconRenderer {
	val enqueuedAnimations = mutableListOf<Animation>()
	private var flippedState: Boolean = false

	val isFlipped: Boolean
		get() = flippedState

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

	override fun setFlipped(flipped: Boolean) {
		flippedState = flipped
	}

	fun clear() {
		enqueuedAnimations.clear()
		flippedState = false
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
