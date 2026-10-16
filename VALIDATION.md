# Validación de Chaos Tablist 1.2.0 — Minecraft 1.21.4

16 de octubre de 2026. Java 21. Fabric Loader 0.19.5 / Fabric API 0.119.4+1.21.4, Forge 54.1.14 y NeoForge 21.4.158.

## Resultados

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| JAR de producción y fuentes | PASS | PASS | PASS |
| Regresiones e aislamiento de cargadores | 82/82 | 82/82 | 82/82 |
| Metadatos, bytecode Java 21, 162 iconos y ocho idiomas | PASS | PASS | PASS |
| Tab, editor, datos privados y guardado por red | PASS | Pendiente | Pendiente |
| Diseños, aviso de cambios, diagnósticos y conflictos | PASS | Pendiente | Pendiente |
| Simulación vanilla y guardado confirmado | PASS | No ejecutada | No ejecutada |
| Arranque dedicado --initSettings | No ejecutado | PASS hasta EULA | PASS hasta EULA |

La adaptación usa las APIs de dibujo de 1.21.4 para texturas y cabezas, alfa explícito y la consulta actualizada al registro de ítems. Un identificador de ítem inexistente muestra la barrera de respaldo. Los recursos usan pack_format 46. Se revisaron capturas reales de Fabric.

## Límite gráfico de Forge y NeoForge

Ambos clientes fallaron con EXCEPTION_ACCESS_VIOLATION dentro de atio6axx.dll+0x190590. Se repitió el arranque omitiendo la ventana inicial del cargador y el mismo fallo volvió al crear la ventana OpenGL de Minecraft. No se certifican el editor, el renderizado ni el guardado dentro del juego en esos dos cargadores para 1.21.4. Se publican como beta. El resultado no demuestra que el problema sea exclusivamente del controlador ni que una prueba aprobada en Fabric valide las otras interfaces.

La opción de omitir la ventana inicial se cambió únicamente en las carpetas locales de desarrollo; no está incluida en los JAR. No se cambiaron controladores ni ajustes gráficos del sistema.

## Reproducción y alcance

Fabric: gradlew -Ploader=fabric build con JVM 25 para Loom. Forge: gradlew-forge build con JAVA_HOME en Java 21 y Gradle 8.12. NeoForge: gradlew -Ploader=neoforge build. Compilador, bytecode y juego: Java 21. En Windows usar los ejecutables .bat.

Autopruebas: :fabric:runSelftest, :forge:runSelftest y :neoforge:runSelftest. Abren o crean automáticamente un mundo plano llamado selftest dentro de <cargador>/run/saves, después de llegar al menú inicial. La simulación vanilla usa runSelftestVanilla. Inspección de los tres JAR: python tools/validate-ports.py.

La simulación vanilla no sustituye una sesión con un cliente sin mod independiente. Las pruebas dedicadas inicializan archivos y se detienen en la EULA; no aceptan sus condiciones ni prueban un mundo activo. No se probaron dos clientes independientes, Chaos Ranks instalado, un launcher externo ni cargas de muchos jugadores. Los errores de JSON y disco de las regresiones son casos negativos deliberados. No se modificaron mundos originales.
