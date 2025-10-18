package dev.stillya.vpet.pet

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import dev.stillya.vpet.graphics.*
import java.awt.Image
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlin.random.Random

@Service
class PetAnimated(
	private val injectedRenderer: IconRenderer? = null,
	private val injectedAtlasLoader: AtlasLoader? = null
) : Animated {
	private val log = Logger.getInstance(PetAnimated::class.java)
	private val atlasLoader: AtlasLoader
		get() = injectedAtlasLoader ?: service<AsepriteJsonAtlasLoader>()

	private val renderer: IconRenderer
		get() = injectedRenderer ?: service<DefaultIconRenderer>()

	private var transitionMatrix: TransitionMatrix
	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image
	private var currentState: AnimationState = AnimationState.IDLE

	private val stateTracker = StateTracker()
	private val bridgeSet = BridgeSet.createDefaultCatBridges()
	private lateinit var executor: Executor

	companion object {
		const val INFINITE = -1
		const val SHORT_LOOP = 5
		const val MEDIUM_LOOP = 8
	}

	init {
		transitionMatrix = buildTransitionMatrix()
		executor = Executor(stateTracker, bridgeSet)
	}

	override fun init(params: Animated.Params) {
		log.trace("Initializing PetAnimated with atlas: ${params.atlasPath}, image: ${params.imgPath}")
		atlas = atlasLoader.load(params.atlasPath)
			?: throw IllegalArgumentException("Atlas not found")
		image = loadImage(params.imgPath)

		val context = renderer.createAnimationContext(
			AnimationTrigger.IDLE_BEHAVIOR,
			AnimationState.IDLE
		)
		log.trace("Starting initial transition to IDLE state")
		playTransition(transitionMatrix.transitionTo(AnimationState.IDLE), context)
	}

	private fun playTransition(
		steps: List<AnimationStep>,
		context: AnimationContext? = null
	) {
		if (steps.isEmpty()) {
			log.trace("Empty transition steps, skipping")
			return
		}

		log.trace(
			"Playing transition sequence [${context?.targetState}]: ${
				steps.mapIndexed { idx, step ->
					val tag =
						if (step.variants.isNotEmpty()) "${step.variants}" else step.animationTag
					"[$idx]$tag(loops=${step.loops})"
				}.joinToString(" → ")
			}"
		)

		context?.targetState?.let { currentState = it }

		val animations = steps.mapIndexed { index, step ->
			val tag = if (step.variants.isNotEmpty()) {
				step.variants[Random.nextInt(step.variants.size)].also {
					log.trace("  Step $index: Selected '$it' from variants ${step.variants}")
				}
			} else {
				step.animationTag.also {
					log.trace("  Step $index: Animation '$it' (${if (step.isTransition) "transition" else "play"})")
				}
			}

			val onFinish = {
				executor.applyStepEffect(step)

				if ((index == steps.size - 1) && step.loops != INFINITE) {
					log.trace("Animation sequence completed, returning to stable idle state")
					val idleContext = renderer.createAnimationContext(
						AnimationTrigger.IDLE_BEHAVIOR,
						AnimationState.IDLE
					)
					playTransition(
						transitionMatrix.transitionTo(AnimationState.IDLE),
						idleContext
					)
				}
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

		log.trace("Enqueueing first animation: ${animations.first().name}")
		renderer.enqueue(animations.first())
	}

	override fun onFail() {
		val fromState = if (Random.nextInt(2) == 0) {
			AnimationState.IDLE
		} else {
			AnimationState.SITTING
		}

		log.trace("BUILD FAILED - Transitioning from $fromState to FAILED")
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_FAIL,
			AnimationState.FAILED
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.FAILED), context)
	}

	override fun onSuccess() {
		log.trace("BUILD SUCCESS - Transitioning to CELEBRATING")
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_SUCCESS,
			AnimationState.CELEBRATING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.CELEBRATING), context)
	}

	override fun onProgress() {
		log.trace("BUILD START - Transitioning to RUNNING")
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_START,
			AnimationState.RUNNING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.RUNNING), context)
	}

	override fun onCompleted() {
		log.trace("BUILD COMPLETED - Transitioning to CELEBRATING")
		val context = renderer.createAnimationContext(
			AnimationTrigger.BUILD_SUCCESS,
			AnimationState.CELEBRATING
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.CELEBRATING), context)
	}

	override fun onOccasion() {
		log.trace("USER CLICK - Transitioning to OCCASION")
		val context = renderer.createAnimationContext(
			AnimationTrigger.USER_CLICK,
			AnimationState.OCCASION
		)
		playTransition(transitionMatrix.transitionTo(AnimationState.OCCASION), context)
	}

	fun buildTransitionMatrix(): TransitionMatrix = transitions {
		idle(AnimationState.IDLE, sequence {
			playRandom("Idle", "Sit", "Dream", "Rest", loops = INFINITE)
		})

		from(AnimationState.IDLE) to AnimationState.GROOMING via sequence {
			transition("Sit_Up")
			play("Scratching", loops = SHORT_LOOP)
			play("Bow_Sit", loops = 1)
			transition("Sit_Down")
		}

		from(AnimationState.IDLE) to AnimationState.STRETCHING via sequence {
			play("Bow_Idle", loops = 1)
			play("Paws", loops = 1)
		}

		from(AnimationState.IDLE) to AnimationState.WALKING via sequence {
			play("Walk", loops = SHORT_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.SITTING via sequence {
			transition("Sit_Up", effect = StateEffect.setPose(Pose.STAND))
			play("Aggress", loops = 1)
			transition("Sit_Down", effect = StateEffect.setPose(Pose.SIT))
		}

		from(AnimationState.IDLE) to AnimationState.EATING via sequence {
			play("Eat", loops = MEDIUM_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.LOOKING_AROUND via sequence {
			play("Paws", loops = 1)
			play("rook_around", loops = 1)
		}

		from(AnimationState.IDLE) to AnimationState.RUNNING via sequence {
			transition("Walk_Run", effect = StateEffect.setSpeed(0.8f))
			playInfinite(
				"Run",
				guard = AnimationGuard.buildGuard(),
				effect = StateEffect.setPose(Pose.STAND) + StateEffect.setSpeed(0.8f)
			)
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

		from(AnimationState.IDLE) to AnimationState.FAILED via sequence {
			play("Dmg")
			playRandom("Death_1", "Death_2")
			play("Deat_End", loops = 10)
			play("Spawn_2")
		}

		from(AnimationState.SITTING) to AnimationState.FAILED via sequence {
			play("Pooping", effect = StateEffect.setPose(Pose.SIT))
			play("Dig", loops = MEDIUM_LOOP)
		}

		from(AnimationState.IDLE) to AnimationState.OCCASION via sequence {
			playRandom("Paws", "Pac-Cat", "Goomba", "J_1", "rook_around", loops = 3)
		}

		from(AnimationState.RUNNING) to AnimationState.IDLE via sequence {
			transition("Stop", effect = StateEffect.stop())
			transition("Walk", effect = StateEffect.setSpeed(0.5f))
			play("Walk", loops = 1)
		}

		from(AnimationState.RUNNING) to AnimationState.CELEBRATING via sequence {
			transition("Stop", effect = StateEffect.stop())
			play("J_1", loops = SHORT_LOOP)
		}

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