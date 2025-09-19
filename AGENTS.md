# AGENTS.md - Pokeball Paper Plugin

This document guides contributors and agents working on this repository. It captures modern Paper best practices, coding
conventions, structure, and project-specific guidance to keep the codebase clean, efficient, and maintainable.

Scope: This file applies to the entire repository.

## Project Snapshot

- Type: Paper plugin
- Language/Runtime: Java 21 (LTS)
- Build: Maven (shade configured; runtime libs provided via Paper `libraries:`)
- Entry: `com.stdnullptr.pokeball.Pokeball`
- Plugin meta: `src/main/resources/paper-plugin.yml` (Paper metadata only)
- API target: Paper 1.21.x (`api-version: 1.21`)

## Goals and Philosophy

- Prefer modern Paper APIs; avoid Bukkit fallbacks where Paper offers improvements.
- Favor clarity and maintainability over cleverness. Small, well-named classes.
- Keep main thread responsive. Offload blocking work. Embrace Paper schedulers.
- Use Kyori Adventure Components for player messaging (no legacy color codes).
- Use PersistentDataContainer (PDC) for item/entity metadata; avoid NMS.
- Structure for testability and feature growth. Keep domain logic decoupled from Bukkit events.

## Build & Run

- Build: `mvn -q -DskipTests package`
- Output: `target/Pokeball-<version>.jar`
- Deploy: drop the jar into your server's `plugins/` folder.
- Dependencies: MiniMessage is provisioned at runtime via `paper-plugin.yml` `libraries:` (no shading/relocation
  needed).

## CI & Release (Automation)

- Default branch: `master`.
- Build CI (`.github/workflows/build.yml`): Java 21 (Temurin), Maven cache, runs on push/PR to `master`, uploads
  `target/Pokeball-*.jar` as an artifact (retained 7 days). Artifact name equals the jar name without the `.jar` suffix.
- Release management (`.github/workflows/release-please.yml` + `.github/release-please-*`):
    - Uses `googleapis/release-please-action@v4` (manifest mode).
    - `release-type: maven` and `extra-files` updates `src/main/resources/paper-plugin.yml` `version` alongside
      `pom.xml`.
    - `component: pokeball`, `include-component-in-tag: false` -> tags like `vX.Y.Z`.
    - `skip-snapshot: true` -> no `-SNAPSHOT` PRs.
- Release assets (`.github/workflows/release-build.yml`): triggers on `release: published`, checks out the tag, builds,
  and attaches `target/Pokeball-*.jar` to the Release.
- Tokens: define repo secret `ADMIN_TOKEN` (PAT with `repo` + `workflow`) so the Release event triggers the asset
  workflow and uploads under a user context.

Usage:

- Merge feature PRs with Conventional Commits (`feat`, `fix`, `BREAKING CHANGE`) to drive semver.
- release-please opens a release PR; merge it to cut a release. The release-assets workflow attaches the jar
  automatically.
- To force a version: `git commit --allow-empty -m "chore: release" -m "Release-As: X.Y.Z" && git push origin master`.

## Paper Essentials (Modern Practices)

1) Plugin Metadata (`paper-plugin.yml`)

- Use `paper-plugin.yml` (not legacy `plugin.yml`) for plugin metadata and capabilities.
- Include: `name`, `version`, `main`, `api-version`, `description`, `authors`, `website`, `dependencies`.
- Define `permissions:` here — still supported — to centralize permission defaults and descriptions.
- Do NOT define `commands` here. Modern Paper forbids YAML-based commands for Paper plugins; use
  `JavaPlugin#registerCommand(...)` or lifecycle events instead. Calling `JavaPlugin#getCommand()` during `onEnable` on
  a Paper plugin will throw `UnsupportedOperationException`.
- Optional fields:
    - `folia-supported: false` — set to true only when validated.
    - `libraries:` — prefer this to let Paper provision runtime libraries (e.g., MiniMessage) instead of shading.

2) Logging

- Use `getSLF4JLogger()` from `JavaPlugin` for structured logs.
- Prefer parameterized logging: `logger.info("Captured {}", entity.getType());`.

3) Messaging (Kyori Adventure)

