# Pokeball - A Simple, Mob Catcher for MC Paper servers

Pokeball lets you "catch" a mob in a ball and "throw" it back out, just like in Pokémon games.  
It's designed to be straightforward, reliable, fun - and simple to configure.

Great for survival or adventure servers where you want players to transport mobs safely without leads or boats.

Developed and tested for version 1.21.8

## What It Does

- Catch mobs by throwing a Pokeball at them
    - If the mob is allowed (see [`config.yml`](src/main/resources/config.yml) allowlist), it's captured
    - If not allowed (or it's a player), the ball pops back with a friendly message
- Release mobs by throwing a filled Pokeball
  - The mob appears right where the ball lands
  - If you throw at a wall, the plugin places the mob just outside the wall so it doesn't get stuck
- Admins give balls to players; players just use them
    - Normal players don't need special permissions - they only need balls

## How You Use It (Players)

- Ask an admin to give you Pokeballs
- Throw an empty ball at an animal to capture it
- Throw a filled ball where you want the mob to appear
- If the ball can't capture what you hit, you'll get it back with a message

## How You Use It (Admins)

- Get help: `/pokeball help` (shows only what you can use)
- Tip: `/pokeball` with no arguments also shows help; short alias: `/pb`.
- Show version: `/pokeball version`
- Give balls to players: `/pokeball give <player> [amount]`
- Reload config: `/pokeball reload`
- Manage storage:
  - `/pokeball admin list` - see stored entries (first 20)
  - `/pokeball admin tp <id>` - teleport to a stored entry location (for inspection)
  - `/pokeball admin clean <id|all>` - remove one/all stored entries
  - `/pokeball admin cap [maxTotal]` - view current capacity or set new maximum capacity
  - `/pokeball admin refund [mode]` - view current refund mode or set new mode (GIVE|DROP)
- Tune capture allow-list live:
    - `/pokeball admin capture list` - show the allowed entity types
    - `/pokeball admin capture allow <entity>` - add an entity type to the allow-list
    - `/pokeball admin capture remove <entity>` - remove an entity type (keeps at least one entry)

Notes:

- Commands are registered using Paper's Brigadier API with typed arguments and tab-completion.
- Player arguments use native selectors and client-side suggestions; integers/booleans are validated client-side.

## Commands

| Command                                   | Description                           | Permission              | Notes                                               |
|-------------------------------------------|---------------------------------------|-------------------------|-----------------------------------------------------|
| `/pokeball`                               | Show contextual help                  | -                       | Alias: `/pb`                                        |
| `/pokeball help`                          | Show help                             | -                       |                                                     |
| `/pokeball version`                       | Show plugin version                   | -                       | Reads plugin meta                                   |
| `/pokeball reload`                        | Reload configuration                  | `pokeball.admin.reload` |                                                     |
| `/pokeball give <player> [amount]`        | Give Pokeballs                        | `pokeball.admin.give`   | Player selector; amount 1-64; tab-suggested amounts |
| `/pokeball admin list`                    | List stasis entries (first 20)        | `pokeball.admin`        | Shows id prefix + type                              |
| `/pokeball admin tp <id>`                 | Teleport to stasis location           | `pokeball.admin`        | Players only; tab-suggests IDs                      |
| `/pokeball admin clean <id\|all>`         | Remove one or all entries             | `pokeball.admin`        | Tab-suggests IDs and `all`                          |
| `/pokeball admin cap [maxTotal]`          | Show/set storage cap                  | `pokeball.admin`        | 0 = unlimited; tab-suggests common limits           |
| `/pokeball admin refund [mode]`           | Show/set refund mode                  | `pokeball.admin`        | Modes: `GIVE`, `DROP`; tab-suggested                |
| `/pokeball admin capture list`            | List allowed capture types            | `pokeball.admin`        | Tab-completion shows current values                 |
| `/pokeball admin capture allow <entity>`  | Add a mob type to the allow-list      | `pokeball.admin`        | Suggests missing types; updates config              |
| `/pokeball admin capture remove <entity>` | Remove a mob type from the allow-list | `pokeball.admin`        | Suggests allowed types; prevents removing all       |

## How it Works

### TL;DR

- Throw empty ball to capture: if the hit mob is allowed and world‑allowed, the entity is parked in stasis, and you
  receive a filled ball linked to it; otherwise you get an empty ball back.
- Throw filled ball to release: the ball's ID rides the projectile; on impact the plugin finds a safe spot just outside
  walls and teleports the stored mob there, then unfreezes it a tick later; optionally refunds an empty ball.
- Stasis = real, hidden mob: captured entities are frozen, invisible, and moved to a stash location; a small record
  `{ballId -> world, entityUUID, chunkX/Z, type}` is saved and cleaned up on startup/death.
- Projectiles are consumed on launch, never deal damage, and can show a configurable flight trail.
- Config drives allowlist, worlds, effects, refund mode, and whether release consumes the ball; admin commands manage
  storage cap, capture allow-list, and refund mode.

### Detailed

- Item identity and state
    - Every Pokeball is an `ItemStack` with a unique `ball_id` stored in its PDC, making it non‑stackable (see [
      `PokeballItemFactory.java`](src/main/java/com/stdnullptr/pokeball/item/PokeballItemFactory.java) and [
      `Keys.java`](src/main/java/com/stdnullptr/pokeball/util/Keys.java)).
    - Empty vs filled is tracked by the presence of `captured_type` in the item's PDC. No raw NBT is stored on the
      item (see [`PokeballItemFactory.java`](src/main/java/com/stdnullptr/pokeball/item/PokeballItemFactory.java)).
    - Display is driven by MiniMessage: name/lore from config; lore shows Contents: Empty or the captured type (see [
      `config.yml`](src/main/resources/config.yml)).

- Throw detection (projectiles)
    - On `ProjectileLaunchEvent`, if the player is holding a Pokeball (see [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java)):
        - If filled, the projectile is tagged with the ball's `ball_id` (release throw).
        - If empty, the projectile is tagged as a Pokeball capture throw.
        - The item is consumed immediately on launch (prevents dupes across gamemodes, for example 'creative' having
          infinite items).
        - A small flight trail (glow, dust, end‑rod streak) is rendered on a short repeating task if enabled (see [
          `EffectsConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java)).

- Impact handling
    - On `ProjectileHitEvent` (see [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java)):
        - If it's a release projectile, we compute a safe spawn position at the impact and call stasis
          `release(ballId, at)` (see [
          `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
        - If it's a capture projectile and it hit an entity, we validate rules and call stasis
          `park(entity, newBallId)`; a filled ball linked to that entity is given back (see [
          `CaptureConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/CaptureConfig.java)).
        - If it hit a block (capture) or missed, an empty ball is refunded per the configured refund mode (see [
          `EffectsConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java)).
        - Pokeball projectiles never damage what they hit; damage is cancelled for marked projectiles.

- Capture flow (high level)
    - World gate: if the world is not allowed, fail and refund (see [`config.yml`](src/main/resources/config.yml)
      `compat.worlds`).
    - Type gate: strict allowlist from config; players cannot be captured. A special permission can bypass the
      allowlist (optionally annotated on the ball's lore) (see [
      `CaptureConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/CaptureConfig.java)).
    - On success: create a new ball (with new `ball_id`), put the target in stasis with that id, mark the ball as filled
      with the captured type, play capture effects, and return the filled ball (see [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java) and [
      `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).

- Release flow (high level)
    - The filled ball's `ball_id` travels on the projectile. On impact, we select a spawn point outside walls
      and has passable head/feet space (see placement below in [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java)).
    - Stasis `release` teleports the stored entity to the target, plays effects, then refunds an empty ball unless
      `consume-on-release` is true (see [
      `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java) and [
      `config.yml`](src/main/resources/config.yml)).

- Release placement
    - When a wall is hit, the code probes outward along the impacted face for a few blocks to find "feet and head
      passable" space (see `resolveImpactSpawn` in [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java)).
    - A small configurable offset is applied outward from the face and slightly up to avoid clipping (see [
      `config.yml`](src/main/resources/config.yml) `release.*`).
    - Wider mobs (spiders) get an extra lateral clearance check; otherwise the same universal rules apply (see
      `isSafeSpawn` in [
      `ProjectileListeners.java`](src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java)).
    - If probing fails, it falls back to "just outside the block" placement; entity/air hits use the projectile
      location.

- Stasis (what it is and why)
    - Stasis parks the real entity safely away from players immediately on capture, instead of serializing complex data
      into the item (see [`StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
    - On capture (`park`):
        - Capacity check (configurable max; 0 = unlimited). If full, capture is refused and the ball is refunded (see [
          `config.yml`](src/main/resources/config.yml) `stasis.cap.*`).
        - For living entities, the plugin freezes and hides them: AI off, collidable off, invisible, invulnerable,
          silent, gravity off, not removed when far (see `park` in [
          `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
        - The entity is teleported to a configured "stash" location (world/x/y/z; default high Y), so it won't collide
          or be seen (see [
          `StasisConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/StasisConfig.java)).
        - A lightweight mapping is written to `plugins/Pokeball/stasis.yml`:
          `{ball_id -> world, entity_uuid, chunkX, chunkZ, type}` (see persistence in [
          `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
    - On release (`release`):
        - Loads the recorded chunk, finds the entity by UUID, teleports it to the impact spawn while still invisible and
          with zero velocity/fall distance (see `release` in [
          `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
        - One tick later, restores AI, collision, visibility, vulnerability, sound, and gravity; velocity is reset again
          to avoid drift.
        - The stasis entry is removed from `stasis.yml`.
    - Housekeeping:
        - On startup, an async cleaner walks `stasis.yml` in small batches per tick, removing entries whose world/entity
          is missing (see `cleanupInvalidAsync` in [
          `StasisService.java`](src/main/java/com/stdnullptr/pokeball/service/StasisService.java)).
        - If a stasis entity somehow dies, a listener removes the stale entry (see [
          `StasisCleanupListener.java`](src/main/java/com/stdnullptr/pokeball/listener/StasisCleanupListener.java)).

- Refunds and permissions
    - Refunds after misses/failures and post‑release are handled by GIVE (to inventory, drop on overflow) or DROP (
      always on ground) (see [`config.yml`](src/main/resources/config.yml) `refund.mode` and [
      `EffectsConfig.java`](src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java)).
  - A special capture permission can bypass the allowlist; admins can live‑tune storage cap, capture allow-list, and
    refund mode via
      `/pokeball admin` (see [
      `AdminCommandExecutor.java`](src/main/java/com/stdnullptr/pokeball/command/executor/AdminCommandExecutor.java)).

## What It Looks Like

- Throwing a ball shows a small, configurable trail
- Capturing and releasing pop tasteful particles and sounds
- Effects are configurable - you can make them subtle or flashy

## Sensible Defaults, Easy Controls

- Allowed mobs are configured by name in [`config.yml`](src/main/resources/config.yml) (strict allowlist: anything not
  listed is blocked)
- Capture/release effects and flight trail are configurable:
  - Turn each effect on/off
  - Change sizes, counts, and intensity
  - Turn the white streak off if you prefer a cleaner view
- Release positioning is smart:
  - When you hit a wall, the mob appears just outside it (not inside)
  - You can tweak how far from the wall and how many blocks to probe

### Refund Behavior

- Config key: `refund.mode: GIVE|DROP`
  - `GIVE`: tries to return balls to the player inventory; if full, drops at the impact location.
  - `DROP`: always drops balls at the impact location.
- Live control (ops): `/pokeball admin refund [mode]` - view current mode or set new mode (GIVE|DROP)
