package dev.stillya.vpet.settings

import com.intellij.openapi.options.Configurable
import java.awt.BorderLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class VPetSettingsConfigurable : Configurable {
	private var panel: JPanel? = null
	private var xmasModeCheckbox: JCheckBox? = null

	override fun getDisplayName(): String = "VPet"

	override fun createComponent(): JComponent {
		panel = JPanel(BorderLayout())
		xmasModeCheckbox = JCheckBox("Enable Xmas Mode")
		panel!!.add(xmasModeCheckbox!!, BorderLayout.NORTH)
		return panel!!
	}

	override fun isModified(): Boolean {
		val settings = VPetSettings.getInstance()
		return xmasModeCheckbox?.isSelected != settings.xmasModeEnabled
	}

	override fun apply() {
		val settings = VPetSettings.getInstance()
		settings.xmasModeEnabled = xmasModeCheckbox?.isSelected ?: true // true until Xmas is over
	}

	override fun reset() {
		val settings = VPetSettings.getInstance()
		xmasModeCheckbox?.isSelected = settings.xmasModeEnabled
	}

	override fun disposeUIResources() {
		panel = null
		xmasModeCheckbox = null
	}
}
