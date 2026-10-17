# Chaos Tablist 1.2.0 — Minecraft 26.1

17 de octubre de 2026. Java 25. Fabric Loader 0.19.5 y Fabric API 0.145.1+26.1; Forge 62.0.9; NeoForge 26.1.0.19-beta.

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| Compilación y 82 comprobaciones de regresión | PASS | PASS | PASS |
| Metadatos, bytecode Java 25, idiomas e iconos | PASS | PASS | PASS |
| Tab, nueve pestañas, ventanas de efectos y colores | PASS | PASS | PASS |
| Clic de pestaña, Ctrl+A y escritura Unicode | PASS | PASS | PASS |
| PNG recibido por red, cabeza e ítem en el Tab | PASS | PASS | PASS |
| Guardado confirmado y conflictos de revisión | PASS | PASS | PASS |
| Presets, biblioteca, cierre y diagnósticos | PASS | PASS | PASS |
| Simulación vanilla y guardado confirmado | PASS | PASS | PASS |
| Inicialización dedicada sin aceptar EULA | PASS limitado | PASS limitado | LIMITADO |

Se revisaron capturas reales del Tab y del editor. Las capturas de `screenshots/26.1/` incluyen el PNG normal y otro con alfa 0.4, una cabeza y un diamante. Las pruebas se ejecutaron con el Java gráfico del JDK 25 y la NVIDIA GeForce RTX 3060 Laptop GPU, sin cambiar los ajustes del sistema. Forge y NeoForge también arrancaron con su ventana inicial activada.

La adaptación actualiza la extracción de interfaz, alfa explícito del texto y texturas, FontDescription, entrada de teclado y ratón, registro de paquetes Fabric, eventos Forge, permisos, perfiles, serialización de componentes y reloj del mundo. La fase lunar global se consulta en el punto de aparición. El paquete cubre los formatos de recursos 84.0 y datos 101.1. Forge incluye la marca Mixin JAVA_21 porque su motor 0.8.7 no reconoce JAVA_25; el bytecode y la ejecución siguen siendo Java 25.

Los errores de JSON y escritura que aparecen durante regressionTest son fallos provocados para comprobar que el archivo y la configuración anterior se conservan.

## Alcance y límites

Las pruebas dentro del juego usan cliente y servidor integrado y el transporte real de cada cargador. La simulación vanilla fuerza los paquetes y respaldos vanilla en un cliente con el mod; no equivale a conectar otro cliente vanilla independiente. No se hizo una sesión de dos clientes ni una partida multijugador en servidor dedicado.

El arranque dedicado usa --initSettings, sin aceptar la EULA ni crear un mundo: Fabric genera los ajustes; Forge termina en la comprobación de la EULA. NeoForge genera server.properties y eula.txt, pero su lanzador 11.0.5 termina con código 1 porque no existe un hilo de servidor tras esa salida deliberada. Esa comprobación se registra como limitada, sin ocultar el error ni tratarla como un arranque completo aprobado.

NeoForge 26.1.0.19-beta es la dependencia exacta probada. Las tres variantes son exclusivamente para Minecraft 26.1, no para 26.1.1 o 26.1.2.

## Reproducción

Usar Java 25 y compilar un cargador por ejecución. Los comandos equivalentes de Windows terminan en .bat.

- Fabric: `./gradlew -Ploader=fabric :fabric:build :fabric:runSelftest :fabric:runSelftestVanilla`
- Forge: `./gradlew-forge -Pselftest :forge:build :forge:runClient`; después `./gradlew-forge -PselftestVanilla :forge:runClient`
- NeoForge: `./gradlew-neoforge :neoforge:build :neoforge:runSelftest :neoforge:runSelftestVanilla`
- En el equipo Windows probado añadir `-I tools/windows-gpu.init.gradle` para usar javaw.exe del mismo JDK.
- Verificación del contenido de los tres JAR: `python tools/validate-ports.py`.
