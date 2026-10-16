# Nueva comprobación gráfica de Minecraft 1.21.4

16 de octubre de 2026. Forge 54.1.14 y NeoForge 21.4.158, Chaos Tablist 1.2.0.

| Prueba | Forge | NeoForge |
|---|---|---|
| Tab, editor y guardado por red | PASS | PASS |
| Diseños, aviso de cambios y diagnósticos | PASS | PASS |
| Conflicto de revisiones y recuperación | PASS | PASS |
| Simulación vanilla y guardado confirmado | PASS | PASS |

Los cuatro procesos terminaron con BUILD SUCCESSFUL y el marcador explícito [selftest] PASS. Se revisaron capturas reales de ambos editores. Estos resultados actualizan las pruebas gráficas que figuraban pendientes en VALIDATION.md y en la publicación anterior.

La preferencia existente de alto rendimiento de Windows estaba asignada a javaw.exe, mientras Gradle iniciaba java.exe. Los arranques con java.exe siguieron fallando en atio6axx.dll+0x190590. Se usó javaw.exe del mismo JDK 21, manteniendo los argumentos del cargador, y las pruebas pasaron. No se modificaron las preferencias gráficas del sistema.

La configuración local usada conserva earlyWindowControl=false en ambos cargadores. No se volvió a probar la ventana inicial activada. Las pruebas se realizaron sobre sus mundos de desarrollo aislados. La simulación vanilla no sustituye una sesión con un cliente sin mod independiente.

## Reproducción en Windows

Forge: `gradlew-forge.bat -I tools/windows-gpu.init.gradle :forge:runSelftest`.

NeoForge: `gradlew.bat -Ploader=neoforge -I tools/windows-gpu.init.gradle :neoforge:runSelftest`.

Para la segunda prueba, usar `runSelftestVanilla`. El script aplica el lanzador Java 21 javaw.exe; usa las preferencias de gráfica ya existentes para ese ejecutable.

Evidencias: forge-javaw-retest.log, neoforge-javaw-retest.log, forge-javaw-vanilla.log y neoforge-javaw-vanilla.log. Capturas en <cargador>/run/screenshots. Los JAR publicados conservan sus hashes SHA256 originales, verificados con tools/validate-ports.py. La publicación se actualiza con estos resultados; los binarios no necesitan recompilarse.
