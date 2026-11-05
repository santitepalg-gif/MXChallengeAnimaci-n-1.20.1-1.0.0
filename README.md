# MXCHALLENDE Dificultad (mod Forge 1.20.1)

Conversión del datapack MXCHALLENDE + su pack de texturas a un mod.

## Compilar
1. Copia `gradlew`, `gradlew.bat` y la carpeta `gradle/` de tu proyecto Forge 1.20.1 (o del MDK 1.20.1-47.3.0) a esta carpeta.
2. `./gradlew build`  ->  `build/libs/mxchallende-1.0.0.jar`
3. Mete el .jar en `mods/` del servidor Y de cada cliente.

## Probar la animación sin morir
`/mxchallende preview`   (requiere OP)

## Configuración
- `config/mxchallende-common.toml`: applyGamerules, spectatorOnDeath, animateOnDeath
- `config/mxchallende-client.toml`: epicSounds, cameraShake
