# Your tab list, exactly the way you want it.

**Chaos Tablist** lets you customize **everything** in the tab list: animated headers and footers, Chaos Ranks badges on every player, gradients, rainbow and fade effects, pixel-art icons and badges, ping colors, server stats (TPS, RAM, CPU…) and client stats (FPS, RAM, clock). Edit it all **in-game** with `/tablist` from a custom editor with a **live preview**: changes apply to the whole server instantly and are saved per world.

Built for **Fabric 1.21.1**.

![The tab list](PASTE_TAB_URL_HERE)

---

## ✨ Features

### 🔀 Hybrid: works with vanilla clients
- **Server only**: players without the mod still get your custom header, footer, colors, gradients, animations and player formats through the normal vanilla packets. Icons and badges fall back to symbols (`♛`, `★`, `[VIP]`).
- **Mod on the client too**: the tab is drawn by the client every frame, unlocking smooth **60 FPS animations**, **backgrounds behind text**, transparency, **wave / bounce / shake**, outline, glow, player heads, items, **custom PNG images**, a styled panel with an **animated gradient border**, and an **opening animation**.

![What vanilla clients see](PASTE_TAB_VANILLA_URL_HERE)

### 🎨 A real in-game editor
Type `/tablist` and a custom editor opens with tabs on the left and your **live tab list** on the right (with your real players plus example players). Switch the preview to **vanilla view** to see what players without the mod get.

- **Header / Footer**, **Players**, **Ping**, **Design**, **Animations**, **Icons** and a **Help** tab listing every placeholder with its current value.
- One click inserts **colors**, **effects**, **icons** and **placeholders** right at your cursor. Templates are syntax-highlighted.
- Every effect has a **✎ editor** to tweak its colors, speed and options and choose a text or an icon inside it, with a live preview.
- **Preview colors** in the live tab before accepting them, **double-click lines** to edit them in place, and **tooltips** everywhere.
- **Save** applies it to everyone instantly, or turn on **Auto** to apply as you type.

![Editor](PASTE_EDITOR_URL_HERE)

![Effects](PASTE_EFFECTS_URL_HERE)

### 🧪 Text effects
MiniMessage-style tags: `<#FF5555>`, `<gradient:#f00:#00f:speed=1>`, `<rainbow:1>`, `<fade>`, `<pulse>`, `<sparkle>`, `<typewriter>`, `<scroll>`, `<blink>`, `<cycle>`, `<if:{ping}:gt:150>`, `<pc:{ping}>`, `<bar:{ram_pct}:10>`… and client-only `<bg>`, `<outline>`, `<glow>`, `<shadow>`, `<wave>`, `<bounce>`, `<shake>`, `<alpha>`, `<fadein>`, `<head>`, `<item>`, `<img>`. Legacy `&a` and `&#RRGGBB` codes work too.

### 🏷️ Icons & badges
**162 pixel-art icons** (`<icon:crown:#FFD700>` or `:crown:`) and pixel-art **badges** like Chaos Ranks (`<badge:VIP:#7B2CBF:#FFFFFF:star>`), usable anywhere. Drop PNG files in `<world>/chaostablist/icons/` to use your own logo.

![Icons](PASTE_ICONS_URL_HERE)

### 📊 Placeholders
TPS, MSPT, RAM, CPU, uptime, online / max players, ping, world, biome, coordinates, health, food, level, game mode, kills, deaths, playtime, AFK, team, date and time… plus client-side **FPS**, **RAM** and **local time**. Frame-based animations with `{anim:name}`.

### 👥 Ranks & sorting
Each row shows the player's **Chaos Ranks** badge next to their name, and you can give single players their own format. Sort by rank, name, ping, game mode, world or team.

### 🤝 Chaos Ranks integration (optional)
With **Chaos Ranks** installed you get the `{rank}` badge on every row and sorting by rank priority.

---

## 📋 Commands (OP)
| Command | |
|---|---|
| `/tablist` | Open the editor (needs the mod on your client) |
| `/tablist reload` | Reload `tablist.json` and custom icons |
| `/tablist toggle` | Enable / disable the custom tab |
| `/tablist preview <text>` | Preview a template in chat |
| `/tablist player <players> prefix\|format\|suffix <text>` | Per-player format |
| `/tablist player <players> hide\|show\|reset` | Hide, show or reset players |

**Languages:** English and Spanish.
