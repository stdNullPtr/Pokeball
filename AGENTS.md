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
- Keep the main thread responsive. Offload blocking work. Embrace Paper schedulers.
- Use Kyori Adventure Components for player messaging (no legacy color codes).
- Use PersistentDataContainer (PDC) for item/entity metadata; avoid NMS.
- Structure for testability and feature growth. Keep domain logic decoupled from Bukkit events.

## Build & Run

- Build: `mvn -q -DskipTests package`
- Output: `target/Pokeball-<version>.jar`
- Deploy: drop the jar into your server's `plugins/` folder.

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
