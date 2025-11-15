package dev.stillya.vpet.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(
	name = "VPetSettings",
	storages = [Storage("vpet.xml")]
)
class VPetSettings : PersistentStateComponent<VPetSettings.State> {
	private var state = State()

	var xmasModeEnabled: Boolean
		get() = state.xmasModeEnabled
		set(value) {
			state.xmasModeEnabled = value
		}

	companion object {
		@JvmStatic
		fun getInstance(): VPetSettings = service<VPetSettings>()
	}

	override fun getState(): State = state

	override fun loadState(state: State) {
		this.state = state
	}

	data class State(
		var xmasModeEnabled: Boolean = true
	)
}