- Use Components: `sender.sendMessage(Component.text("..."))`.
- Use MiniMessage for rich formatting. Either:
    - Declare in `paper-plugin.yml` `libraries:` (preferred), or
    - Shade + relocate `adventure-text-minimessage` in the final jar.
- No legacy section signs or ChatColor; use Components everywhere.

4) Scheduling & Performance

- Never block the main thread with I/O or heavy computation.
- For sync entity/world operations, stay on the main thread.
- For async work (e.g., file I/O), use Paper's async scheduler and then hop back to main thread to interact with the
  world.
- Consider region/async schedulers for high-scale or Folia-compatible designs. If targeting Folia later, remove any
  assumptions of global main thread.

5) Events

- Keep listeners small. Delegate to services.
- Validate event state early; bail fast.
- Respect cancellation, priorities, and ignoreCancelled where applicable.

6) Commands

- Register programmatically via lifecycle: `LifecycleEvents.COMMANDS` (see `PokeballCommands`). Do not use YAML for
  commands.
- Use Paper Brigadier (`Commands.literal`, `SuggestionProvider<CommandSourceStack>`) for parsing and tab-suggestions.
- Keep executors small and delegate; validate permissions early and message with Adventure/MiniMessage.

Tricky bits:

- `JavaPlugin#getCommand(...)` is not usable in `onEnable()` for Paper plugins — it throws. Use `registerCommand(...)`
  instead (see Paper Javadoc) or hook into `LifecycleEvents.COMMANDS`.
- If migrating from a Bukkit/Spigot plugin with `plugin.yml` commands, remove those entries and replace with
  programmatic registration.

7) Data & Persistence

- Prefer `PersistentDataContainer` on `ItemMeta`/`Entity` to tag plugin data.
- Use `NamespacedKey` constants in a `Keys` class for PDC keys.
- Store small, stable data (ids, enums, tiny JSON). Avoid raw NBT.

8) Dependencies

- For common libs (MiniMessage, Configurate, etc.), prefer Paper `libraries:` to let Paper load them at runtime. If
  shading, relocate packages to avoid conflicts.
- Keep the shaded jar minimal; avoid unnecessary dependencies.

9) Compatibility & API Versioning

- Set `api-version` to the minimum you support (e.g., `1.21`).
- Only use APIs available at that level. Feature-gate newer APIs if needed.

## Repository Conventions (IMPORTANT)

Packages:

- `com.stdnullptr.pokeball` — entrypoint (`Pokeball`) and plugin-wide accessors.
- `com.stdnullptr.pokeball.command` — command wiring (see `PokeballCommands`).
    - `command.builder` — Brigadier tree assembly (`CommandTreeBuilder`).
    - `command.executor` — command handlers (`BasicCommandExecutor`, `GiveCommandExecutor`, `AdminCommandExecutor`).
    - `command.suggestion` — suggestion providers (`PokeballSuggestionProviders`).
- `com.stdnullptr.pokeball.listener` — gameplay listeners (`ProjectileListeners`, `StasisCleanupListener`).
- `com.stdnullptr.pokeball.item` — item factory (`PokeballItemFactory`).
- `com.stdnullptr.pokeball.service` — services (`StasisService`).
- `com.stdnullptr.pokeball.config` — typed config (`ConfigManager`, `ConfigLoader`).
    - `config.sections` — section views (`ItemConfig`, `CaptureConfig`, `EffectsConfig`, `StasisConfig`,
      `MessagesConfig`).
    - `config.models` — effect/flight/refund models.
- `com.stdnullptr.pokeball.util` — shared helpers (`Keys`).

Command surface (permissions):

- `/pokeball` (`/pb`) help; `/pokeball help`, `/pokeball version` — no permission.
- `/pokeball reload` — `pokeball.admin.reload`.
- `/pokeball give <player> [amount]` — `pokeball.admin.give`.
- `/pokeball admin list|tp|clean|cap|refund` — `pokeball.admin`.

Style:

- Java 21.
- No one-letter variable names in production code.
- Prefer constructor injection for services; avoid hard singletons.
- Keep classes focused; avoid god-objects.
- Use `final` where applicable.
- Use Optional only where it clarifies absence; otherwise null checks + `Objects.requireNonNull`.

