package dev.stillya.vpet.game

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightPlatform4TestCase
import org.junit.Test

class CoinCollectedListenerTest : LightPlatform4TestCase() {

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
