# Chaos Tablist 1.2.0 — Minecraft 1.21.4

Tres JAR independientes: Fabric, Forge y NeoForge. Requiere Java 21; Fabric necesita Fabric API para 1.21.4. Instala solo el archivo de tu cargador.

- Adaptación de texturas, cabezas, transparencias e ítems a las APIs de 1.21.4.
- Tab animado, 162 iconos, ocho idiomas y editor con vista previa.
- Datos privados por jugador, guardado confirmado, control de revisiones y recuperación de conflictos.
- Biblioteca de diseños, importación/exportación, Ctrl+S y avisos al cerrar.

Los tres compilan y pasan 82 comprobaciones y la inspección de sus JAR. Fabric pasa las pruebas dentro del juego y la simulación vanilla. Forge y NeoForge se publican como **beta**: sus interfaces quedan pendientes porque el equipo de prueba falla en la biblioteca gráfica AMD durante la creación de la ventana OpenGL, con y sin ventana inicial del cargador. Ambos completan el arranque dedicado hasta EULA. Consulta VALIDATION.md para los límites.

Ramas: fabric-1.21.4, forge-1.21.4 y neoforge-1.21.4. Se incluyen fuentes y SHA256SUMS.txt. Servidor y cliente deben usar 1.2.0 para la vista avanzada y el editor.
