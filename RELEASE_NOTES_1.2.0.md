# Chaos Tablist 1.2.0 — Minecraft 1.21.1

Tres JAR independientes: Fabric, Forge y NeoForge. Requiere Java 21; Fabric necesita también Fabric API. Instala solamente el archivo de tu cargador.

- Datos privados enviados solo a su propietario, guardado confirmado y control de revisiones.
- Recuperación de archivos, límites de JSON/PNG y mejoras de rendimiento.
- Ctrl+S, diseños reutilizables, importación/exportación, aviso de cambios y diagnósticos del editor.
- Implementación común y adaptadores nativos por cargador, con 162 iconos y ocho idiomas.

Los tres compilan y pasan 82 comprobaciones y la inspección de sus JAR. Fabric y NeoForge pasaron las autopruebas normales y la simulación vanilla. Forge pasó Tab, editor, guardado por red y conflictos; se distribuye como **beta** porque su simulación vanilla falló al inicializar el servidor integrado. El fallo anterior de AMD no se reprodujo tras reparar el controlador. Consulta VALIDATION.md para límites y reproducción.

Ramas: fabric-1.21.1, forge-1.21.1 y neoforge-1.21.1. Servidor y cliente deben usar 1.2.0 para la vista avanzada y el editor. Los JAR y sus fuentes se publican por separado; SHA256SUMS.txt permite verificar las descargas.
