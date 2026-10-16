# Validación de Chaos Tablist 1.2.0 — Minecraft 1.21.4

16 de octubre de 2026. Java 21. Fabric Loader 0.19.5 / Fabric API 0.119.4+1.21.4, Forge 54.1.14 y NeoForge 21.4.158.

## Resultados

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| JAR de producción y fuentes | PASS | PASS | PASS |
| Regresiones e aislamiento de cargadores | 82/82 | 82/82 | 82/82 |
| Metadatos, bytecode Java 21, 162 iconos y ocho idiomas | PASS | PASS | PASS |
| Tab, editor, datos privados y guardado por red | PASS | PASS | PASS |
| Diseños, aviso de cambios, diagnósticos y conflictos | PASS | PASS | PASS |
| Simulación vanilla y guardado confirmado | PASS | PASS | PASS |
| Arranque dedicado --initSettings | No ejecutado | PASS hasta EULA | PASS hasta EULA |

La adaptación usa las APIs de dibujo de 1.21.4 para texturas y cabezas, alfa explícito y la consulta actualizada al registro de ítems. Un identificador de ítem inexistente muestra la barrera de respaldo. Los recursos usan pack_format 46. Se revisaron capturas reales de Fabric.

## Nueva comprobación gráfica

Forge y NeoForge completaron las autopruebas normales y la simulación vanilla el 16 de octubre de 2026. Se verificaron capturas de ambos editores, guardado por red, diseños, avisos, diagnósticos y conflictos. Los cuatro procesos terminaron con [selftest] PASS y BUILD SUCCESSFUL.

El fallo inicial se reprodujo al usar java.exe. La preferencia existente de alto rendimiento de Windows estaba asignada a javaw.exe; al usar ese ejecutable del mismo JDK 21, las pruebas pasaron. tools/windows-gpu.init.gradle reproduce ese arranque sin modificar los ajustes del sistema. Se mantuvo earlyWindowControl=false en las carpetas locales de pruebas; no se certificó de nuevo la ventana inicial activada. Consulta GRAPHICS_RETEST_1.21.4.md.

## Reproducción y alcance

Fabric: gradlew -Ploader=fabric build con JVM 25 para Loom. Forge: gradlew-forge build con JAVA_HOME en Java 21 y Gradle 8.12. NeoForge: gradlew -Ploader=neoforge build. Compilador, bytecode y juego: Java 21. En Windows usar los ejecutables .bat.

Autopruebas: :fabric:runSelftest, :forge:runSelftest y :neoforge:runSelftest. Abren o crean automáticamente un mundo plano llamado selftest dentro de <cargador>/run/saves, después de llegar al menú inicial. La simulación vanilla usa runSelftestVanilla. Inspección de los tres JAR: python tools/validate-ports.py.

La simulación vanilla no sustituye una sesión con un cliente sin mod independiente. Las pruebas dedicadas inicializan archivos y se detienen en la EULA; no aceptan sus condiciones ni prueban un mundo activo. No se probaron dos clientes independientes, Chaos Ranks instalado, un launcher externo ni cargas de muchos jugadores. Los errores de JSON y disco de las regresiones son casos negativos deliberados. No se modificaron mundos originales.
