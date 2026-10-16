# Validación de Chaos Tablist 1.2.0 — Minecraft 1.21.1

Actualizada el 16 de octubre de 2026. Java 21. Fabric Loader 0.19.5 / Fabric API 0.116.17+1.21.1, Forge 52.1.16 y NeoForge 21.1.257.

## Resultados

| Comprobación | Fabric | Forge | NeoForge |
|---|---|---|---|
| Compilación del JAR y fuentes | PASS | PASS | PASS |
| Regresiones e aislamiento de cargadores | 82/82 | 82/82 | 82/82 |
| Metadatos, Java 21, 162 iconos y ocho idiomas | PASS | PASS | PASS |
| Tab, editor, datos privados y guardado por red | PASS | PASS | PASS |
| Diseños, aviso de cambios, diagnósticos y recuperación de conflictos | PASS | PASS | PASS |
| Simulación de paquetes vanilla y guardado confirmado | PASS | Falló; pendiente | PASS |
| Arranque dedicado --initSettings | No ejecutado | PASS | PASS |

Forge y NeoForge completaron sus autopruebas gráficas después de la reparación del controlador. El bloqueo anterior en AMD atio6axx.dll no se reprodujo. Se revisaron capturas reales del Tab de Forge y del editor de NeoForge.

La prueba adicional runSelftestVanilla de Forge falló dos veces al iniciar el servidor integrado: la configuración no había recibido la carga del mundo y Forge lanzó «Can not retrieve LootModifierManager until resources have loaded once». La autoprueba normal de Forge sí completó el guardado y los conflictos. No se ha determinado la causa del fallo adicional ni se ha certificado la compatibilidad vanilla de Forge; su archivo se publica como beta. El arranque manual desde menú se inició, pero no completó la autoprueba. No confundir un proceso que termina correctamente sin entrar al mundo con una prueba aprobada.

La vía vanilla se simula mediante una opción de desarrollo; no sustituye una sesión con un cliente vanilla independiente. La prueba dedicada solo inicializa archivos: no acepta la EULA ni carga un mundo. No se han realizado pruebas multijugador independientes, benchmarks ni pruebas con Chaos Ranks instalado.

## Estructura y reproducción

common/ contiene lógica, configuración, red, editor y recursos. Cada cargador tiene su adaptador nativo, metadatos y proveedor de plataforma. Los JAR no mezclan APIs de cargadores. NeoForge usa payloads opcionales versionados; Forge usa su canal nativo opcional.

- Fabric: gradlew -Ploader=fabric build (Gradle 9.7.1 y JVM 25 para Loom; compilador/juego Java 21).
- Forge: gradlew-forge build, JAVA_HOME en Java 21 (Gradle 8.12).
- NeoForge: gradlew-neoforge build, JAVA_HOME en Java 21 (Gradle 8.12).
- En Windows, añadir .bat al ejecutable.
- Autopruebas: :fabric:runSelftest, :forge:runSelftest o :neoforge:runSelftest. Requieren una copia del mundo de desarrollo selftest en <cargador>/run/saves/selftest.
- Simulación vanilla: runSelftestVanilla. Forge admite -PmanualWorld para entrar desde el menú; esta ruta queda pendiente de verificación.
- Inspección de los tres artefactos: python tools/validate-ports.py.

Las pruebas modificaron solo copias de mundos de desarrollo. La opción local que desactiva la ventana inicial de Forge no está incluida en los JAR ni en la distribución. Los errores de disco y JSON de las regresiones son casos negativos intencionados.
