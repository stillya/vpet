package dev.stillya.vpet.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

class VPetSettingsConfigurable : Configurable {
	private var panel: JPanel? = null
	private var xmasModeCheckbox: JCheckBox? = null
	private var catVariantComboBox: JComboBox<CatVariant>? = null

	override fun getDisplayName(): String = "VPet"

	override fun createComponent(): JComponent {
		xmasModeCheckbox = JCheckBox("Enable Xmas Mode")

		catVariantComboBox = ComboBox(CatVariant.entries.toTypedArray())
		catVariantComboBox!!.renderer = object : javax.swing.DefaultListCellRenderer() {
			override fun getListCellRendererComponent(
				list: javax.swing.JList<*>?,
				value: Any?,
				index: Int,
				isSelected: Boolean,
				cellHasFocus: Boolean
			): java.awt.Component {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
				if (value is CatVariant) {
					text = value.displayName
				}
				return this
			}
		}

		panel = FormBuilder.createFormBuilder()
			.addComponent(xmasModeCheckbox!!)
			.addLabeledComponent(JBLabel("Cat variant:"), catVariantComboBox!!)
			.addComponentFillVertically(JPanel(), 0)
			.panel

		return panel!!
	}

	override fun isModified(): Boolean {
		val settings = VPetSettings.getInstance()
		return xmasModeCheckbox?.isSelected != settings.xmasModeEnabled ||
				catVariantComboBox?.selectedItem != settings.catVariant
	}

	override fun apply() {
		val settings = VPetSettings.getInstance()
		settings.xmasModeEnabled = xmasModeCheckbox?.isSelected ?: false
		settings.catVariant = catVariantComboBox?.selectedItem as? CatVariant ?: CatVariant.DEFAULT
	}

	override fun reset() {
		val settings = VPetSettings.getInstance()
		xmasModeCheckbox?.isSelected = settings.xmasModeEnabled
		catVariantComboBox?.selectedItem = settings.catVariant
	}

	override fun disposeUIResources() {
		panel = null
		xmasModeCheckbox = null
		catVariantComboBox = null
	}
}