Error Handling:

- Fail fast on invalid input; log at `warn` for user-caused issues, `error` for plugin faults.
- For recoverable flows, degrade gracefully with clear player feedback.

## Configuration

`src/main/resources/config.yml` (key highlights):

- `item.*` — MiniMessage `name`, `lore`, optional `custom-model-data`.
- `capture.allowed-entity-types` — strict allowlist; anything else blocked.
- `capture.consume-on-release` — if true, filled ball is consumed on release.
- `special-capture.*` — permission (`pokeball.capture.any`), optional lore annotation.
- `compat.worlds` — world allowlist (empty = all).
- `messages.*` — user-facing strings (MiniMessage).
- `stasis.world|x|y|z` — stash location for parked entities; `stasis.cap.max-total` limit (0 = unlimited).
- `effects.flight|capture|release` — visuals; flight trail, capture/release presets.
- `release.offset-normal|offset-up|probe-max-steps` — safe-release placement tuning.
- `refund.mode` — `GIVE` (inventory, drop on overflow) or `DROP` (always drop).

Rules of thumb:

- Keep serialized data minimal; use PDC for item identity and YAML for stasis mapping.

## Plugin Lifecycle

- `onEnable` (see `Pokeball`): save defaults; build `ConfigManager`, `Keys`, `PokeballItemFactory`, `StasisService`.
- Register commands in `LifecycleEvents.COMMANDS` (`PokeballCommands`).
- Register listeners: `ProjectileListeners`, `StasisCleanupListener`.
- Kick off async cleanup of stale stasis entries (`StasisService#cleanupInvalidAsync`).
- `onDisable`: standard shutdown; no blocking I/O.

## Useful Links

- Paper Project Setup: https://docs.papermc.io/paper/dev/project-setup/
- How Plugins Work: https://docs.papermc.io/paper/dev/how-do-plugins-work/
- Getting Started: https://docs.papermc.io/paper/dev/getting-started/paper-plugins/
- Plugin YAML: https://docs.papermc.io/paper/dev/plugin-yml/

Differences vs Bukkit:

- Paper plugins use `paper-plugin.yml`; `plugin.yml` is legacy. Commands are not declared in YAML.
- Prefer `libraries:` in `paper-plugin.yml` for common libs (e.g., MiniMessage) over shading.

## Do / Don't

Do:

- Use Adventure Components and MiniMessage.
- Use PDC for item/entity tagging.
- Keep event handlers lean and delegate to services.
- Align `api-version` with minimum supported server.

Don't:

- Block the main thread or perform heavy I/O synchronously.
- Use NMS unless absolutely necessary (and only with paperweight + Gradle).
- Mix legacy chat color codes with Components.

## File Handling Discipline

- Always inspect file encoding before editing. Use `(Get-Content path -Encoding Byte -TotalCount 3)` to detect a UTF-8
  BOM (EF BB BF).
- When writing files that started without a BOM, call
  `[System.IO.File]::WriteAllText(path, content, (New-Object System.Text.UTF8Encoding($false)))` or
  `Set-Content -Encoding utf8` to keep it that way.
- Never mix `Add-Content` with pre-existing UTF-8 documents; rebuild the full text and overwrite instead so encoding
  stays consistent.
- Avoid non-ASCII punctuation (smart quotes, en/em dashes) unless the file already uses them intentionally. Convert them
  to plain ASCII if they appear.
- After edits, spot-check with `(Get-Content path)[0]` to ensure no mojibake (for example stray `??` or byte triples
  such as `0xE2 0x80 0x93`). If you see that, undo and rewrite using ASCII-safe encoding.

## Agent Notes

- Preserve existing file encoding when editing docs. `README.md` is UTF-8 (no BOM); avoid commands that add a BOM or
  re-encode punctuation.
- Use `[System.IO.File]::WriteAllLines(path, lines, new System.Text.UTF8Encoding($false))` (or equivalent) so smart
  quotes and dashes are not mangled.
- Prefer plain ASCII in new text when feasible to avoid Windows console artifacts.
