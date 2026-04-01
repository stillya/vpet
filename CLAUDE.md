# VPet: Pixel Art Companions for IntelliJ

## What This Project Does

VPet is a delightful IntelliJ IDEA plugin that brings animated pixel art companions to the
IDE's status bar. These interactive pets respond to development activities (builds, tests,
executions) and add personality to the coding environment through sprite-based animations.

## How It Works

```
Developer Activity → Event Listeners → Animation State Machine → Sprite Renderer → Status Bar Widget
        |                   |                    |                      |              |
        | Build/Run         | Captures events    | Selects animation    | Renders      | Displays
        | events            | from IDE           | sequences            | frames       | animated pet
        |------------------>|                    |                      |              |
                            |                    |                      |              |
                            |------------------->| Based on success/    |              |
                                                 | failure/progress     |              |
                                                 |--------------------->|              |
                                                                        |------------->|
```

**Architecture Flow:**

1. **Event Capture**: BuildEventListener monitors IDE build/execution events
2. **State Management**: AnimationEventService broadcasts animation state changes
3. **Animation Selection**: PetAnimated selects appropriate animation sequences based on
   events
4. **Sprite Rendering**: DefaultIconRenderer processes sprite sheet frames
5. **UI Display**: AnimatedStatusBarWidget displays frames in the status bar using
   Flow-based reactive updates

## Architecture

### Core Components

**Status Bar Integration**

- `AnimatedStatusBarWidget`: Main widget implementation using Flow-based icon streaming
- `AnimatedStatusBarWidgetFactory`: Factory for creating widget instances
- Integrates with IntelliJ's StatusBar widget system

**Graphics System**

- `PetAnimated`: Main animation controller with state machine for pet behaviors; implements
  `Animated` (status bar), `Character` (game physics), and `Game` (game lifecycle hooks)
- `DefaultIconRenderer`: Renders sprite sheet frames into Swing Icons with animation queue
  management
- `Animation`: Data class representing animation sequences with looping and chaining
- `SpriteSheet`: Represents a collection of sprite frames from the atlas

**Configuration & Assets**

- `AsepriteJsonAtlasLoader`: Parses Aseprite JSON atlas files for sprite metadata
- `SpriteSheetAtlas`: Data structure for sprite sheet frame definitions
- Assets stored in `/META-INF/spritesheets/` (PNG images + JSON atlases)

**Event System**

- `BuildEventListener`: Captures ProjectTaskListener and ExecutionListener events
- `AnimationEventService`: Service broadcasting animation events to widgets
- `AnimationEventListener`: Interface for animation state change listeners

**Game System**

- `Game`: Interface defining game lifecycle hooks (`onGameStart()`, `onGameStop()`) for
  participants in game mode
- `GameEngine`: Coordinator owning the game loop (Timer at 16ms), input gathering, world
  tick (`WorldUpdate.tick()`), and renderer updates — replaces inline logic from
  `GameController`
- `GameController`: Thin plugin.xml adapter; creates and delegates to `GameEngine` on
  `enterGameMode()` / `exitGameMode()`

### Animation State Machine

The `PetAnimated` class implements a behavior-driven state machine:

- **Idle States**: Random behaviors (grooming, stretching, walking, sitting)
- **Success**: Walking + jumping celebration
- **Failure**: Death animation or pooping/digging sequence
- **Progress**: Running animation (loops infinitely)
- **Completed**: Sit up → attack → walk celebration sequence
- **Occasion**: Special animations triggered by user clicks (Pac-Cat, Goomba)

Animations chain together using `onNext()` and `onFinish()` callbacks.

### Sprite Sheet System

Uses Aseprite format:

- JSON atlas defines frame positions and animation tags
- PNG sprite sheet contains all frames
- Frames extracted at runtime based on atlas coordinates
- Supports multiple animation variants per behavior

## Development Environment

- **JDK**: 17 or newer (required for IntelliJ Platform)
- **Build System**: Gradle 9.1.0 with Kotlin DSL
- **IDE**: IntelliJ IDEA (for plugin development and testing)
- **Platform**: IntelliJ Platform 2023.3.8 (IC - Community Edition)
- **Language**: Kotlin with JVM target 17

### Key Dependencies

- IntelliJ Platform SDK (bundled plugins required)
- Kotlin coroutines for Flow-based reactive UI updates

### Build Configuration

```bash
# Build plugin
./gradlew buildPlugin

# Run plugin in IDE sandbox
./gradlew runIde

# Output location
build/distributions/vpet-{version}.zip
```

## Kotlin Code Style Requirements

### File Organization

- **Imports**: Standard Kotlin/IntelliJ grouping (kotlin, java, javax, com.intellij,
  dev.stillya)
- **Package Structure**: Follow `dev.stillya.vpet.*` namespace convention
- **Services**: Use `@Service` annotation for IntelliJ services with `service<T>()` lookup

### Plugin-Specific Patterns

- **Animation Chaining**: Use `Animation.onNext()` for sequential animations
- **Random Behavior**: Use `kotlin.random.Random` for variant selection
- **Service Lifecycle**: Services are application/project scoped, managed by platform
- **Resource Loading**: Always use classpath-relative paths with leading slash

## Key Constraints

- **Resource Management**: All assets must be in classpath under `/META-INF/`
- **Thread Safety**: UI updates via Flow must run on EDT (Event Dispatch Thread)
- **Plugin Descriptor**: All extensions must be declared in `plugin.xml`
- **Service Lifecycle**: Don't manually instantiate services, use `service<T>()`
- **Status Bar Updates**: Use Flow emission for frame updates, not manual timer

## Plugin Development Notes

- **Testing**: Run via Gradle `runIde` task, not direct build/run
- **Distribution**: Plugin ZIP built via `buildPlugin` task
- **Versioning**: Uses SemVer, configured in `gradle.properties`
- **Icon Format**: 16x16 pixel sprites, rendered at status bar size

## No-Go Zones

- **No Documentation Changes**: Do not modify README.md unless explicitly requested
- **No Asset Creation**: Focus on code, not creating new sprite sheets
- **No Direct Timer Usage**: Always use Flow/coroutines for animation timing
- **No Manual Service Registration**: Services auto-discovered via annotations and
  plugin.xml