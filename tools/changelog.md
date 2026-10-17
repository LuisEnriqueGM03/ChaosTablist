## Minecraft 26.2 — Chaos Tablist 1.2.0

- Fabric, Forge and NeoForge ports for Minecraft 26.2, requiring Java 25.
- Adapted screen management and Tab HUD access to the 26.2 Gui/Hud split.
- Updated GPU device info, score number formatting and legacy color handling.
- NeoForge icon metadata now uses iconFile, avoiding the deprecated logoFile warning.
- Added regressions for all sixteen named colors, legacy gold/reset and gray aliases.
- Adapted payload registration, Forge events, permissions, profiles, world clocks and component serialization.
- Resource format 88.0 and data format 107.1 are covered by the bundled pack metadata.
- Preserves the editor, animated Tab, presets, diagnostics, acknowledged network saves, revision conflicts and private placeholders.
- Build includes 85 regression checks per loader. See VALIDATION.md for in-game checks and their scope.
- Install the matching loader JAR. Fabric requires Fabric API. NeoForge 26.2.0.90 is the loader used for validation.
