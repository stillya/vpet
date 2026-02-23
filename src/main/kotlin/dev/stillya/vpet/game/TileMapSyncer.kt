package dev.stillya.vpet.game

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class TileMapSyncer(private val editor: Editor) : Disposable {

	@Volatile
	var tileMap: VirtualTileMap = VirtualTileMap()
		private set

	private val listener = object : DocumentListener {
		override fun documentChanged(event: DocumentEvent) {
			scheduleRebuild()
		}
	}

	fun start() {
		rebuildSync()
		editor.document.addDocumentListener(listener, this)
	}

	override fun dispose() {
		// DocumentListener auto-removed via Disposable parent
	}

	private fun rebuildSync() {
		val newMap = buildFromDocument()
		tileMap = newMap
	}

	private fun scheduleRebuild() {
		ApplicationManager.getApplication().executeOnPooledThread {
			ApplicationManager.getApplication().runReadAction {
				val newMap = buildFromDocument()
				tileMap = newMap
			}
		}
	}

	private fun buildFromDocument(): VirtualTileMap {
		val doc = editor.document
		val chars = doc.charsSequence
		val lineCount = doc.lineCount
		val map = VirtualTileMap()
		map.rebuildFromDocument(lineCount) { line ->
			val start = doc.getLineStartOffset(line)
			val end = doc.getLineEndOffset(line)
			chars.subSequence(start, end).toString()
		}
		return map
	}
}
