# Chaos Tablist 1.2.0 — Minecraft 1.20.1

Tres archivos independientes: **Fabric, Forge y NeoForge legacy**. Bytecode Java 17; pruebas en juego con Java 21. Fabric necesita Fabric API para 1.20.1. Instala solo el JAR de tu cargador.

- Port completo de red, skins, scoreboard, interfaz y selección de texto a las APIs de 1.20.1.
- Tab animado, 162 iconos, ocho idiomas y editor con vista previa.
- Datos privados por jugador, guardado confirmado y control de revisiones y conflictos.
- Recuperación de configuración, límites de paquetes/PNG y biblioteca de diseños importables.

Los tres cargadores compilan y pasan **85 comprobaciones**, la inspección de sus JAR y las autopruebas normales y de simulación vanilla dentro del juego, incluido el guardado por red. Forge y NeoForge también inicializan el servidor dedicado hasta la comprobación de EULA. Consulta VALIDATION.md para límites y reproducción.

Ramas: fabric-1.20.1, forge-1.20.1 y neoforge-1.20.1. Se adjuntan los JAR, fuentes y SHA256SUMS.txt. Servidor y cliente deben usar 1.2.0 para el editor y la vista avanzada. NeoForge 1.20.1 conserva la API histórica de Forge.
