# Chaos Tablist

Mod de Fabric (Minecraft 1.21.1) para personalizar el Tab al máximo: header y footer con varias líneas, rango de cada jugador con Chaos Ranks, ping, degradados, fade, arcoíris, animaciones por letra, fondos detrás del texto, iconos y badges en pixel art (como los de Chaos Ranks), datos del servidor (TPS, RAM, CPU…) y del cliente (FPS, RAM, hora). Todo se edita **en caliente** con `/tablist` desde un menú propio con vista previa en vivo, y queda guardado en el servidor.

Es **híbrido**:
- **Solo en el servidor** ya funciona con clientes vanilla: el servidor monta el Tab y lo manda por los paquetes normales (header, footer y nombre de cada jugador), con las animaciones al ritmo de `updateTicks`.
- **Con el mod también en el cliente** el Tab lo pinta el propio cliente a cada frame: animaciones suaves a los FPS del juego, fondos detrás del texto, transparencia, ola, temblor, contorno, brillo, cabezas, ítems, imágenes PNG propias, panel con borde degradado animado, animación al abrir… y placeholders del cliente como `{fps}` o `{client_ram}`. El editor `/tablist` necesita el mod en el cliente.

Lo que solo se ve con el mod (marcado con ★ en el editor) en vanilla simplemente no se aplica, sin romper el texto. Los iconos y badges salen como un símbolo de respaldo (`♛`, `★`, `[VIP]`…).

## Comandos (requiere OP, nivel 2)

| Comando | Qué hace |
|---|---|
| `/tablist` | Abre el editor (con vista previa en vivo) |
| `/tablist reload` | Vuelve a leer `tablist.json` y los iconos PNG |
| `/tablist toggle` | Activa o desactiva el Tab personalizado |
| `/tablist preview <texto>` | Evalúa una plantilla y la muestra en el chat (como la vería un cliente vanilla) |
| `/tablist player <jugadores> prefix\|format\|suffix <texto>` | Formato propio para esos jugadores |
| `/tablist player <jugadores> hide\|show\|reset` | Ocultar del Tab (con mod), mostrar o quitar sus cambios |

## El editor

Pestañas a la izquierda y el Tab en vivo a la derecha (con tus jugadores reales y jugadores de ejemplo con pings y modos distintos). Debajo de la vista previa: **Vista mod / vanilla** (para ver lo que ven los que no tienen el mod), **▶** (repite la animación de apertura) y cuántos jugadores de ejemplo mostrar.

- **General**: activar, nombre del servidor, refresco para vanilla, orden (rango, nombre, ping, modo, mundo, equipo), espectadores al final.
- **Header / Footer**: una línea por fila. Clic en una línea para editarla en el campo de abajo, o doble clic para editarla ahí mismo.
- **Jugadores**: cambios para un jugador concreto, u ocultarlo.
- **Ping**: barras, número, ambos, solo número u oculto; formato del número, sufijo para vanilla y colores por umbral.
- **Diseño**: fondo en degradado, borde de dos colores animado, esquinas, colores de fila (alterna y la tuya), alto de fila, márgenes, columnas, escala, cabezas, sombra, animación al abrir (caída, fundido, deslizar, zoom), estilo del marcador…
- **Animaciones**: animaciones por frames (`{anim:nombre}`), un frame por línea.
- **Iconos**: los 162 iconos (y los PNG propios); clic para copiar.
- **Ayuda**: todos los placeholders con su valor actual y todas las etiquetas con un ejemplo en vivo.

Los botones **Color / Efecto / Icono / Dato** meten la etiqueta en el cursor del último campo usado. En **Color**, **Vista previa** prueba el color en el Tab en vivo y solo se queda si pulsas **OK**. En **Efecto**, el botón **✎** de cada efecto abre su editor: colores, velocidad y demás opciones, y si va con un texto o con un icono, con vista previa antes de insertarlo. Todas las pestañas y botones tienen un tooltip que explica qué hacen. En los campos se puede seleccionar texto arrastrando el ratón (doble clic selecciona una palabra o una etiqueta entera) y copiar, cortar y pegar con Ctrl+C / Ctrl+X / Ctrl+V. **Ctrl+Z** deshace y **Ctrl+Y** (o Ctrl+Shift+Z) rehace cualquier cambio del editor. Los campos de plantilla colorean las etiquetas en morado y los placeholders en celeste. **Guardar** lo aplica a todo el servidor al momento; con **Auto: Sí** se guarda solo mientras escribes.

## Formato de texto

Estilo MiniMessage. `</>` cierra la última etiqueta; `\<` escribe un `<`.

