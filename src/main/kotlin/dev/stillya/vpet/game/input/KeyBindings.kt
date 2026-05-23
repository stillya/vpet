package dev.stillya.vpet.game.input

import java.awt.event.KeyEvent

object KeyBindings {
    val defaults: Map<Int, GameAction> = mapOf(
        KeyEvent.VK_LEFT  to GameAction.MOVE_LEFT,
        KeyEvent.VK_RIGHT to GameAction.MOVE_RIGHT,
        KeyEvent.VK_UP    to GameAction.JUMP,
        KeyEvent.VK_SPACE to GameAction.JUMP,
    )
}
