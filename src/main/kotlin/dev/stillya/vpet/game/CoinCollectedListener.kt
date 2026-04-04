package dev.stillya.vpet.game

import com.intellij.util.messages.Topic

interface CoinCollectedListener {
	fun onCoinsCollected(count: Int)

	companion object {
		@JvmField
		val TOPIC = Topic.create("CoinCollected", CoinCollectedListener::class.java)
	}
}
