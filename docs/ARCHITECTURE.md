# VPet Game Engine Architecture

## Overview

Game engine running inside IntelliJ editor overlay. Editor text becomes the physical world — non-whitespace characters are solid tiles, whitespace is air. A pixel-art pet walks, jumps and collides with code.

Core loop: **Input → Intent → Physics → Frame → Render**

## Data Flow

```
GameController.gameTick() [every 16ms]
    │
    ├── gatherInput() → InputState(moveDirection, jumpJustPressed)
    │
    └── WorldUpdate.tick(world, input, dt, character, tileMap, visibleRange)
            │
            ├── 1. character.update(input, TickContext, dt) → CharacterIntent
            │       Character declares DESIRED velocity + animation tag.
            │       Never touches position. Never resolves collisions.
            │
            ├── 2. PhysicsBody.moveAndSlide(transform, intent.velocity, tileMap, range, dt)
            │       Resolves collisions per-axis. Returns final position + grounded status.
            │
            ├── 3. advanceFrame(tag, direction, sprite, dt) → SpriteState
            │       Advances animation frame every 96ms regardless of game FPS.
            │
            └── 4. GameFrame(world, animation, bounds)
                    │
                    └── GameRenderer.update(frame, tileMap) → paintComponent()
                            Maps tile coordinates → editor pixels via logicalPositionToXY.
```

## Components

### World (`World.kt`)

All game state in one immutable data class. New `World` created every tick — never mutated.

```
World
├── transform: Transform(x, y)     — position in tile coordinates
├── velocity: Velocity(x, y)       — current movement
├── isOnGround: Boolean             — from collision, not stored
├── sprite: SpriteState             — animation tag, frame index, timer, direction
├── phase: GamePhase                — ENTRANCE or PLAYING
└── displayLine: Int                — floor(y), the editor line the pet stands on
```

### InputState (`World.kt`)

```
InputState
├── moveDirection: Int    — -1 (left), 0 (none), 1 (right)
└── jumpJustPressed: Boolean
```

### Physics constants (`World.kt`)

```
Physics
├── GRAVITY = 75.0              — applied every frame, even when grounded
├── MAX_FALL_SPEED = 30.0       — terminal velocity
├── VELOCITY_EPSILON = 0.1      — below this, velocity is treated as zero
└── FRAME_ADVANCE_INTERVAL = 0.096  — sprite frame advance interval (~96ms)
```

## Character System

### Spatial (`Spatial.kt`)

```kotlin
interface Spatial {
    fun id(): EntityID       // value class wrapper around String
    fun collider(): AABB     // shape descriptor: width=2, height=2
}
```

### Character (`Character.kt`)

```kotlin
interface Character : Spatial {
    fun update(input: InputState, ctx: TickContext, dt: Float): CharacterIntent
}
```

Called once per tick. Receives context (position, velocity, grounded, sprite, phase). Returns intent — never mutates anything.

### TickContext

What the character sees:

```
TickContext
├── transform      — current position
├── velocity       — current velocity
├── isOnGround     — was grounded last frame (from physics)
├── sprite         — current animation state
└── phase          — ENTRANCE or PLAYING
```

### CharacterIntent

What the character wants:

```
CharacterIntent
├── velocity       — desired velocity (physics will resolve collisions)
├── animation      — animation to play (Animation object)
├── direction      — LEFT or RIGHT facing
└── phase          — next phase (allows ENTRANCE → PLAYING transition)
```

### PetAnimated (`PetAnimated.kt`)

Concrete character implementation. Key behavior:

**Movement:**
- Ground: `vx = moveDirection * WALK_SPEED(9.0)`, with exponential friction (`damping=18.0`)
- Air: `vx += moveDirection * WALK_SPEED * AIR_CONTROL(0.6) * dt` — gentle steering
- Jump: `vy = JUMP_VELOCITY(-15.6)` when grounded + jump pressed

**Animation tag resolution:**
```
vy < 0           → "J_2"   (ascending)
vy >= 0, airborne → "J_3"  (descending)
J_3 + grounded   → "Stop"  (landing)
|vx| > epsilon   → "Walk"
else              → "Idle"
```

**Phase transitions:**
- `ENTRANCE`: input suppressed, pet falls from spawn point
- `ENTRANCE` + grounded → `PLAYING`: normal gameplay begins

## Physics Engine

### AABB (`PhysicsBody.kt`)

