package dev.stillya.vpet.graphics

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import dev.stillya.vpet.listener.BuildEventListener
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.random.Random

@Service
class PetAnimated : Animated {
	private val atlasLoader: AtlasLoader
		get() = service<AsepriteJsonAtlasLoader>()

	private val renderer: IconRenderer
		get() = service<DefaultIconRenderer>()

	private var transitionMatrix: TransitionMatrix
	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image

	companion object {
		const val INFINITE = -1
		const val SHORT_LOOP = 5
		const val MEDIUM_LOOP = 8
	}

	init {
		transitionMatrix = buildTransitionMatrix()
	}

	override fun init(params: Animated.Params) {
		atlas = atlasLoader.load(params.atlasPath)
			?: throw IllegalArgumentException("Atlas not found")
		image = loadImage(params.imgPath)

		val context = renderer.createAnimationContext(
			AnimationTrigger.IDLE_BEHAVIOR,
			AnimationState.IDLE
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.IDLE), context)
	}

	private fun buildTransitionMatrix(): TransitionMatrix = transitions {
		// Idle state
		idle(AnimationState.IDLE, sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		})

		// Idle → Active states
		from(AnimationState.IDLE) to AnimationState.GROOMING via sequence {
			transition("Sit_Up")
			play("Scratching", loops = SHORT_LOOP)
			transition("Sit_Down")
		}

		from(AnimationState.IDLE) to AnimationState.STRETCHING via sequence {
			play("Paws", loops = 2)
		}

		from(AnimationState.IDLE) to AnimationState.WALKING via sequence {
			play("Walk", loops = SHORT_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.SITTING via sequence {
			transition("Sit_Up")
			play("Aggress", loops = 1)
			transition("Sit_Down")
		}

		from(AnimationState.IDLE) to AnimationState.EATING via sequence {
			play("Eat", loops = MEDIUM_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.LOOKING_AROUND via sequence {
			play("Paws", loops = 1)
			play("rook_around", loops = 1)
		}

		// Build events
		from(AnimationState.IDLE) to AnimationState.RUNNING via sequence {
			transition("Walk_Run")
			playInfinite("Run", guard = AnimationGuard.buildGuard())
		}

		from(AnimationState.IDLE) to AnimationState.CELEBRATING via sequence {
			play("Paws", loops = 2)
			playRandom("J_1", "J_U_D", loops = SHORT_LOOP)
			playRandom(
				"Attack_1",
				"Attack_2",
				"Attack_3",
				"Attack_4",
				"Attack_5",
				loops = 3
			)
			play("Walk", loops = 3)
		}

		// Failure animations (death variant)
		from(AnimationState.IDLE) to AnimationState.FAILED via sequence {
			play("Dmg")
			playRandom("Death_1", "Death_2")
			play("Deat_End", loops = 10)
			play("Spawn_2")
		}

		// Failure animations (pooping variant)
		from(AnimationState.SITTING) to AnimationState.FAILED via sequence {
			play("Pooping")
			play("Dig", loops = MEDIUM_LOOP)
		}

		// User interaction
		from(AnimationState.IDLE) to AnimationState.OCCASION via sequence {
			playRandom("Paws", "Pac-Cat", "Goomba", "J_1", "rook_around", loops = 3)
		}

		// Running → transitions
		from(AnimationState.RUNNING) to AnimationState.IDLE via sequence {
			transition("Stop")
			transition("Walk")
			play("Walk", loops = 1)
		}

		from(AnimationState.RUNNING) to AnimationState.CELEBRATING via sequence {
			transition("Stop")
			play("J_1", loops = SHORT_LOOP)
		}

		// Return to idle transitions
		from(AnimationState.GROOMING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.STRETCHING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.WALKING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.SITTING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.CELEBRATING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.FAILED) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.OCCASION) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.EATING) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}

		from(AnimationState.LOOKING_AROUND) to AnimationState.IDLE via sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		}
	}

	private fun playTransition(
		steps: List<AnimationStep>,
		context: AnimationContext? = null
	) {
		if (steps.isEmpty()) return

		val animations = steps.mapIndexed { index, step ->
			val tag = if (step.variants.isNotEmpty()) {
				step.variants[Random.nextInt(step.variants.size)]
			} else {
				step.animationTag
			}

			val isLast = index == steps.size - 1
			val onFinish: () -> Unit = if (isLast && step.loops != INFINITE) {
				{
					val idleContext = renderer.createAnimationContext(
						AnimationTrigger.IDLE_BEHAVIOR,
						AnimationState.IDLE
					)
					playTransition(transitionMatrix.getRandomIdleBehavior(), idleContext)
				}
			} else {
				{}
			}

			Animation(
				name = tag,
				loop = step.loops,
				sheet = createSpriteSheet(tag),
				onFinish = onFinish,
				context = context,
				guard = step.guard
			)
		}

		animations.forEachIndexed { index, animation ->
			if (index < animations.size - 1) {
				animation.nextAnimation = animations[index + 1]
			}
		}

		renderer.enqueue(animations.first())
	}

	override fun onFail() {
		val fromState = if (Random.nextInt(2) == 0) {
			AnimationState.IDLE
		} else {
			AnimationState.SITTING
		}

		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_FAIL,
			AnimationState.FAILED
		)
		val transition = transitionMatrix.getTransition(fromState, AnimationState.FAILED)
		playTransition(transition, context)
	}

	override fun onSuccess() {
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_SUCCESS,
			AnimationState.CELEBRATING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.CELEBRATING), context)
	}

	override fun onProgress() {
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_START,
			AnimationState.RUNNING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.RUNNING), context)
	}

	override fun onCompleted() {
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_SUCCESS,
			AnimationState.CELEBRATING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.CELEBRATING), context)
	}

	override fun onOccasion() {
		val context = renderer.createAnimationContext(
			AnimationTrigger.USER_CLICK,
			AnimationState.OCCASION
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.OCCASION), context)
	}

	private fun createSpriteSheet(animationTag: String): SpriteSheet {
		val frameTag = atlas.meta.frameTags.find { it.name == animationTag }
			?: throw IllegalArgumentException("Animation tag not found: $animationTag")

		return SpriteSheet(
			image = image,
			frames = atlas.frames.subList(frameTag.from, frameTag.to + 1)
		)
	}

	private fun loadImage(path: String): BufferedImage {
		val inputStream = SpriteSheet::class.java.getResourceAsStream(path)
		return ImageIO.read(inputStream)
	}
}