package dev.stillya.vpet.game

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent
import kotlin.math.floor

class GameOverlayPanel(
    private val editor: Editor,
    private val spriteRenderer: GameSpriteRenderer
) : JComponent() {

    var colX: Float = 0f
    var lineY: Float = 0f
    var facingLeft: Boolean = false
    var animState: GameAnimationState = GameAnimationState.IDLE
    var frameIndex: Int = 0
    var tileMap: GameTileMap? = null
    var isOnGround: Boolean = true

    init {
        isOpaque = false
    }

    override fun contains(x: Int, y: Int): Boolean = false

    private fun logicalToX(line: Int, col: Int): Int =
        editor.logicalPositionToXY(LogicalPosition(line, col)).x

    private fun logicalToX(line: Int, col: Float): Int {
        val colInt = floor(col).toInt()
        val frac = col - colInt
        val x0 = logicalToX(line, colInt)
        val x1 = logicalToX(line, colInt + 1)
        return x0 + ((x1 - x0) * frac).toInt()
    }

    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )

        val lineHeight = editor.lineHeight
        val spriteSize = lineHeight * 2

        val groundLine = floor(lineY).toInt()
        val groundY = editor.logicalPositionToXY(LogicalPosition(groundLine, 0)).y

        drawDebug(g2d, lineHeight, groundLine, groundY)

        val frames = spriteRenderer.getFrames(animState.spriteTag, facingLeft)
        if (frames.isEmpty()) return
        val frame = frames[frameIndex % frames.size]

        val catLeft = floor(colX).toInt()
        val hitboxX = logicalToX(groundLine, catLeft)
        val hitboxEndX = logicalToX(groundLine, catLeft + GamePhysics.CAT_WIDTH)
        val hitboxCenterX = (hitboxX + hitboxEndX) / 2
        val pixelX = hitboxCenterX - spriteSize / 2
        val spriteY = groundY - spriteSize

        g2d.drawImage(frame, pixelX, spriteY, spriteSize, spriteSize, null)
    }

    private fun drawDebug(g2d: Graphics2D, lineHeight: Int, groundLine: Int, groundY: Int) {
        val map = tileMap ?: return
        val oldStroke = g2d.stroke

        // platform extents — green
        g2d.stroke = BasicStroke(1f)
        map.forEachExtent { line, extent ->
            val y = editor.logicalPositionToXY(LogicalPosition(line, 0)).y
            val x = logicalToX(line, extent.first)
            val endX = logicalToX(line, extent.last + 1)
            g2d.color = Color(0, 255, 0, 30)
            g2d.fillRect(x, y, endX - x, lineHeight)
            g2d.color = Color(0, 255, 0, 120)
            g2d.drawRect(x, y, endX - x, lineHeight)
        }

        // ground line — yellow stripe
        g2d.color = Color(255, 255, 0, 25)
        g2d.fillRect(0, groundY, width, lineHeight)

        // cat hitbox on ground — red
        val catLeft = floor(colX).toInt()
        val hitboxX = logicalToX(groundLine, catLeft)
        val hitboxEndX = logicalToX(groundLine, catLeft + GamePhysics.CAT_WIDTH)
        g2d.color = if (isOnGround) Color(255, 0, 0, 180) else Color(255, 100, 0, 180)
        g2d.stroke = BasicStroke(2f)
        g2d.drawRect(hitboxX, groundY, hitboxEndX - hitboxX, lineHeight)

        // body line extent — blue (where wall checks happen in air)
        val bodyLine = groundLine - 1
        if (bodyLine >= 0) {
            val bodyY = editor.logicalPositionToXY(LogicalPosition(bodyLine, 0)).y
            g2d.stroke = BasicStroke(1f)
            g2d.color = Color(0, 100, 255, 40)
            // show only the extent of the body line, not full width
            val bodyExtent = map.getExtent(bodyLine)
            if (bodyExtent != null) {
                val bx = logicalToX(bodyLine, bodyExtent.first)
                val bEndX = logicalToX(bodyLine, bodyExtent.last + 1)
                g2d.fillRect(bx, bodyY, bEndX - bx, lineHeight)
                g2d.color = Color(0, 100, 255, 100)
                g2d.drawRect(bx, bodyY, bEndX - bx, lineHeight)
            }
        }

        g2d.stroke = oldStroke
    }

}
