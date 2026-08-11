package dev.stillya.vpet.pet

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.managed.modelchecking.ModelCheckingOptions
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class PetAnimatedObservingStateLincheckTest {

	private val isObserving = AtomicBoolean(false)

	@Operation
	fun startObserving(): Boolean {
		return isObserving.compareAndSet(false, true)
	}

	@Operation
	fun exitObserving(): Boolean {
		return isObserving.compareAndSet(true, false)
	}

	@Operation
	fun isCurrentlyObserving(): Boolean {
		return isObserving.get()
	}

	@Test
	fun stressTest() = StressOptions().check(this::class)

	@Test
	fun modelCheckingTest() = ModelCheckingOptions().check(this::class)
}
