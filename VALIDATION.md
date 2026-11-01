# Validación de la primera entrega

- `./gradlew test assembleDebug assembleRelease --max-workers=4`: compilación y tests aprobados.
- Motor JVM: 21 tests ejecutados, 0 errores, 0 fallos, 0 omitidos. Incluye determinismo durante 3600 ticks, dos peers lockstep, retraso de entradas, detección de desincronización, estadísticas, economía, comandos, joystick, doble toque, agarres, reciclado y geometría.
- Los targets unitarios de `:app` no contienen pruebas; sus resultados NO-SOURCE no se cuentan como tests aprobados.
- APK release verificado con Android apksigner, esquema v2, RSA 3072. SHA-256 del certificado: `a8c231eb36ba8c1682da32929d95e16180a4fa62482ef05ba8f4a4feac2073de`.
- JDK 17, SDK 34 y Gradle 8.9 descargados de fuentes oficiales con comprobaciones de integridad. Gradle wrapper fija SHA-256 de distribución.
- El entorno no dispone de /dev/kvm. Se inició Android Emulator con emulación de CPU; todavía no constituye una validación de la aplicación en ejecución. La primera instalación no pudo completarse antes de que Android iniciara PackageManager.
- Sin medición de 60 FPS en dispositivo físico y sin prueba de dos teléfonos WiFi. Las comprobaciones visuales de TRACEABILITY siguen pendientes.

- GitHub Actions ejecutó correctamente tests y compilación en un runner independiente. El job de publicación verificó SHA-256 y el certificado y adjuntó `FightForge.apk` y `SHA256SUMS` a la release pública preliminar `v0.1.0`. Se precreó el tag exacto mediante Git para evitar el límite de creación de tags del token de Actions.

## Corrección posterior conservada en el código

`main` incorpora un ZIP determinista para estabilizar los hashes de caché LAN: 23 pruebas JVM aprobadas, 0 fallos/errores/omitidas, y `test assembleDebug` aprobado. Esta corrección no está incluida en el APK de v0.1.0 y no se ha publicado una segunda release.

Android 10 completó el arranque e instaló el APK. El recorrido local no terminó: UIAutomator superó 30 segundos y la captura de la máquina emulada presentó corrupción gráfica. No se cuenta como prueba funcional aprobada ni como defecto demostrado del juego.
