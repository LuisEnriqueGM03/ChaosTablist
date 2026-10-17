## Minecraft 26.3 — Chaos Tablist 1.2.0

- Fabric, Forge and NeoForge ports for Minecraft 26.3, requiring Java 25.
- Migrated editor input from GLFW key codes to Minecraft's SDL-based input constants and translated shortcut keys.
- Updated left-click handling and text dragging for the new mouse-button identifiers.
- Adapted the design-folder action to Blaze3D.openPath.
- In-game checks cover tab clicks, drag selection, Ctrl+A/Z/Y, Unicode input, Escape, Enter and acknowledged saving through Ctrl+S.
- Resource format 97.1 and data format 121.0 are covered by the bundled pack metadata.
- Preserves animated Tab, PNG icons, editor presets, diagnostics, revision conflicts and private placeholders.
- Build includes 85 regression checks per loader. See VALIDATION.md for scope and results.
- Fabric requires Fabric API. Tested loader versions: Fabric Loader 0.19.5 with Fabric API 0.162.0+26.3, Forge 66.0.10 and NeoForge 26.3.0.76.
