package dev.stillya.vpet.game

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightPlatform4TestCase
import org.junit.Test

class CoinCollectedListenerTest : LightPlatform4TestCase() {

	@Test
	fun `GameEngine exposes finalScore from world`() {
		val world = World(score = 42)
		val engine = GameEngine(
			editor = null,
			character = null,
			renderer = null,
			onExit = {},
		)

		val worldField = GameEngine::class.java.getDeclaredField("world").also { it.isAccessible = true }
		worldField.set(engine, world)

		assertEquals(42, engine.finalScore)
	}

	@Test
	fun `CoinCollectedListener TOPIC is properly configured`() {
		val topic = CoinCollectedListener.TOPIC
		assertEquals("CoinCollected", topic.displayName)
		assertEquals(CoinCollectedListener::class.java, topic.listenerClass)
	}

	@Test
	fun `message bus can publish coin collected events`() {
		var receivedCount = -1
		val listener = object : CoinCollectedListener {
			override fun onCoinsCollected(count: Int) {
				receivedCount = count
			}
		}

		val connection = ApplicationManager.getApplication().messageBus.connect()
		try {
			connection.subscribe(CoinCollectedListener.TOPIC, listener)
			ApplicationManager.getApplication().messageBus
				.syncPublisher(CoinCollectedListener.TOPIC)
				.onCoinsCollected(7)
			assertEquals(7, receivedCount)
		} finally {
			connection.disconnect()
		}
	}
}
