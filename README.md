# Fight Forge

Juego de lucha 2D para Android nativo, exclusivamente horizontal. Kotlin 2.0.20, Compose, SurfaceView y motor determinista JVM separado. API mínima 29; objetivo 34.

## Construir

Requisitos: JDK 17, Android SDK `platforms;android-34`, `build-tools;34.0.0`.

```sh
./gradlew test
./gradlew assembleDebug
```

El APK está en `app/build/outputs/apk/debug/app-debug.apk`. En este entorno:

```sh
source scripts/environment.sh
./gradlew test assembleDebug assembleRelease --max-workers=4
```

## Jugar y crear

UN JUGADOR permite elegir luchador, escenario y dificultad. Joystick absoluto y botones A/B/C/D admiten multitáctil. Atrás hacia el rival bloquea; doble toque horizontal corre. Comandos de ejemplo: `S → E → A` y ulti `S → E → D`. La lista de movimientos está en pausa. Las rondas son al mejor de tres; práctica permite configurar muñeco y vida infinita.

CREACIÓN permite crear/copiar personajes, repartir 20 puntos, cambiar dimensiones con asas, importar o dibujar frames y descargar plantillas. Para guardar un personaje se requiere nombre, foto y un frame quieto. El editor de movimientos valida comandos, nombres y saldo, dispone de dibujo/edición de hitboxes y parámetros de proyectiles/objetos. Los fondos admiten 1–12 imágenes. Interfaz permite importar controles; música admite reproducción y asignación. La tienda guarda compras y monedas.

LAN utiliza WiFi local, descubrimiento NSD `_fightforge._tcp`, conexión TCP, retraso de tres frames y checksum cada 60 ticks. También admite IP y puerto manuales. Los personajes y sus imágenes se intercambian con verificación de integridad y caché.

## Releases y firma

La firma se conserva fuera del repositorio en `/workspace/fightforge-signing/release.p12`, con contraseña en el archivo privado `store-password` del mismo directorio. Ambos tienen permisos restringidos. Conservar el entorno/su copia de seguridad es necesario para seguir firmando actualizaciones. No borrar datos de la aplicación para actualizar una release firmada con la misma clave.

```sh
scripts/release.sh 0.2.0
```

El script exige un checkout limpio, ejecuta tests, construye y verifica el APK firmado, sube main y crea la release preliminar con checksum, directamente o a través del workflow de publicación de artefactos verificados. `versionCode` usa el número de commits: mantener historial ascendente sin reiniciarlo. Los APK debug utilizan una firma distinta y no actualizan una instalación release.

GitHub Actions construye tests y APK debug en pushes y PR. Para firmar tags requiere secretos `FIGHTFORGE_KEYSTORE_BASE64` y `FIGHTFORGE_STORE_PASSWORD`. La integración actual responde HTTP 403 al guardarlos; la clave local está disponible y la publicación usa un APK ya firmado en una rama `release-artifacts/vX.Y.Z` y el token propio de Actions cuando la subida directa está restringida. La clave privada no se sube.

## Editores y recursos

FABRICAR integra lápiz, borrador, relleno, línea, rectángulo, cuentagotas, paleta de 32 colores y HSV, zoom/paneo, 50 pasos de deshacer/rehacer, guía, onion skin al 30%, tira de hasta 24 frames y reproducción con FPS configurables. Las importaciones PNG validan medidas y ofrecen AJUSTAR/CANCELAR; los lienzos se limitan a 4096 píxeles por lado.

Los efectos admiten 1–12 frames, escala, FPS y sonido, con asignación al golpear/recibir daño. Los escenarios admiten hasta 12 frames y música propia. Se pueden importar, previsualizar y restablecer barras y los seis elementos de controles. La persistencia migra datos del esquema inicial, aísla personajes dañados y recupera sus archivos individuales si el índice global se corrompe.

LAN utiliza el protocolo FFG2: ambos jugadores deben instalar la misma versión. El anfitrión elige escenario; los personajes, imágenes, efectos, fondos y música asociada se transfieren con SHA-256 y caché verificada. Se admiten descubrimiento NSD, IP/puerto y código de sala IPv4. El código representa una dirección local: los dispositivos deben compartir red WiFi y permitir conexiones entre clientes.

## Verificación

`./gradlew test assembleDebug assembleDebugAndroidTest` ejecuta los tests del motor y construye aplicación e instrumentación. GitHub Actions arranca Android 10 con aceleración para probar navegación, combate, pausa, creación y dibujo de personajes. La instrumentación usa dos conexiones TCP reales en el emulador para comparar 3600 ticks, transferir recursos, reutilizar/reparar caché y comprobar persistencia y herramientas de píxeles. Los resultados y capturas quedan como artefactos de Actions.

TRACEABILITY.md relaciona los 36 requisitos duros con código, pruebas y revisiones manuales. El emulador no acredita 60 FPS en teléfonos de gama media ni descubrimiento WiFi entre dos dispositivos físicos; esas comprobaciones necesitan ese hardware. No se ha sustituido ninguna de ellas por un resultado inventado.