Shape descriptor only — no position. Width and height in tiles.

```kotlin
data class AABB(val width: Int, val height: Int = 2)
```

Pet occupies 2 columns wide, 2 lines tall. Position comes from `Transform`. The "feet line" (ground line) = `floor(y)`, the "body line" = `floor(y) - 1`.

### PhysicsBody.moveAndSlide

Entry point. Takes transform, velocity, tileMap, visibleRange, dt. Returns `PhysicsResult(transform, velocity, isOnGround)`.

#### Step 1: Adaptive sub-stepping

```
displacement = sqrt(vx² + vy²) * dt
steps = ceil(displacement / 0.5)
```

Large movements are split into smaller sub-steps to prevent tunneling through tiles.

#### Step 2: Per-axis collision (each sub-step)

Each sub-step runs four phases sequentially:

**Phase A — moveAndResolveX**

Horizontal movement with wall collision.

```
newX = x + vx * dt

if already overlapping solid on body line:
    skip wall collision, just clamp x >= 0

if moving right (vx > 0):
    sweep columns from prevRight+1 to newRight
    if any column is solid on body line → stop at column - width

if moving left (vx < 0):
    sweep columns from prevLeft-1 down to newLeft
    if any column is solid on body line → stop at column + 1
```

Wall collision checks the **body line** (one line above feet). This means text on the same line as the pet's feet doesn't block horizontal movement — only text at "chest height" does.

**Phase B — moveAndResolveY**

Vertical movement with gravity, ground detection, ceiling detection.

```
vy += GRAVITY * dt                    // ALWAYS applied
vy = min(vy, MAX_FALL_SPEED)          // cap terminal velocity
newY = y + vy * dt

if falling (vy >= 0):
    sweep from ceil(y - epsilon) to floor(newY)
    for each line in sweep:
        if any solid tile spans catLeft..catRight → land here
        return grounded = true, vy = 0, y = landLine

if rising (vy < 0):
    sweep body lines upward from old to new
    if ceiling found → push down, vy = 0, grounded = false

else:
    return newY, vy, grounded = false
```

Key: **gravity is always applied**. Even when grounded, gravity pushes down, collision pushes back up, resulting in `vy = 0`. No special "skip gravity when grounded" logic. Grounded is purely a collision result.

**Ground detection sweep:**
```
sweepStart = ceil(currentY - 0.01)    // slight epsilon to avoid re-detecting current ground
sweepEnd = floor(newY)

findGroundBelow scans lines top-to-bottom in this range.
First line where ANY column in catLeft..catRight is solid = landing line.
```

**Phase C — resolveNewBodyLine**

When vertical movement changes the body line (the line above feet), check if the new body line has solid tiles overlapping the pet's horizontal extent.

```
newBodyLine = floor(y) - 1
if newBodyLine == prevBodyLine → skip

find solid columns overlapping catLeft..catRight on newBodyLine
if none overlap → skip

compute push distances:
    pushLeft = solidLeft - width      (push pet left of obstacle)
    pushRight = solidRight + 1        (push pet right of obstacle)

pick shorter distance, apply push, zero vx
```

This handles the case where jumping past a line of code would place the pet's body inside text. The pet gets pushed to the nearest clear side.

**Phase D — clampToVisibleArea**

```
clampedY = y.coerceIn(visibleRange)

if y > visibleRange.last → grounded = true   (hit bottom boundary)
if y < visibleRange.first → grounded = false  (hit top boundary)
```

Bottom of visible area acts as floor. Top acts as ceiling without grounding.

### Summary: single sub-step pipeline

```
transform, velocity
    │
    ├── moveAndResolveX(t, v, dt, tileMap) → t', v'
    │
    ├── bodyLineBefore = floor(t'.y) - 1
    │
    ├── moveAndResolveY(t', v', tileMap, dt) → PhysicsResult(t'', v'', grounded)
    │
    ├── resolveNewBodyLine(t'', v'', bodyLineBefore, tileMap) → t''', v'''
    │
    └── clampToVisibleArea(t''', v''', grounded, visibleRange) → final PhysicsResult
```

## Tile Map

### VirtualTileMap (`VirtualTileMap.kt`)

Sparse storage: `HashMap<Int, ByteArray>` — only non-empty lines stored.

Each character in source code maps to one tile:
- Non-whitespace → `SOLID (0x01)`
- Whitespace → `AIR (0x00)`

