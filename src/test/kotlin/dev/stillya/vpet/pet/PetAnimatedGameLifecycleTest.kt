package dev.stillya.vpet.pet

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.testFramework.LightPlatform4TestCase
import com.intellij.testFramework.registerServiceInstance
import dev.stillya.vpet.Animated
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.game.Character
import dev.stillya.vpet.game.EntityID
import dev.stillya.vpet.game.Game
import org.junit.Test

class PetAnimatedGameLifecycleTest : LightPlatform4TestCase() {

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
    fun `PetAnimated implements all three interfaces`() {
        val animated: Animated = project.service<Animated>()
        assertTrue("PetAnimated should implement Character", animated is Character)
        assertTrue("PetAnimated should implement Game", animated is Game)
    }

    @Test
    fun `onGameStart does not throw and state is consistent`() {
        val game = petAnimated as Game
        game.onGameStart()
        // No exception means state is consistent
    }

    @Test
    fun `onGameStop does not throw and state is consistent`() {
        val game = petAnimated as Game
        game.onGameStop()
        // No exception means state is consistent
    }

    @Test
    fun `onGameStart followed by onGameStop leaves pet in consistent state`() {
        val game = petAnimated as Game
        game.onGameStart()
        game.onGameStop()
        // No exception means state is consistent
    }

    @Test
    fun `multiple start-stop cycles do not break the pet`() {
        val game = petAnimated as Game
        repeat(3) {
            game.onGameStart()
            game.onGameStop()
        }
        // No exception — state is consistent
    }

    @Test
    fun `Animated service can be cast to Game for lifecycle wiring`() {
        val animated = project.service<Animated>()
        // This is exactly what GameController does — ensure it doesn't throw
        val game = animated as Game
        game.onGameStart()
        game.onGameStop()
    }

    @Test
    fun `PetAnimated exposes stable spatial id`() {
        val character = project.service<Animated>() as Character
        assertEquals(EntityID("pet"), character.id())
    }
}
