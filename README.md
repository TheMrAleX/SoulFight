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
scripts/release.sh 0.1.1
```

El script exige un checkout limpio, ejecuta tests, construye y verifica el APK firmado, sube main y crea la release preliminar con checksum, directamente o a través del workflow de publicación de artefactos verificados. `versionCode` usa el número de commits: mantener historial ascendente sin reiniciarlo. Los APK debug utilizan una firma distinta y no actualizan una instalación release.

GitHub Actions construye tests y APK debug en pushes y PR. Para firmar tags requiere secretos `FIGHTFORGE_KEYSTORE_BASE64` y `FIGHTFORGE_STORE_PASSWORD`. La integración actual responde HTTP 403 al guardarlos; la clave local está disponible y la publicación usa un APK ya firmado en una rama `release-artifacts/vX.Y.Z` y el token propio de Actions cuando la subida directa está restringida. La clave privada no se sube.

## Alcance de esta versión preliminar

Esta versión todavía no cumple toda la especificación comercial. El pixel editor tiene lápiz, borrador, relleno, línea, rectángulo, cuentagotas, historial, paleta HSV y zoom/paneo; faltan onion skin y una tira de animación con reproducción integrada. No están completos los efectos personalizados con sonido/escala/asignación, las migraciones entre esquemas futuros, el intercambio LAN de escenarios, y la configuración avanzada de sonidos en efectos. La importación de sprites se escala al renderizar y todavía no ofrece el diálogo de ajuste de dimensiones. Faltan el pulido de transiciones, previews Compose de los tres tamaños y parte de la presentación avanzada del HUD.

Las pruebas de motor y compilación no equivalen a una certificación de rendimiento en teléfonos ni a una validación LAN con dos dispositivos físicos. TRACEABILITY.md distingue cobertura automatizada y comprobaciones manuales.

El código de `main` incluye una corrección posterior para hashes de paquetes LAN estables. Junto con correcciones posteriores de carrera, contraste y HUD, se validó localmente con 24 pruebas; todavía no está incluida en el APK publicado v0.1.0.