**API:**
- `isSolid(line, col)` — point query
- `hasGroundAt(line, catLeft, catRight)` — range query (any solid in span?)
- `findGroundBelow(startLine, catLeft, catRight, endLine)` — sweep downward
- `getExtent(line)` — min..max solid column range for a line

### TileMapSyncer (`TileMapSyncer.kt`)

Listens to `DocumentListener` events. On any edit, rebuilds tile map asynchronously using `runReadAction` to safely read document text. The rebuild extracts raw `charsSequence` — **does not account for inlay hints** or other visual overlays.

## Rendering

### GameRenderer (`GameRenderer.kt`)

`JComponent` overlay on the editor. Receives `GameFrame` each tick.

**Coordinate mapping:**
- `logicalToX(line, col)` → `editor.logicalPositionToXY(LogicalPosition(line, col)).x`
- Supports fractional columns via interpolation between integer positions
- Y: `editor.logicalPositionToXY(LogicalPosition(groundLine, 0)).y + lineFrac * lineHeight`

**Sprite rendering:**
- Sprite size = `lineHeight * 2` (two editor lines tall)
- Centered horizontally on hitbox
- Frames cached per animation tag; flipped variants cached separately

**Debug overlay:**
- Green rectangles: tile extents (solid regions per line)
- Red/orange rectangle: character hitbox (red=grounded, orange=airborne)
- Blue rectangle: body line extent
- Yellow stripe: ground line highlight
- FPS counter

### Known issue: inlay hints

`logicalPositionToXY` accounts for inlay hints (parameter name hints, type hints). The tile map does not — it uses raw document columns. This means `logicalToX(lineA, col)` may produce different pixel X than `logicalToX(lineB, col)` if the lines have different inlay hints. Causes visual teleportation when the pet changes lines.

## Game Controller

### GameController (`GameController.kt`)

Project-scoped service. Casey Muratori-style fixed-timestep loop via `javax.swing.Timer` at 16ms.

**Lifecycle:**
1. `enterGameMode()` — spawns pet at caret position with `ENTRANCE` phase, overlays renderer, starts timer
2. `gameTick()` — calculates dt, gathers input, calls `WorldUpdate.tick`, updates renderer
3. `exitGameMode()` — stops timer, removes overlay, disposes resources

**Input handling:**
- `IdeEventQueue.EventDispatcher` intercepts keyboard events
- Arrow keys → moveDirection, Space → jump, Escape → exit
- Events consumed to prevent IDE shortcut interference

**Visible range:**
- Uses document bounds (`0..lastDocumentLine`), not viewport bounds
- Prevents pet from being dragged when user scrolls

## Math Reference

### dt (delta time)

`dt` = time elapsed since the previous tick, in seconds.

```kotlin
val now = System.nanoTime()
val dt = ((now - lastTickNanos) / 1_000_000_000f).coerceAtMost(0.05f)
```

Timer fires every 16ms, so dt is usually ~0.016 seconds. Capped at 0.05s to prevent physics explosions when the app freezes (e.g. debugger pause, GC stall).

**Why multiply by dt:** makes movement frame-rate independent. Without dt, a 60 FPS machine moves the pet twice as fast as a 30 FPS machine. With dt, both move the same distance per real-world second.

```
position = position + velocity * dt

Example at 60 FPS (dt = 0.016):
  x = 0 + 9.0 * 0.016 = 0.144 tiles per frame
  after 60 frames (1 second): x ≈ 8.64 tiles

Example at 30 FPS (dt = 0.033):
  x = 0 + 9.0 * 0.033 = 0.297 tiles per frame
  after 30 frames (1 second): x ≈ 8.91 tiles

Both ≈ 9 tiles/second regardless of frame rate.
```

Every formula in the engine uses dt to scale time-dependent values.

### Velocity and position

Velocity = how many tiles the pet moves per second. Position updates each frame:

```
newX = x + vx * dt
newY = y + vy * dt
```

`vx = 9.0` means 9 tiles per second to the right. `vy = -15.6` means 15.6 tiles per second upward (negative Y = up in screen coordinates).

### Gravity

Gravity is acceleration — it changes velocity over time:

```
vy = vy + GRAVITY * dt
```

Each frame, `vy` increases by `75.0 * 0.016 = 1.2` tiles/sec. Starting from zero:

```
Frame 0: vy = 0.0      → pet stationary
Frame 1: vy = 1.2      → falling slowly
Frame 2: vy = 2.4      → falling faster
Frame 3: vy = 3.6      → accelerating
...
Frame 25: vy = 30.0    → terminal velocity (capped)
```

