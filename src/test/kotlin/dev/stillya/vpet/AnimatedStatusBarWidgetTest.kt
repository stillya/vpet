package dev.stillya.vpet

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightPlatform4TestCase
import com.intellij.testFramework.registerServiceInstance
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.game.CoinCollectedListener
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationTrigger
import dev.stillya.vpet.pet.PetAnimated
import dev.stillya.vpet.settings.VPetSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Test
import javax.swing.Icon

class AnimatedStatusBarWidgetTest : LightPlatform4TestCase() {

	private lateinit var widget: AnimatedStatusBarWidget
	private lateinit var scope: CoroutineScope

	override fun setUp() {
		super.setUp()

		ApplicationManager.getApplication()
			.registerServiceInstance(AtlasLoader::class.java, AsepriteJsonAtlasLoader())
		ApplicationManager.getApplication()
			.registerServiceInstance(VPetSettings::class.java, VPetSettings())

		val mockRenderer = object : IconRenderer {
			override fun enqueue(animation: Animation) {}
			override fun render(): List<Icon> = emptyList()
			override fun createAnimationContext(trigger: AnimationTrigger): AnimationContext {
				return AnimationContext(epoch = 0L, triggerEvent = trigger)
			}
			override fun setFlipped(flipped: Boolean) {}
		}

		project.registerServiceInstance(IconRenderer::class.java, mockRenderer)
		project.registerServiceInstance(Animated::class.java, PetAnimated(project))

		scope = CoroutineScope(SupervisorJob())
		widget = AnimatedStatusBarWidget(project, scope)
	}

	override fun tearDown() {
		try {
			widget.dispose()
		} finally {
			super.tearDown()
		}
	}

	@Test
	fun `tooltip text updates after coin collected event`() = runBlocking {
		val initialTooltip = widget.getTooltipText()
		assertEquals("Coins collected: 0", initialTooltip)

		project.messageBus
			.syncPublisher(CoinCollectedListener.TOPIC)
			.onCoinsCollected(5)

		val updatedTooltip = widget.getTooltipText()
		assertEquals("Coins collected: 5", updatedTooltip)
	}

	@Test
	fun `tooltip text accumulates multiple coin events`() = runBlocking {
		assertEquals("Coins collected: 0", widget.getTooltipText())

		project.messageBus
			.syncPublisher(CoinCollectedListener.TOPIC)
			.onCoinsCollected(3)
		assertEquals("Coins collected: 3", widget.getTooltipText())

		project.messageBus
			.syncPublisher(CoinCollectedListener.TOPIC)
			.onCoinsCollected(7)
		assertEquals("Coins collected: 10", widget.getTooltipText())

		project.messageBus
			.syncPublisher(CoinCollectedListener.TOPIC)
			.onCoinsCollected(2)
		assertEquals("Coins collected: 12", widget.getTooltipText())
	}

	@Test
	fun `implements CoinCollectedListener interface`() {
		assertTrue(widget is CoinCollectedListener)
	}
}
