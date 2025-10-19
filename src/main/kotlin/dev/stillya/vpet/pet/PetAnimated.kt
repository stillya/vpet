package dev.stillya.vpet.pet

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import dev.stillya.vpet.AtlasLoader
import dev.stillya.vpet.IconRenderer
import dev.stillya.vpet.animation.Animation
import dev.stillya.vpet.animation.AnimationGuard
import dev.stillya.vpet.animation.AnimationPlayer
import dev.stillya.vpet.animation.AnimationSequenceWithRequirement
import dev.stillya.vpet.animation.AnimationState
import dev.stillya.vpet.animation.Bridge
import dev.stillya.vpet.animation.BridgeGuard
import dev.stillya.vpet.animation.INFINITE
import dev.stillya.vpet.animation.MEDIUM_LOOP
import dev.stillya.vpet.animation.NO_SPEED
import dev.stillya.vpet.animation.Pose
import dev.stillya.vpet.animation.RUNNING_SPEED
import dev.stillya.vpet.animation.SHORT_LOOP
import dev.stillya.vpet.animation.SequenceRequirement
import dev.stillya.vpet.animation.StateEffect
import dev.stillya.vpet.animation.TransitionMatrix
import dev.stillya.vpet.animation.WALKING_SPEED
import dev.stillya.vpet.animation.sequence
import dev.stillya.vpet.animation.transitions
import dev.stillya.vpet.config.AsepriteJsonAtlasLoader
import dev.stillya.vpet.config.SpriteSheetAtlas
import dev.stillya.vpet.graphics.AnimationContext
import dev.stillya.vpet.graphics.AnimationTrigger
import dev.stillya.vpet.graphics.DefaultIconRenderer
import dev.stillya.vpet.graphics.SpriteSheet
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

	private var animationPlayer: AnimationPlayer

	private var transitionMatrix: TransitionMatrix = buildTransitionMatrix()

	@Volatile
	private var currentState: AnimationState = AnimationState.IDLE
	private lateinit var atlas: SpriteSheetAtlas
	private lateinit var image: Image

	private val bridges: List<Bridge> = buildBridges()

	init {
		animationPlayer = AnimationPlayer(bridges)
	}

	override fun init(params: Animated.Params) {
		log.trace("Initializing PetAnimated with atlas: ${params.atlasPath}, image: ${params.imgPath}")
		atlas = atlasLoader.load(params.atlasPath)
			?: throw IllegalArgumentException("Atlas not found")
		image = loadImage(params.imgPath)

		val context = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
		log.trace("Starting initial transition to IDLE state")
		val idleSequence = transitionMatrix.transitionTo(currentState, AnimationState.IDLE)

		animationPlayer.setInitialEffect(idleSequence.first.requirement.toEffect())

		playTransition(idleSequence, context)
	}

	private fun playTransition(
		sequence: Pair<AnimationSequenceWithRequirement, AnimationState>,
		context: AnimationContext? = null
	) {
		val steps = animationPlayer.buildPlaylist(sequence.first)

		if (steps.isEmpty()) {
			log.trace("Empty transition steps, skipping")
			return
		}

		log.trace(
			"Playing transition sequence [${sequence.second}]: ${
				steps.mapIndexed { idx, step ->
					val tag =
						if (step.variants.isNotEmpty()) "${step.variants}" else step.animationTag
					"[$idx]$tag(loops=${step.loops})"
				}.joinToString(" → ")
			}"
		)

		currentState = sequence.second

		val animations = steps.mapIndexed { index, step ->
			val tag = if (step.variants.isNotEmpty()) {
				step.variants[Random.nextInt(step.variants.size)].also {
					log.trace("Step $index: Selected '$it' from variants ${step.variants}")
				}
			} else {
				step.animationTag.also {
					log.trace("Step $index: Animation '$it' (${if (step.isTransition) "transition" else "play"})")
				}
			}

			val onFinish = {
				if ((index == steps.size - 1) && step.loops != INFINITE) {
					log.trace("Animation sequence completed, returning to stable idle state")
					val idleContext = renderer.createAnimationContext(AnimationTrigger.IDLE_BEHAVIOR)
					playTransition(
						transitionMatrix.transitionTo(currentState, AnimationState.IDLE),
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
		log.trace("BUILD FAILED - Transitioning to FAILED")
		val context = renderer.createAnimationContext(AnimationTrigger.BUILD_FAIL)
		playTransition(transitionMatrix.transitionTo(currentState, AnimationState.FAILED), context)
	}

	override fun onSuccess() {
		log.trace("BUILD SUCCESS - Transitioning to CELEBRATING")
		val context = renderer.createAnimationContext(AnimationTrigger.BUILD_SUCCESS)
		playTransition(transitionMatrix.transitionTo(currentState, AnimationState.CELEBRATING), context)
	}

	override fun onProgress() {
		log.trace("BUILD START - Transitioning to RUNNING")
		val context = renderer.createAnimationContext(AnimationTrigger.BUILD_START)
		playTransition(transitionMatrix.transitionTo(currentState, AnimationState.RUNNING), context)
	}

	override fun onCompleted() {
		log.trace("BUILD COMPLETED - Transitioning to CELEBRATING")
		val context = renderer.createAnimationContext(AnimationTrigger.BUILD_SUCCESS)
		playTransition(transitionMatrix.transitionTo(currentState, AnimationState.CELEBRATING), context)
	}

	override fun onOccasion() {
		log.trace("USER CLICK - Transitioning to OCCASION")
		val context = renderer.createAnimationContext(AnimationTrigger.USER_CLICK)
		playTransition(transitionMatrix.transitionTo(currentState, AnimationState.OCCASION), context)
	}

	private fun buildTransitionMatrix(): TransitionMatrix = transitions {
		idle(
			sequence {
				require { pose = Pose.LIE }
				playRandom("Dream", "Rest", loops = INFINITE)
			},
			sequence {
				require { pose = Pose.SIT }
				playRandom("Sit", loops = INFINITE)
			},
			sequence {
				require { pose = Pose.STAND }
				playRandom("Idle", loops = INFINITE)
			}
		)

		from(AnimationState.IDLE) to AnimationState.RUNNING via sequence {
			require {
				pose = Pose.STAND
				speed = WALKING_SPEED
			}
			playInfinite(
				"Run",
				guard = AnimationGuard.buildGuard(),
				effect = StateEffect(pose = Pose.STAND, speed = RUNNING_SPEED)
			)
		}

//		from(AnimationState.IDLE) to AnimationState.OCCASION via sequence {
//			playRandom("Paws", "Pac-Cat", "Goomba", "J_1", "rook_around", loops = 3)
//		}

		from(AnimationState.RUNNING) to AnimationState.CELEBRATING via sequence {
			require {
				pose = Pose.STAND
				speed = WALKING_SPEED
			}
			transition("Stop", effect = StateEffect.stop())
			play("J_1", loops = SHORT_LOOP, effect = StateEffect.setPose(Pose.STAND))
		}

		from(AnimationState.RUNNING) to AnimationState.FAILED via sequence {
			// no require intended, can be from any running state
			play("Dmg")
			playRandom("Death_1", "Death_2")
			play("Deat_End", loops = MEDIUM_LOOP)
			play("Spawn_2", effect = StateEffect(pose = Pose.STAND, speed = 0f))
		}
	}

	private fun buildBridges(): List<Bridge> = listOf(
		// Lying bridges
		Bridge(
			name = "Lie_to_Stand",
			animationTag = "J_1",
			guard = BridgeGuard.requirePose(Pose.LIE),
			effect = StateEffect.setPose(Pose.STAND)
		),
		Bridge(
			name = "Stand_to_Lie",
			animationTag = "Sit_Rest",
			guard = BridgeGuard.requirePose(Pose.STAND),
			effect = StateEffect.setPose(Pose.LIE)
		),
		// Sitting bridges
		Bridge(
			name = "Sit_to_Stand",
			animationTag = "Sit_Up",
			guard = BridgeGuard.requirePose(Pose.SIT),
			effect = StateEffect.setPose(Pose.STAND)
		),
		Bridge(
			name = "Stand_to_Sit",
			animationTag = "Sit_Down",
			guard = BridgeGuard.requirePose(Pose.STAND),
			effect = StateEffect.setPose(Pose.SIT)
		),
		// Speed bridges
		Bridge(
			name = "NoSpeed_to_Walk",
			animationTag = "Walk",
			guard = BridgeGuard.requireSpeed(NO_SPEED),
			effect = StateEffect.setSpeed(WALKING_SPEED)
		),
		Bridge(
			name = "Walk_to_Run",
			animationTag = "Walk_Run",
			guard = BridgeGuard.requireSpeed(WALKING_SPEED),
			effect = StateEffect.setSpeed(RUNNING_SPEED)
		),
		Bridge(
			name = "Run_to_Walk",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(RUNNING_SPEED),
			effect = StateEffect.setSpeed(WALKING_SPEED)
		),
		Bridge(
			name = "Walk_to_NoSpeed",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(WALKING_SPEED),
			effect = StateEffect.setSpeed(NO_SPEED)
		),
		Bridge(
			name = "Run_to_NoSpeed",
			animationTag = "Stop",
			guard = BridgeGuard.requireSpeed(RUNNING_SPEED),
			effect = StateEffect.setSpeed(NO_SPEED)
		)
	)

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

	private fun SequenceRequirement.toEffect(): StateEffect {
		return StateEffect(
			pose = this.pose,
			speed = this.speed,
			direction = this.direction
		)
	}
}