Then position updates with this velocity: `newY = y + vy * dt`. The pet accelerates downward like a real object.

### Jump

Jump sets `vy = -15.6` (upward). Then gravity pulls it back:

```
Frame 0: vy = -15.6    → moving up fast
Frame 1: vy = -14.4    → still up, slowing
Frame 2: vy = -13.2    → still up, slower
...
Frame 13: vy ≈ 0       → apex of jump
Frame 14: vy = +1.2    → starts falling
Frame 15: vy = +2.4    → falling faster
...
```

No special "jump state" or "apex detection". Gravity alone creates the parabolic arc. The pet leaves the ground because `vy < 0` moves it upward past the ground tile; physics no longer detects ground collision → `isOnGround = false`.

### Exponential friction (ground damping)

When the player releases the movement key on ground, velocity decays exponentially:

```
vx = vx * exp(-GROUND_DAMPING * dt)
vx = vx * exp(-18.0 * 0.016)
vx = vx * 0.75
```

`exp(-18 * 0.016) ≈ 0.75` — each frame, velocity is multiplied by 0.75.

```
Frame 0: vx = 9.0      (just released key)
Frame 1: vx = 6.7
Frame 2: vx = 5.0
Frame 3: vx = 3.8
Frame 4: vx = 2.8
Frame 5: vx = 2.1
Frame 6: vx = 1.6
Frame 7: vx = 1.2
Frame 8: vx = 0.9
Frame 9: vx = 0.7
...approaches zero but never reaches it
```

**Why exponential, not linear?** Linear decay (`vx -= constant * dt`) reaches zero at a fixed time, but can overshoot to negative. Exponential decay approaches zero asymptotically — it's always positive, always decelerating, and feels like natural friction. The deceleration is proportional to current speed: fast → strong braking, slow → gentle braking.

**Why `exp()` instead of just `vx * 0.75`?** The raw multiplier `0.75` depends on frame rate. At 30 FPS, dt = 0.033, so `exp(-18 * 0.033) ≈ 0.55` — different multiplier, same physical behavior. Using `exp(-damping * dt)` makes friction frame-rate independent.

**Velocity epsilon:** exponential decay never truly reaches zero. When `|vx| < 0.1`, it snaps to zero. This stops invisible sub-pixel sliding and lets the animation switch from Walk to Idle.

### Air control

In air, pressing a direction doesn't set velocity directly. It adds a small nudge per frame:

```
vx = vx + moveDirection * WALK_SPEED * AIR_CONTROL * dt
vx = vx + 1 * 9.0 * 0.6 * 0.016
vx = vx + 0.086
```

Compare to ground movement where `vx = 9.0` instantly. In air, it takes many frames to build up speed. This gives the floaty, limited control feeling of mid-air movement.

**Why accumulate instead of set?** On ground, the pet has traction — full control. In air, there's nothing to push against. The small per-frame addition simulates weak air resistance steering. The `AIR_CONTROL = 0.6` factor further reduces the rate.

### Terminal velocity

```
vy = min(vy, MAX_FALL_SPEED)
vy = min(vy, 30.0)
```

Caps downward speed. Without it, a long fall accelerates forever: after 60 frames, `vy = 75 * 60 * 0.016 = 72 tiles/sec`. At that speed the pet could skip past platforms between frames (tunneling). The cap at 30.0 keeps displacement per frame manageable: `30 * 0.016 = 0.48 tiles` — less than one tile, so the sweep-based collision won't miss anything.

### Adaptive sub-stepping

When velocity is high, a single frame could move the pet more than 0.5 tiles. That risks skipping over thin platforms.

```
displacement = sqrt(vx² + vy²) * dt
steps = ceil(displacement / 0.5)
```

If displacement = 1.2 tiles, physics runs 3 sub-steps of `dt/3` each. Each sub-step moves ≤0.5 tiles, so collision detection never misses a tile boundary.

### Sprite frame timing

Animation frames advance on a separate timer, independent of game FPS:

```
frameTimer += dt
if frameTimer >= 0.096:
    frameIndex++
    frameTimer -= 0.096
```

At 60 FPS, a sprite frame changes every ~6 game frames. At 30 FPS, every ~3 game frames. Either way, the animation plays at the same visual speed (~10.4 sprite frames per second).
