package dev.stillya.vpet.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic

enum class CatVariant(val displayName: String, val atlasPath: String, val imagePath: String) {
	DEFAULT("Default Cat", "/META-INF/spritesheets/cat/atlas.json", "/META-INF/spritesheets/cat/sprite.png"),
	ALTERNATIVE(
		"Alternative Cat",
		"/META-INF/spritesheets/alt_cat/atlas.json",
		"/META-INF/spritesheets/alt_cat/sprite.png"
	)
}

interface VPetSettingsListener {
	fun settingsChanged(settings: VPetSettings)
}

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

	var catVariant: CatVariant
		get() = CatVariant.entries.find { it.name == state.catVariantName } ?: CatVariant.DEFAULT
		set(value) {
			if (state.catVariantName != value.name) {
				state.catVariantName = value.name
				notifySettingsChanged()
			}
		}

	companion object {
		@JvmStatic
		fun getInstance(): VPetSettings = service<VPetSettings>()

		@JvmField
		val TOPIC = Topic.create("VPetSettings", VPetSettingsListener::class.java)
	}

	override fun getState(): State = state

	override fun loadState(state: State) {
		this.state = state
	}

	private fun notifySettingsChanged() {
		ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).settingsChanged(this)
	}

	data class State(
		var xmasModeEnabled: Boolean = false,
		var catVariantName: String = CatVariant.DEFAULT.name
	)
}
