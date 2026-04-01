package dev.stillya.vpet.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.awt.event.KeyEvent

/**
 * Tests for GameEngine input gathering logic.
 *
 * start()/stop() lifecycle requires a live IntelliJ platform (Editor, IdeEventQueue) and is
 * covered by manual/integration testing. This test suite focuses on the pure gatherInput()
 * logic which has no platform dependencies.
 */
class GameEngineTest {

	private lateinit var engine: GameEngine
	private val keysHeldField by lazy {
		GameEngine::class.java.getDeclaredField("keysHeld").also { it.isAccessible = true }
	}
	private val jumpWasPressedField by lazy {
		GameEngine::class.java.getDeclaredField("jumpWasPressed").also { it.isAccessible = true }
	}

	@Suppress("UNCHECKED_CAST")
	private fun keysHeld(): MutableSet<Int> =
		keysHeldField.get(engine) as MutableSet<Int>

	private fun setJumpWasPressed(value: Boolean) {
		jumpWasPressedField.setBoolean(engine, value)
	}

	@Before
	fun setUp() {
		// editor/character/renderer are not used by gatherInput(); safe to pass null
		engine = GameEngine(
			editor = null,
			character = null,
			renderer = null,
			onExit = {},
		)
	}

	@Test
	fun `gatherInput returns no movement when no keys held`() {
		val input = engine.gatherInput()
		assertEquals(0, input.moveDirection)
		assertFalse(input.jumpJustPressed)
	}

	@Test
	fun `gatherInput returns left movement when left key held`() {
		keysHeld().add(KeyEvent.VK_LEFT)
		val input = engine.gatherInput()
		assertEquals(-1, input.moveDirection)
	}

	@Test
	fun `gatherInput returns right movement when right key held`() {
		keysHeld().add(KeyEvent.VK_RIGHT)
		val input = engine.gatherInput()
		assertEquals(1, input.moveDirection)
	}

	@Test
	fun `gatherInput returns no movement when both left and right held`() {
		keysHeld().add(KeyEvent.VK_LEFT)
		keysHeld().add(KeyEvent.VK_RIGHT)
		val input = engine.gatherInput()
		assertEquals(0, input.moveDirection)
	}

	@Test
	fun `gatherInput detects jump just pressed on first space press`() {
		setJumpWasPressed(false)
		keysHeld().add(KeyEvent.VK_SPACE)
		val input = engine.gatherInput()
		assertTrue(input.jumpJustPressed)
	}

	@Test
	fun `gatherInput does not report jump just pressed when space already held last frame`() {
		keysHeld().add(KeyEvent.VK_SPACE)
		// first call sets jumpWasPressed = true
		engine.gatherInput()
		// second call with space still held: jumpJustPressed should be false
		val input = engine.gatherInput()
		assertFalse(input.jumpJustPressed)
	}

	@Test
	fun `gatherInput detects jump with UP key`() {
		setJumpWasPressed(false)
		keysHeld().add(KeyEvent.VK_UP)
		val input = engine.gatherInput()
		assertTrue(input.jumpJustPressed)
	}

	@Test
	fun `gatherInput jump registers again after key release`() {
		keysHeld().add(KeyEvent.VK_SPACE)
		engine.gatherInput() // press: jumpWasPressed = true
		keysHeld().remove(KeyEvent.VK_SPACE)
		engine.gatherInput() // release: jumpWasPressed = false
		keysHeld().add(KeyEvent.VK_SPACE)
		val input = engine.gatherInput() // press again: justPressed = true
		assertTrue(input.jumpJustPressed)
	}

	@Test
	fun `gatherInput combines movement and jump`() {
		setJumpWasPressed(false)
		keysHeld().add(KeyEvent.VK_RIGHT)
		keysHeld().add(KeyEvent.VK_SPACE)
		val input = engine.gatherInput()
		assertEquals(1, input.moveDirection)
		assertTrue(input.jumpJustPressed)
	}
}
