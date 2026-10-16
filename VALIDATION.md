# Validación de Chaos Tablist 1.2.0 para Minecraft 1.20.1

16 de octubre de 2026. Fabric Loader 0.19.5 / Fabric API 0.92.6+1.20.1, Forge 47.4.0 y NeoForge legacy 47.1.106.

## Resultados

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| JAR de producción y fuentes | PASS | PASS | PASS |
| Regresiones, límites de paquetes y aislamiento | 85/85 | 85/85 | 85/85 |
| Metadatos, bytecode Java 17, 162 iconos y ocho idiomas | PASS | PASS | PASS |
| Tab, editor, datos privados y guardado por red | PASS | PASS | PASS |
| Diseños, cierre con cambios, diagnósticos y recuperación de conflictos | PASS | PASS | PASS |
| Simulación de paquetes vanilla y guardado confirmado | PASS | PASS | PASS |
| Arranque dedicado --initSettings | No ejecutado | PASS hasta EULA | PASS hasta EULA |

Las pruebas gráficas se ejecutaron con Java 21 sobre mundos planos creados exclusivamente en <cargador>/run/saves/selftest. El compilador emite bytecode Java 17 y usa las APIs de esa versión de Java; no se ejecutó una JVM 17 independiente. Los recursos usan pack_format 15. Se revisaron capturas reales del Tab y del editor.

El protocolo usa buffers acotados y los canales propios de 1.20.1: Fabric Networking y SimpleChannel para Forge y NeoForge. Los límites de arrays se verifican antes de asignar memoria. La prueba cubre longitudes excesivas en C2S/S2C y nombres de iconos, además de revisión, permisos de interfaz, persistencia y conflictos. NeoForge 1.20.1 conserva net.minecraftforge y META-INF/mods.toml; no debe confundirse con la API NeoForge 21.x.

Se adaptaron el dibujo de nueve secciones, los iconos de ping, las skins, el scoreboard, el scroll del ratón, la selección de texto y la captura del idioma del jugador. El mixin del HUD se ajusta al render de 1.20.1. El mundo de pruebas se abre desde el menú inicial después de cargar el mod; no usa Quick Play.

## Reproducción y límites

- Compilación: gradlew -Ploader=fabric build, -Ploader=forge build o -Ploader=neoforge build; en Windows gradlew.bat.
- Gradle 9.7.1, compilador Java 21 con --release 17. Loom necesita JVM 25 para el proceso Gradle; ModDev legacy admite la configuración usada con JVM 25.
- Autoprueba normal: :fabric:runSelftest, :forge:runSelftest o :neoforge:runSelftest. Crea automáticamente su mundo plano si falta.
- Vía vanilla simulada: runSelftestVanilla del mismo cargador.
- Inspección de JAR: python tools/validate-ports.py.

La simulación vanilla no sustituye una sesión con un cliente sin mod independiente. El editor siempre necesita el mod en cliente. El arranque dedicado no acepta la EULA ni comprueba un mundo activo. No se hicieron benchmarks, pruebas con dos clientes independientes, Chaos Ranks instalado ni lanzamiento separado del JAR de producción en un launcher externo. Los errores deliberados de disco/JSON de las regresiones forman parte de las pruebas de recuperación. Los avisos de Realms de las cuentas de desarrollo no afectaron a las pruebas locales.
