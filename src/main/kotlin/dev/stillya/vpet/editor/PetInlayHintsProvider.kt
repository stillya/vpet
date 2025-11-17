package dev.stillya.vpet.editor

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.InlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsProvider
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.codeInsight.hints.NoSettings
import com.intellij.codeInsight.hints.SettingsKey
import com.intellij.codeInsight.hints.presentation.PresentationFactory
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import dev.stillya.vpet.AnimatedStatusBarWidget.Companion.DEFAULT_SPRITE_SHEET_ATLAS
import dev.stillya.vpet.AnimatedStatusBarWidget.Companion.DEFAULT_SPRITE_SHEET_IMAGE
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.graphics.SpriteSheet
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JPanel

@Suppress("UnstableApiUsage")
class PetInlayHintsProvider : InlayHintsProvider<NoSettings> {

	override val key: SettingsKey<NoSettings> = SettingsKey("vpet.inlay.hints")
	override val name: String = "VPet Animations"
	override val previewText: String = """
        class Example {
            fun doSomething() {
                // Your pet will appear here!
            }
        }
    """.trimIndent()

	override fun createSettings(): NoSettings = NoSettings()

	override fun getCollectorFor(
		file: PsiFile,
		editor: Editor,
		settings: NoSettings,
		sink: InlayHintsSink
	): InlayHintsCollector {
		return PetInlayHintsCollector(editor)
	}

	override fun createConfigurable(settings: NoSettings): ImmediateConfigurable {
		return object : ImmediateConfigurable {
			override fun createComponent(listener: ChangeListener): JComponent = JPanel()
		}
	}
}

@Suppress("UnstableApiUsage")
class PetInlayHintsCollector(
	private val editor: Editor
) : InlayHintsCollector {

	private val atlasLoader: AtlasLoader
		get() = service()

	companion object {
		private val log = logger<PetInlayHintsCollector>()
	}

	override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
		if (element !is PsiMethod) return true

		log.info("Found method: ${element.name} at offset ${element.textOffset}")

		val textOffset = element.textOffset
		if (textOffset <= 0) return true

		val spriteSheet = loadSpriteSheet()
		if (spriteSheet == null) {
			log.warn("Failed to load sprite sheet")
			return true
		}

		val factory = PresentationFactory(editor)
		val presentation = PetAnimatedPresentation(spriteSheet, factory, editor)

		sink.addBlockElement(
			offset = textOffset,
			relatesToPrecedingText = true,
			showAbove = true,
			priority = 0,
			presentation = presentation
		)

		log.info("Added pet inlay for method: ${element.name}")
		return true
	}

	private fun loadSpriteSheet(): SpriteSheet? {
		val atlas = atlasLoader.load(DEFAULT_SPRITE_SHEET_ATLAS) ?: return null
		val imageStream = javaClass.getResourceAsStream(DEFAULT_SPRITE_SHEET_IMAGE) ?: return null
		val image = ImageIO.read(imageStream) ?: return null

		val idleTag = atlas.meta.frameTags.find { it.name == "Idle" } ?: return null
		val idleFrames = atlas.frames.subList(idleTag.from, idleTag.to + 1)

		return SpriteSheet(image, idleFrames)
	}
}
