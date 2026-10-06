## Chaos Tablist 1.1.0

- **Effect editor**: every effect in the Effect list has a ✎ button to set its colors, speed and other options, and to choose whether it wraps a text or an icon, with a live preview before inserting it.
- **Color preview**: the color picker has a Preview button that tries the color in the live tab before you press OK. Closing without OK puts the old color back.
- **Edit lines in place**: double-click a header, footer or animation line to edit it right in the list.
- **Tooltips** on every editor tab and button explaining what they do.
- **Select text with the mouse** in every field: drag to select, double-click selects a word or a whole tag; Ctrl+C / X / V to copy, cut and paste.
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
