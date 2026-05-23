package dev.stillya.vpet.game.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.event.KeyEvent

class InputTrackerTest {

    private lateinit var tracker: InputTracker

    @Before
    fun setUp() {
        tracker = InputTracker()
    }

    @Test
    fun `no input when no keys pressed`() {
        val input = tracker.snapshot()
        assertEquals(0, input.moveDirection)
        assertFalse(input.jumpJustPressed)
    }

    @Test
    fun `left key produces negative move direction`() {
        tracker.press(KeyEvent.VK_LEFT)
        assertEquals(-1, tracker.snapshot().moveDirection)
    }

    @Test
    fun `right key produces positive move direction`() {
        tracker.press(KeyEvent.VK_RIGHT)
        assertEquals(1, tracker.snapshot().moveDirection)
    }

    @Test
    fun `both left and right held produces no movement`() {
        tracker.press(KeyEvent.VK_LEFT)
        tracker.press(KeyEvent.VK_RIGHT)
        assertEquals(0, tracker.snapshot().moveDirection)
    }

    @Test
    fun `space produces jump just pressed on first frame`() {
        tracker.press(KeyEvent.VK_SPACE)
        assertTrue(tracker.snapshot().jumpJustPressed)
    }

    @Test
    fun `up key produces jump just pressed on first frame`() {
        tracker.press(KeyEvent.VK_UP)
        assertTrue(tracker.snapshot().jumpJustPressed)
    }

    @Test
    fun `jump not reported again while key held`() {
        tracker.press(KeyEvent.VK_SPACE)
        tracker.snapshot() // first frame: just pressed
        assertFalse(tracker.snapshot().jumpJustPressed) // still held: no edge
    }

    @Test
    fun `jump registers again after key release`() {
        tracker.press(KeyEvent.VK_SPACE)
        tracker.snapshot()                    // press
        tracker.release(KeyEvent.VK_SPACE)
        tracker.snapshot()                    // release
        tracker.press(KeyEvent.VK_SPACE)
        assertTrue(tracker.snapshot().jumpJustPressed) // re-press: edge detected
    }

    @Test
    fun `movement and jump can be combined`() {
        tracker.press(KeyEvent.VK_RIGHT)
        tracker.press(KeyEvent.VK_SPACE)
        val input = tracker.snapshot()
        assertEquals(1, input.moveDirection)
        assertTrue(input.jumpJustPressed)
    }

    @Test
    fun `reset clears all state`() {
        tracker.press(KeyEvent.VK_RIGHT)
        tracker.press(KeyEvent.VK_SPACE)
        tracker.snapshot() // advance prevActions
        tracker.reset()
        val input = tracker.snapshot()
        assertEquals(0, input.moveDirection)
        assertFalse(input.jumpJustPressed)
    }

    @Test
    fun `release removes action`() {
        tracker.press(KeyEvent.VK_LEFT)
        tracker.release(KeyEvent.VK_LEFT)
        assertEquals(0, tracker.snapshot().moveDirection)
    }

    @Test
    fun `unknown key code is ignored`() {
        tracker.press(KeyEvent.VK_A)
        val input = tracker.snapshot()
        assertEquals(0, input.moveDirection)
        assertFalse(input.jumpJustPressed)
    }
}
