# Pokeball - A simple mob‑catcher plugin for Paper servers

Pokeball lets you catch a mob in a ball and throw it back out, just like in Pokémon. It's designed to be
straightforward, reliable, and easy to configure - great for survival or adventure servers where players need to move
mobs without leads or boats.

Built against Paper API 1.21.8 (Minecraft 1.21.x)

## What It Does

- Catch mobs by throwing a Pokeball at them.
    - If the mob is allowed (see [`config.yml`](src/main/resources/config.yml) allowlist), it's captured.
    - If not allowed (or it's a player), the ball is refunded with a message.
- Release mobs by throwing a filled Pokeball.
    - The mob appears where the ball lands.
    - Wall hits are adjusted so the mob spawns just outside the wall.
- Admins give balls to players; players just use them - no extra permissions needed for normal use.

## See It In Action

https://github.com/user-attachments/assets/ff69137f-0903-4fe2-8e1a-fa4adb28f12a

## Screenshots

### Empty ball
<img width="1135" height="265" alt="image" src="https://github.com/user-attachments/assets/7042c09f-de90-428c-b089-59ef9d925a43" />

### Caught a Sheep
<img width="1002" height="195" alt="image" src="https://github.com/user-attachments/assets/32876d16-da30-4cb3-8ff8-e0e14e294836" />

### When you capture a creature with the 'special' permissions, bypassing the allowlist
<img width="999" height="208" alt="image" src="https://github.com/user-attachments/assets/88fb2648-f089-446d-9ae7-928283200d3d" />

## Requirements

- Paper 1.21.x
- Java 21 (LTS)
- Folia: not currently supported (`folia-supported: false` in `paper-plugin.yml`:9)

## Installation

- Download the latest [release JAR](https://github.com/stdNullPtr/Pokeball/releases) and drop it into your server's
  `plugins/` folder.
- Start the server once to generate `plugins/Pokeball/config.yml`.
- Edit `config.yml` to your liking, then run `/pokeball reload` in‑game or from console.

## Player Guide

- Ask an admin for Pokeballs.
- Throw an empty ball at an allowed mob to capture it.
- Throw a filled ball where you want the mob to appear.
- If capture is not allowed, you get the ball back with a message.

## Admin Guide

- Help: `/pokeball` or `/pokeball help` (alias `/pb`).
- Version: `/pokeball version`.
- Give balls: `/pokeball give <player> [amount]`.
- Reload config: `/pokeball reload`.
- Storage management: `/pokeball admin list|tp|clean|cap`.
- Refund mode: `/pokeball admin refund [GIVE|DROP]`.
- Capture allowlist: `/pokeball admin capture list|allow <entity>|remove <entity>`.

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

## Configuration

- Allowed mobs are configured by name in [`config.yml`](src/main/resources/config.yml) (strict allowlist: anything not
  listed is blocked).
- Capture/release effects and flight trail are configurable:
    - Enable/disable specific effects.
    - Tweak sizes, counts, intensity.
    - Turn the white streak (trail) off if you prefer a cleaner view.
- Release positioning is smart:
    - Hitting a wall spawns the mob just outside it (not inside).
    - You can tune how far from the wall and how many blocks to probe.

### Ball Refund Behavior

- Config key: `refund.mode: GIVE|DROP`
    - `GIVE`: return balls to player inventory; if full, drop at the impact location.
    - `DROP`: always drop balls at the impact location.
- Live control: `/pokeball admin refund [mode]`.

## Permissions

- `pokeball.admin` - access admin subcommands (`list`, `tp`, `clean`, `cap`, `refund`, `capture …`).
- `pokeball.admin.give` - allow `/pokeball give`.
- `pokeball.admin.reload` - allow `/pokeball reload`.
- `pokeball.capture.any` - bypass capture allowlist (default op).
- Players do not need extra permissions to use balls; they just need the item.

## Compatibility

- Paper 1.21.x (api-version `1.21`).
- Folia: not currently supported (`folia-supported: false`).
- No NMS; uses modern Paper APIs and Adventure Components.

## How It Works

### TL;DR

- Throw empty ball to capture: if the hit mob is allowed and the world is allowed, the entity is parked in stasis and
  you receive a filled ball linked to it; otherwise you get an empty ball back.
- Throw filled ball to release: the ball's ID rides the projectile; on impact the plugin finds a safe spot just outside
  walls and teleports the stored mob there, then unfreezes it a tick later; optionally refunds an empty ball.
- Stasis = real, hidden mob: captured entities are frozen, invisible, and moved to a stash location; a small record
  `{ballId -> world, entityUUID, chunkX/Z, type}` is saved and cleaned up on startup/death.
- Projectiles are consumed on launch; capture projectiles never deal damage. A configurable flight trail can be shown.
- Config drives allowlist, worlds (capture and release), effects, refund mode, and whether release consumes the ball;
  admin commands manage storage cap, capture allowlist, and refund mode.

<details>
  <summary>How it works (Detailed)</summary>

- Item identity and state
    - Every Pokeball is an `ItemStack` with a unique `ball_id` stored in its PDC, making it non‑stackable (see
      `src/main/java/com/stdnullptr/pokeball/item/PokeballItemFactory.java`).
    - Empty vs filled is tracked by the presence of `captured_type` in the item's PDC. No raw NBT is stored on the item.
    - Display is driven by MiniMessage: name/lore from config; lore shows Contents: Empty or the captured type (see
      `src/main/resources/config.yml`).

- Throw detection (projectiles)
    - On `ProjectileLaunchEvent`, if the player is holding a Pokeball (see
      `src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java`):
        - If filled, the projectile is tagged with the ball's `ball_id` (release throw).
        - If empty, the projectile is tagged as a Pokeball capture throw.
      - The item is consumed immediately on launch (prevents dupes across gamemodes, e.g., creative).
      - A small flight trail (glow, dust, end‑rod streak) is rendered if enabled (see
        `src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java`).

- Impact handling
    - On `ProjectileHitEvent` (see `src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java`):
        - Release projectile: compute a safe spawn position at impact and call stasis `release(ballId, at)` (see
          `src/main/java/com/stdnullptr/pokeball/service/StasisService.java`).
        - Capture projectile hitting an entity: validate rules and call stasis `park(entity, newBallId)`; a filled ball
          linked to that entity is given back (see
          `src/main/java/com/stdnullptr/pokeball/config/sections/CaptureConfig.java`).
        - Block hit or miss (capture): an empty ball is refunded per the configured refund mode (see
          `src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java`).
        - Pokeball capture projectiles never deal damage; damage is cancelled for marked projectiles.

- Capture flow (high level)
    - World gate: if the world is not allowed, fail and refund (see `src/main/resources/config.yml` `compat.worlds`).
    - Type gate: strict allowlist from config; players cannot be captured. A special permission can bypass the
      allowlist (optionally annotated on the ball's lore) (see
      `src/main/java/com/stdnullptr/pokeball/config/sections/CaptureConfig.java`).
    - On success: create a new ball (with new `ball_id`), put the target in stasis with that id, mark the ball as filled
      with the captured type, play capture effects, and return the filled ball (see
      `src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java` and
      `src/main/java/com/stdnullptr/pokeball/service/StasisService.java`).

- Release flow (high level)
    - The filled ball's `ball_id` travels on the projectile. On impact, we select a spawn point outside walls with
      passable head/feet space.
    - Stasis `release` teleports the stored entity to the target, plays effects, then refunds an empty ball unless
      `consume-on-release` is true (see `src/main/java/com/stdnullptr/pokeball/service/StasisService.java` and
      `src/main/resources/config.yml`).

- Release placement
    - When a wall is hit, the code probes outward along the impacted face for a few blocks to find “feet and head
      passable” space (see `resolveImpactSpawn` in
      `src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java`).
    - A configurable offset is applied outward from the face and slightly up to avoid clipping (see
      `src/main/resources/config.yml` `release.*`).
    - Wider mobs (spiders) get an extra lateral clearance check; otherwise universal rules apply (see `isSafeSpawn` in
      `src/main/java/com/stdnullptr/pokeball/listener/ProjectileListeners.java`).
    - If probing fails, it falls back to “just outside the block”; entity/air hits use the projectile location.

- Stasis (what it is and why)
    - Stasis parks the real entity safely away from players immediately on capture, instead of serializing complex data
      into the item (see `src/main/java/com/stdnullptr/pokeball/service/StasisService.java`).
    - On capture (`park`):
        - Capacity check (configurable max; 0 = unlimited). If full, capture is refused and the ball is refunded (see
          `src/main/resources/config.yml` `stasis.cap.*`).
        - For living entities, the plugin freezes and hides them: AI off, collidable off, invisible, invulnerable,
          silent, gravity off, not removed when far.
        - The entity is teleported to a configured “stash” location (world/x/y/z; default high Y), so it won't collide
          or be seen (see `src/main/java/com/stdnullptr/pokeball/config/sections/StasisConfig.java`).
        - A lightweight mapping is written to `plugins/Pokeball/stasis.yml`:
          `{ball_id -> world, entity_uuid, chunkX, chunkZ, type}`.
    - On release (`release`):
        - Ensures the source chunk is loaded, finds the entity by UUID, teleports it while invisible and with zero
          velocity/fall distance, plays effects, then restores normal state a tick later.
        - The stasis entry is removed from `stasis.yml`.
    - Housekeeping:
        - On startup, an async cleaner walks `stasis.yml` in small batches per tick, removing entries whose world/entity
          is missing (see `cleanupInvalidAsync` in `src/main/java/com/stdnullptr/pokeball/service/StasisService.java`).
        - If a stasis entity somehow dies, a listener removes the stale entry (see
          `src/main/java/com/stdnullptr/pokeball/listener/StasisCleanupListener.java`).

- Refunds and permissions
    - Refunds after misses/failures and post‑release are handled by GIVE (to inventory, drop on overflow) or DROP (
      always on ground) (see `src/main/resources/config.yml` `refund.mode` and
      `src/main/java/com/stdnullptr/pokeball/config/sections/EffectsConfig.java`).
    - A special capture permission can bypass the allowlist; admins can live‑tune storage cap, capture allowlist, and
      refund mode via `/pokeball admin`.

- Notes for command setup
    - Commands are registered using Paper's Brigadier API with typed arguments and tab‑completion (no YAML commands).
    - Player arguments use native selectors and client‑side suggestions; integers/booleans are validated client‑side.

</details>

## Build From Source

- Build: `mvn -q -DskipTests package`
- Output: `target/Pokeball-<version>.jar`

## License

This project is licensed under the AGPL‑3.0. See `LICENSE` for details.

## Support / Issues

Found a bug or have a feature request? Open an issue on
the [project's GitHub repository](https://github.com/stdNullPtr/Pokeball/issues).
