# Chaos Tablist 1.2.0 — Minecraft 1.21.4

Tres JAR independientes: Fabric, Forge y NeoForge. Requiere Java 21; Fabric necesita Fabric API para 1.21.4. Instala solo el archivo de tu cargador.

- Adaptación de texturas, cabezas, transparencias e ítems a las APIs de 1.21.4.
- Tab animado, 162 iconos, ocho idiomas y editor con vista previa.
- Datos privados por jugador, guardado confirmado, control de revisiones y recuperación de conflictos.
- Biblioteca de diseños, importación/exportación, Ctrl+S y avisos al cerrar.

Los tres compilan y pasan **82 comprobaciones**, la inspección de sus JAR y las pruebas dentro del juego, incluida la simulación vanilla. Forge y NeoForge también completaron editor, guardado por red, diseños y recuperación de conflictos tras usar javaw.exe con la preferencia gráfica de alto rendimiento de Windows. Se mantuvo desactivada la ventana inicial en el entorno de pruebas. Ambos inicializan el servidor dedicado hasta EULA. Consulta VALIDATION.md y GRAPHICS_RETEST_1.21.4.md.

Actualización de validación: los JAR conservan exactamente sus hashes anteriores; se actualizan los resultados y el estado de la publicación.

Ramas: fabric-1.21.4, forge-1.21.4 y neoforge-1.21.4. Se incluyen fuentes y SHA256SUMS.txt. Servidor y cliente deben usar 1.2.0 para la vista avanzada y el editor.
