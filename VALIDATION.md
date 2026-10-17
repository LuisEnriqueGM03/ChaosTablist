# Chaos Tablist 1.2.0 — Minecraft 26.2

17 de octubre de 2026. Java 25. Fabric Loader 0.19.5 y Fabric API 0.161.0+26.2; Forge 65.1.3; NeoForge 26.2.0.90.

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| Compilación y 85 regresiones | PASS | PASS | PASS |
| Metadatos, bytecode Java 25, idiomas e iconos | PASS | PASS | PASS |
| Tab, nueve pestañas y ventanas de efectos y colores | PASS | PASS | PASS |
| Clic de pestaña, Ctrl+A y escritura Unicode | PASS | PASS | PASS |
| PNG recibido por red, cabeza e ítem en el Tab | PASS | PASS | PASS |
| Guardado confirmado y conflictos de revisión | PASS | PASS | PASS |
| Presets, biblioteca, cierre y diagnósticos | PASS | PASS | PASS |
| Simulación vanilla y guardado confirmado | PASS | PASS | PASS |

Las tres nuevas regresiones comprueban los valores RGB de los dieciséis colores de Minecraft, el código antiguo &6 con &r y los alias grey/dark_grey. Las 82 anteriores cubren persistencia, privacidad, límites, compresión, historial, guardado y diseños.

Se revisaron capturas reales de cada cargador en screenshots/26.2/. El Tab incluye un PNG recibido por red con opacidad normal y 0.4, una cabeza y un diamante. El editor muestra texto, controles y vista previa correctamente. Las capturas de la simulación vanilla llevan un prefijo distinto para conservar la evidencia de ambas rutas.

La adaptación usa Minecraft.gui para las pantallas y Minecraft.gui.hud para el Tab. El mixin se aplica a Hud.extractTabList. La captura usa GameRenderer.mainRenderTarget, la consulta de GPU usa DeviceInfo y los colores antiguos se resuelven con TextColor. NeoForge usa iconFile para evitar el aviso de logoFile obsoleto. El paquete declara recursos 88.0 y datos 107.1.

En Windows se usó javaw.exe del mismo JDK 25 mediante tools/windows-gpu.init.gradle y la NVIDIA GeForce RTX 3060 Laptop GPU. No se cambiaron los ajustes del sistema. Los errores de JSON y escritura durante regressionTest son fallos provocados para comprobar que la configuración anterior se conserva. Los avisos del registro de contadores de Windows y de Realms no impidieron completar estas pruebas.

## Alcance y límites

Las pruebas dentro del juego usan cliente y servidor integrado con el transporte real de cada cargador. La simulación vanilla fuerza los paquetes y respaldos vanilla en un cliente con el mod; no equivale a conectar otro cliente vanilla independiente. No se hizo una sesión con dos clientes ni una partida en servidor dedicado para 26.2. No se probó la integración opcional con Chaos Ranks cargado.

Forge incluye la marca Mixin JAVA_21 para su motor 0.8.7; el bytecode y la ejecución siguen siendo Java 25. Las variantes admiten Minecraft 26.2 y requieren el cargador correspondiente.

## Reproducción

Usar Java 25 y compilar un cargador por ejecución. En Windows usar los ejecutables .bat.

- Fabric: `./gradlew -Ploader=fabric :fabric:build :fabric:runSelftest :fabric:runSelftestVanilla`
- Forge: `./gradlew-forge -Pselftest :forge:build :forge:runClient`; después `./gradlew-forge -PselftestVanilla :forge:runClient`
- NeoForge: `./gradlew-neoforge :neoforge:build :neoforge:runSelftest :neoforge:runSelftestVanilla`
- En el equipo Windows probado añadir `-I tools/windows-gpu.init.gradle` para usar javaw.exe.
- Contenido de los tres JAR: `python tools/validate-ports.py`.
