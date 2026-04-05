# Refactor Game Engine to Mature ECS Architecture

## Overview
Refactor the game engine from a single flat package to a well-structured ECS architecture organized by system layers (ecs/, physics/, rendering/, input/). Introduce shared Animation resources with AnimationComponent referencing them (not duplicating). Separate rendering logic from JComponent concerns. Fix dependency inversion violations and improve EntityID integration with ECS registry.

## Context
- Files involved: All files in src/main/kotlin/dev/stillya/vpet/game/
- Related patterns: Modern ECS rendering (Bevy, Unity ECS, LibGDX), shared resource system, render systems
- Dependencies: Existing IntelliJ Platform SDK, Kotlin coroutines, existing Animation system
- Key insight: Renderer should be a SYSTEM that queries entities, not a JComponent with embedded logic

## Development Approach
- Testing approach: Regular (code first, then tests)
- Complete each task fully before moving to the next
- Follow ECS best practices: shared resources, component references resources by ID/type, systems query and process
- CRITICAL: every task MUST include new/updated tests
- CRITICAL: all tests must pass before starting next task

## Implementation Steps

### Task 1: Restructure packages by system layer

**Files:**
- Move: All game/* files to appropriate subdirectories

- [x] Create package structure: dev.stillya.vpet.game.ecs/, .physics/, .rendering/, .input/, .resources/
- [x] Move EntityRegistry, Spatial, SpatialGrid, World to game/ecs/
- [x] Move Components, CollectibleComponents to game/ecs/components/
- [x] Move CollisionSystem, CoinSpawner to game/ecs/systems/
- [x] Move PhysicsBody, AABB to game/physics/
- [x] Move GameRenderer, VisualColumnMapper to game/rendering/
- [x] Move InputState to game/input/
- [x] Keep Character, CharacterIntent, TickContext, GamePhase at game/ root (OOP interface layer)
- [x] Keep WorldUpdate, GameEngine, GameController, Game at game/ root (orchestration layer)
- [x] Keep TileMapSyncer, VirtualTileMap at game/ root (tilemap logic)
- [x] Update all imports in moved files and files referencing them
- [x] Update test file imports
- [x] run project test suite - must pass before task 2

### Task 2: Create shared Animation resource system

**Files:**
- Create: game/resources/AnimationResource.kt
- Create: game/resources/AnimationCache.kt
- Create: game/ecs/components/AnimationComponent.kt

- [x] Create AnimationResource data class (id: String, animation: Animation, frames: List<BufferedImage>)
- [x] Create AnimationCache singleton object that loads and caches AnimationResource instances
- [x] AnimationCache has loadAnimation(path, tag) that returns AnimationResource with pre-extracted frames
- [x] Add coin animation loading to AnimationCache with a specific ID like "coin_idle"
- [x] Create AnimationComponent data class (resourceId: String, currentFrame: Int, elapsed: Float)
- [x] AnimationComponent references resource by ID string, not by holding full Animation instance
- [x] write tests for AnimationResource and AnimationCache singleton
- [x] write tests for AnimationComponent
- [x] run project test suite - must pass before task 3

### Task 3: Update CoinSpawner to use shared animation resource

**Files:**
- Modify: game/ecs/systems/CoinSpawner.kt
- Modify: game/ecs/components/CollectibleComponents.kt

- [x] Pre-load coin animation resource in AnimationCache before spawning (singleton, loaded once)
- [x] Remove CoinVisual marker component entirely
- [x] Update CoinSpawner to add AnimationComponent(resourceId="coin_idle") to spawned coins
- [x] All coins share the same animation resource via the "coin_idle" ID
- [x] write tests for updated CoinSpawner with AnimationComponent
- [x] run project test suite - must pass before task 4

### Task 4: Create RenderSystem separate from JComponent

**Files:**
- Create: game/rendering/RenderSystem.kt
- Modify: game/rendering/GameRenderer.kt
- Modify: game/GameEngine.kt

- [x] Create RenderSystem class with render(g2d: Graphics2D, world: World, animation: Animation, tileMap: VirtualTileMap, editor: Editor, bounds: IntRange)
- [x] Move all rendering logic from GameRenderer.paintComponent to RenderSystem.render
- [x] RenderSystem queries AnimationComponent entities and renders them using AnimationCache.get(resourceId)
- [x] RenderSystem renders player using passed Animation (Character/OOP path) and AnimationComponent entities (ECS path)
- [x] Keep GameRenderer as lightweight JComponent wrapper that calls renderSystem.render(g2d, ...) in paintComponent
- [x] GameRenderer holds state (world, animation, tileMap, bounds) and passes to RenderSystem
- [x] Remove all lazy coinFrames loading from GameRenderer (now in AnimationCache)
- [x] Remove frameCache/flippedFrameCache from GameRenderer (now in AnimationResource)
- [x] write tests for RenderSystem render logic
- [x] run project test suite - must pass before task 5

### Task 5: Create AnimationSystem for entity animation updates

**Files:**
- Create: game/ecs/systems/AnimationSystem.kt
- Modify: game/WorldUpdate.kt

- [ ] Create AnimationSystem with updateAnimations(registry: EntityRegistry, dt: Float)
- [ ] AnimationSystem queries all entities with AnimationComponent
- [ ] For each entity, advance elapsed time and update currentFrame based on AnimationResource.animation.sheet frame duration
- [ ] Lookup AnimationResource from AnimationCache using component.resourceId
- [ ] Call AnimationSystem.updateAnimations() in WorldUpdate.tick() after physics, before rendering
- [ ] write tests for AnimationSystem frame advancement with multiple entities
- [ ] write integration test verifying AnimationSystem works in tick()
- [ ] run project test suite - must pass before task 6

### Task 6: Fix DIP violations in atlas loading

**Files:**
- Modify: game/resources/AnimationCache.kt
- Modify: game/GameEngine.kt

- [ ] Add AtlasLoader parameter to AnimationCache.loadAnimation or make AnimationCache configurable with AtlasLoader
- [ ] Remove direct AsepriteJsonAtlasLoader() instantiation from AnimationCache
- [ ] Inject service<AtlasLoader>() from GameEngine or pass as parameter
- [ ] Ensure all resource loading goes through injected AtlasLoader, not hardcoded implementation
- [ ] Update tests to inject mock/test AtlasLoader into AnimationCache
- [ ] run project test suite - must pass before task 7

### Task 7: Improve EntityID integration with registry

**Files:**
- Modify: game/ecs/Spatial.kt
- Modify: game/Character.kt
- Modify: pet/PetAnimated.kt
- Modify: game/ecs/EntityRegistry.kt
- Modify: game/WorldUpdate.kt

- [ ] Ensure Spatial.id() returns EntityID that is the primary key in EntityRegistry
- [ ] Update Character interface documentation to clarify it uses Spatial.id() for registry lookups
- [ ] Verify PetAnimated stores and returns EntityID consistently via Spatial.id()
- [ ] Ensure WorldUpdate uses character.id() when querying registry for components
- [ ] Add validation test that Character's EntityID matches player EntityID in World.registry
- [ ] Add test verifying components can be retrieved from registry using character.id()
- [ ] run project test suite - must pass before task 8

### Task 8: Verify acceptance criteria

- [ ] run full test suite (./gradlew test)
- [ ] verify all tests pass
- [ ] verify no compilation errors
- [ ] verify game mode works in plugin sandbox (./gradlew runIde)
- [ ] verify coins display with shared AnimationComponent (all use same resource)
- [ ] verify Character-based entities (player) still work with OOP animation control
- [ ] verify AnimationCache shows singleton behavior (resource loaded once, shared by all coins)

### Task 9: Update documentation

- [ ] update MEMORY.md with new package structure, AnimationCache pattern, RenderSystem approach
- [ ] update CLAUDE.md with new architecture (shared resources, AnimationComponent, RenderSystem)
- [ ] move this plan to docs/plans/completed/