| Etiqueta | Ejemplo |
|---|---|
| Color | `<#FF5555>`, `<red>`, `<gold>`, `&a`, `&#FF55FF`, `<color:#AARRGGBB>` |
| Estilo | `<b>`, `<i>`, `<u>`, `<st>`, `<obf>`, `<font:minecraft:uniform>`, `<smallcaps>`, `<upper>` |
| Degradado | `<gradient:#f00:#0f0:#00f>` · animado: `<gradient:#B57CF0:#3FD8EA:speed=1>` |
| Arcoíris | `<rainbow>` · animado: `<rainbow:1>` |
| Fundido / pulso | `<fade:#FF5555:#FFFF55:1>`, `<pulse:#FFFFFF:1>` |
| Destellos | `<sparkle:#FFFFFF>` |
| Máquina de escribir | `<typewriter:8:2:cursor=_>` (letras/s, pausa) |
| Marquesina | `<scroll:16:4>` (ancho, velocidad) |
| Parpadeo | `<blink:0.5:0.5>` |
| Alternar textos | `<cycle:2:fade=true>uno<next>dos<next>tres</cycle>` |
| Condición | `<if:{ping}:gt:150>lento<else>rápido</if>` (eq, ne, gt, lt, ge, le, contains, empty) |
| Color por ping | `<pc:{ping}>{ping}ms</pc>` (umbrales de la pestaña Ping) |
| Color por valor | `<scale:{tps}:15:20:#FF5555:#55FF55>{tps}</scale>` |
| Barra | `<bar:{ram_pct}:10:#55FF55:#3A3A3A:char=▌:to=#FF5555>` |
| Icono | `<icon:crown:#FFD700>` o `:crown:` |
| Badge | `<badge:VIP:#7B2CBF:#FFFFFF:star:#3B1A5C:true>` (texto, fondo, letras, icono, marco, negrita) |
| Alinear / espacio | `<left>`, `<center>`, `<right>`, `<space:6>`, `<br>` |
| ★ Fondo | `<bg:#C0000000:2:true:#FFE8B23A>` (color, margen, redondeado, borde) |
| ★ Contorno / brillo / sombra | `<outline:#000>`, `<glow:#FFD700>`, `<shadow:#5A0F8C>`, `<noshadow>` |
| ★ Movimiento | `<wave:1.5:1>`, `<bounce>`, `<shake:0.6>` |
| ★ Transparencia | `<alpha:0.5>`, `<fadein:0.4:0.05>` (aparece al abrir el Tab, letra a letra) |
| ★ Cabeza / ítem / imagen | `<head:{player}>`, `<item:minecraft:diamond_sword>`, `<icon:custom/logo>`, `<img:logo:64:24>` |

## Placeholders

Servidor: `{player}` `{ping}` `{online}` `{max_players}` `{server_name}` `{motd}` `{version}` `{tps}` `{mspt}` `{ram}` `{ram_pct}` `{ram_used}` `{ram_max}` `{cpu}` `{uptime}` `{date}` `{time}` `{day}` `{world_time}` `{weather}` `{weather_icon}` `{is_day}` `{moon_phase}` `{staff_online}` `{afk_online}` `{difficulty}` `{view_distance}` `{sim_distance}` `{entities}` `{chunks}` `{worlds}` `{threads}` `{java_version}` `{server_mods}` `{port}`.

Jugador: `{world}` `{dimension}` `{biome}` `{x}` `{y}` `{z}` `{health}` `{max_health}` `{food}` `{level}` `{gamemode}` `{deaths}` `{kills}` `{mob_kills}` `{playtime}` `{afk}` `{op}` `{team}` `{display_name}` `{xp}` `{xp_pct}` `{armor}` `{saturation}` `{air}` `{light}` `{facing}` `{chunk_x}` `{chunk_z}` `{held_item}` `{language}` `{jumps}` `{fish}` `{walked}` `{since_death}`.

Cliente (★): `{fps}` `{client_ram}` `{client_ram_pct}` `{client_ram_used}` `{client_ram_max}` `{my_ping}` `{local_time}` `{local_time_s}` `{local_date}` `{local_weekday}` `{render_distance}` `{resolution}` `{gpu}` `{client_mods}`. Sin el mod salen como el texto de "Texto sin mod" (`-`), salvo la hora y el ping, que usan los del servidor.

Animaciones: `{anim:nombre}`.

## Formato de cada fila

Cada fila es `prefijo + nombre + sufijo`. Por defecto: el badge del rango de Chaos Ranks (si lo tiene) y el nombre en blanco. No se edita desde el menú: los rangos los gestiona Chaos Ranks. Se puede cambiar a mano en `tablist.json` (`row`) y, para un jugador concreto, en la pestaña **Jugadores**.

## Chaos Ranks (opcional)

Si Chaos Ranks está instalado se activan `{rank}` (el badge del rango, en la versión compacta del Tab), `{rank_name}`, `{rank_id}`, `{rank_priority}`, `{has_rank}` y el orden por rango. Los clientes vanilla ven el rango como `[NOMBRE]`. Se enlaza por reflexión, así que no hace falta para compilar ni para jugar.

## Datos

En `<mundo>/chaostablist/`:
- `tablist.json`: toda la configuración (se puede editar a mano y recargar con `/tablist reload`).
- `icons/*.png`: imágenes propias (máx. 64, 256 KB cada una). Se mandan a los clientes con el mod y se usan con `<icon:custom/nombre>` o `<img:nombre:ancho:alto>`.

## Desarrollo

- `python tools/gen_font.py`: regenera la fuente de iconos y badges (`chaostablist:badge` y `badge_compact`) y `badge_meta.json` a partir del ASCII art del script, con el símbolo de respaldo de cada icono.
- `python tools/gen_gui.py`: texturas del editor.
- `python tools/gen_lang.py`: archivos de idioma (inglés y todas las variantes `es_*`).
- `python tools/gen_icon.py`: icono del mod.
- `./gradlew build`: jar en `build/libs/`.
- `./gradlew runSelftest`: capturas del Tab, de cada pestaña del editor y de los desplegables en `run/screenshots/selftest_*.png`, y prueba el guardado por red. Necesita el mundo `run/saves/selftest`.
- `./gradlew runSelftestVanilla`: lo mismo, pero el servidor trata al cliente como vanilla (para ver lo que reciben los clientes sin el mod).
- `powershell -File tools/upload-curseforge.ps1 -ProjectId <id> [-DryRun]`: sube el jar a CurseForge (token en `CURSEFORGE_TOKEN`).
