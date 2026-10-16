## Minecraft 1.21.4 — Chaos Tablist 1.2.0

- Adapted GUI textures, player heads, alpha rendering and item registry lookup to Minecraft 1.21.4.
- All three loaders compile, pass 82 regression checks and production JAR validation.
- Fabric passes in-game editor, network save, conflict recovery and vanilla packet simulation.
- Forge and NeoForge are **beta**: in-game checks remain pending because the test PC crashes in AMD atio6axx.dll during OpenGL window creation, even without the loader's early window. Dedicated initialization reaches the EULA check. See VALIDATION.md.

## Earlier Minecraft 1.21.1 validation (2026-10-16)

- Added a native NeoForge 21.1 port for Minecraft 1.21.1, including optional typed payloads, lifecycle events and client integration.
- Forge and NeoForge completed the in-game editor, private data, network save, presets, diagnostics and conflict recovery tests after the graphics driver repair.
- Strengthened loader isolation checks to inspect all three loaders.
- Forge is beta: the additional vanilla simulation failed during integrated-server initialization. Fabric and NeoForge passed both in-game modes. See VALIDATION.md.

## Fabric / Forge builds for Minecraft 1.21.1 (1.2.0)

- Shared implementation with separate native Fabric and Forge entrypoints, lifecycle events and optional versioned networking.
- Forge does not depend on Fabric API. Both loaders use the same configuration, private placeholders, editor, animations and design library.
- Separate production JARs and source JARs, Java 21 bytecode, loader-specific regression checks and game selftests.
- Resource generation and upload preparation adapted to the multi-loader layout; no publication is performed by the build.

## Chaos Tablist 1.2.0

- Private player placeholders are delivered only to their owner. Administrators may explicitly opt individual fields into public synchronization.
- Durable saves with server acknowledgements, revision conflict detection, permission feedback and timeouts. Drafts remain available on failure.
- Temporary-file configuration writes with a previous-file backup; malformed reloads preserve the source file and the live configuration.
- Ctrl+S, unsaved-changes confirmation and explicit reload of the server version, with undo recovery.
- Local JSON design library, draft exports and Chaos, Minimal and Ocean presets. Independent animation duplication and 64 editable frames.
- Safer JSON, PNG and compressed payload handling, finite numeric values, parser depth and glyph budgets. Negative scrolling speeds no longer throw.
- Shared vanilla placeholder fallbacks, vanilla-like ordering/limits and an explicit approximate-preview label.
- Cached player entries, cached compressed configuration and entity counts only when requested. Periodic updates stop while disabled or empty.
- Extracted editor persistence, history and design-library components. Java 21 toolchain and automated headless regression checks wired into build.
- All editor messages available in English and Spanish variants.

Server and client should both use 1.2.0 for the custom view/editor. Older clients fall back to vanilla packets.

## Chaos Tablist 1.1.0

- **Effect editor**: every effect in the Effect list has a ✎ button to set its colors, speed and other options, and to choose whether it wraps a text or an icon, with a live preview before inserting it.
- **Color preview**: the color picker has a Preview button that tries the color in the live tab before you press OK. Closing without OK puts the old color back.
- **Edit lines in place**: double-click a header, footer or animation line to edit it right in the list.
- **Tooltips** on every editor tab and button explaining what they do.
- **Select text with the mouse** in every field: drag to select, double-click selects a word or a whole tag; Ctrl+C / X / V to copy, cut and paste.
- **Undo / redo** in the editor: Ctrl+Z undoes, Ctrl+Y (or Ctrl+Shift+Z) redoes any change, including Discard and Defaults.
- Default texts are now in English (`discord.gg/example` as the sample link).
- **35 new placeholders**: OPs and AFK online, difficulty, view/simulation distance, entities, chunks, moon phase, day/night, Java version, mods; per player XP, XP %, armor, saturation, air, light level, facing, chunk, held item, language, jumps, fish caught, distance walked, time since death; client weekday, render distance, resolution, graphics card and mod count.
- Removed groups: each row now shows the Chaos Ranks badge next to the name; per-player formats still work.
- Chaos Ranks placeholders only show up in the editor when Chaos Ranks is installed.
- Picking a color while text is selected now colors only that text instead of replacing it.

## Chaos Tablist 1.0.0

First release.

- Full tab list customization: multi-line header and footer, Chaos Ranks rank badge on every row, per-player overrides (prefix, name, suffix).
- MiniMessage-style text: hex colors, gradients (static and moving), rainbow, fade, pulse, sparkles, typewriter, marquee, blink, cycling texts, conditions, progress bars, color by ping or by value.
- Pixel-art icons (162) and Chaos Ranks-style badges usable anywhere in the tab.
- Placeholders: TPS, MSPT, RAM, CPU, uptime, online, ping, world, biome, coordinates, health, kills, deaths, playtime and more; client-side FPS, RAM and local time.
- Hybrid: works server-side with vanilla clients; with the mod on the client you get smooth 60 FPS animations, text backgrounds, transparency, wave/bounce/shake, outline, glow, player heads, items, custom PNG images, a styled panel with an animated gradient border and an opening animation.
- In-game editor (`/tablist`) with live preview (mod and vanilla views), hot-applied to the whole server and saved per world.
- Optional Chaos Ranks integration: `{rank}` badge and sorting by rank priority.
- Commands: `/tablist reload`, `/tablist toggle`, `/tablist preview`, `/tablist player`.
- English and Spanish translations.
