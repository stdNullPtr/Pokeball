# Pokeball — A Simple, Modern Mob Catcher for Paper

Pokeball lets you “catch” a mob in a ball and “throw” it back out, just like in monster‑catching games. It’s designed to be simple, reliable, and fun — and to feel great for players without needing technical knowledge.

## What It Does

- Catch mobs by throwing a Pokeball at them
  - If the mob is allowed, it’s captured
  - If not allowed (or it’s a player), the ball pops back with a friendly message
- Release mobs by throwing a filled Pokeball
  - The mob appears right where the ball lands
  - If you throw at a wall, the plugin places the mob just outside the wall so it doesn’t get stuck
- Admins give balls to players; players just use them
  - Normal players don’t need special permissions — they only need balls

## Why It’s Reliable

- No fragile tricks, no weird side effects
- The plugin doesn’t try to “rebuild” mobs
- Instead, it freezes the real mob and safely moves it away during capture (so it can’t be bumped or attacked)
- When you throw the ball to release, the original mob is brought back with its gear and personality intact

## How You Use It (Players)

- Ask an admin to give you Pokeballs
- Throw an empty ball at an animal to capture it
- Throw a filled ball where you want the mob to appear
- If the ball can’t capture what you hit, you’ll get it back with a message

## How You Use It (Admins)

- Get help: `/pokeball help` (shows only what you can use)
- Give balls to players: `/pokeball give <player> [amount]`
- Reload config: `/pokeball reload`
- Manage storage:
  - `/pokeball admin list` — see stored entries (first 20)
  - `/pokeball admin tp <id>` — teleport to a stored entry location (for inspection)
  - `/pokeball admin clean <id|all>` — remove one/all stored entries
  - `/pokeball admin cap get|set <enabled> <max>` — view or change storage cap
  - `/pokeball admin refund get|set <GIVE|DROP>` — set refund behavior (see below)

Notes:
- Commands are registered using Paper’s Brigadier API with typed arguments and tab-completion.
- Player arguments use native selectors and client-side suggestions; integers/booleans are validated client-side.

## What It Looks Like

- Throwing a ball shows a small, configurable trail
- Capturing and releasing pop tasteful particles and sounds
- Effects are configurable — you can make them subtle or flashy

## Sensible Defaults, Easy Controls

- Allowed mobs are configured by name (strict allowlist: anything not listed is blocked)
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
- Live control (ops): `/pokeball admin refund get|set <GIVE|DROP>`
