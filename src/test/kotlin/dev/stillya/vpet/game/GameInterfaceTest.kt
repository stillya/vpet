package dev.stillya.vpet.game

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatform4TestCase
import com.intellij.testFramework.registerServiceInstance
import dev.stillya.vpet.Animated
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.pet.IconRendererSpy
import dev.stillya.vpet.pet.PetAnimated
import org.junit.Test

class GameInterfaceTest : LightPlatform4TestCase() {

	private lateinit var petAnimated: PetAnimated

	override fun setUp() {
		super.setUp()

		ApplicationManager.getApplication()
			.registerServiceInstance(AtlasLoader::class.java, AsepriteJsonAtlasLoader())
		project.registerServiceInstance(IconRenderer::class.java, IconRendererSpy(project))
		project.registerServiceInstance(Animated::class.java, PetAnimated(project))

		petAnimated = project.service<Animated>() as PetAnimated
		petAnimated.init(
			Animated.Params(
				atlasPath = "/META-INF/spritesheets/cat/atlas.json",
				imgPath = "/META-INF/spritesheets/cat/sprite.png"
			)
		)
	}

	@Test
	fun `PetAnimated can be cast to Game`() {
		val animated: Animated = project.service<Animated>()
		assertTrue("PetAnimated should implement Game", animated is Game)
	}

	@Test
	fun `onGameStart is callable without error`() {
		val game = petAnimated as Game
		game.onGameStart()
		// No exception means success
	}

	@Test
	fun `onGameStop is callable without error`() {
		val game = petAnimated as Game
		game.onGameStop()
		// No exception means success
	}

	@Test
	fun `onGameStart and onGameStop can be called in sequence`() {
		val game = petAnimated as Game
		game.onGameStart()
		game.onGameStop()
		game.onGameStart()
		game.onGameStop()
		// No exception means success
	}
}
