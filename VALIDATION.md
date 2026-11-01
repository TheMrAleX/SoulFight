# Validación de Fight Forge 0.3.0

- Aplicación verificada: `185bfe8f9e711043e6820ea1fb9ee4f2389a4f50`.
- Motor JVM: 47 pruebas, 0 fallos, 0 errores y 0 omitidas. `AirCombatTest` contiene 9 regresiones de ataques aéreos, daño, caída, recuperación, pausa de ulti y reinicio; `MovementTemplateTest` comprueba tres casos de plantillas y destinos.
- `test assembleDebug assembleDebugAndroidTest lintDebug` y APK release firmado: aprobados. Los targets `:app` sin tests no se cuentan como pruebas aprobadas.
- [Build Android 41359356207](https://github.com/TheMrAleX/SoulFight/actions/runs/41359356207): aprobado.
- [Aceptación Android 41359356155](https://github.com/TheMrAleX/SoulFight/actions/runs/41359356155): recorrido real y 12 comprobaciones nativas aprobados sobre Android 10 acelerado.
- Interfaz: confirmación/cancelación de compra, selección y combate, pausa/lista de movimientos, reinicio y contenido real del framebuffer, reanudación, creación de personaje con foto y quieto dibujados, guardado/biblioteca, práctica y casillas activadas/desactivadas. Sin excepción fatal del paquete durante el recorrido.
- Instrumentación: compra persistente sin doble cargo, importación PNG con ajuste centrado, paquetes de personajes/efectos, escenario animado, TCP con 3600 ticks intercalando saltos y ataques aéreos, igualdad de checksum, caché reutilizada/reparada, 50 pasos de deshacer/rehacer, relleno/borrador, recuperación/borrado persistente y chip de hitbox eliminado mediante un toque localizado en sus píxeles reales.
- Resultados: `checks=12`, `passed=true`, `INSTRUMENTATION_CODE: -1`. Evidencias en el artefacto `device-evidence` y copia local `/tmp/ff-030-passed-device`; se revisaron las capturas de tienda, selección, práctica y combate reiniciado.
- El fallo reportado del ataque aéreo se reprodujo primero con tres pruebas fallidas: arco de salto, control horizontal y gravedad durante daño. Las mismas pruebas pasan después de separar física y estados de ataque/daño.
- La prueba de reinicio encontró una superficie vacía al reemplazar el nodo nativo. Se corrigió conservando la superficie y reiniciando simulación, IA e interpolación dentro de ella. Se mantuvo y aprobó la aserción de contenido del framebuffer.

Firma conservada: applicationId `com.fightforge.game`, RSA 3072, esquema APK v2 y certificado SHA-256 `a8c231eb36ba8c1682da32929d95e16180a4fa62482ef05ba8f4a4feac2073de`. La clave permanece fuera del repositorio en `/workspace/fightforge-signing`.

LAN FFG3 requiere actualizar ambos dispositivos. La prueba usa dos peers TCP en un mismo emulador; no acredita descubrimiento NSD entre dos teléfonos físicos. Tampoco se han medido 60 FPS sostenidos en teléfonos de gama media ni audio/vibración en distintos fabricantes. TRACEABILITY.md mantiene esas comprobaciones manuales diferenciadas.

# Historial: validación de Fight Forge 0.2.0

## Resultados comprobados

- `test assembleDebug assembleDebugAndroidTest`: compilación aprobada. Motor JVM: 35 pruebas, 0 fallos, 0 errores y 0 omitidas. Los targets unitarios de `:app` sin pruebas no se cuentan como resultados aprobados.
- `lintDebug` y `assembleRelease`: aprobados. Lint mantiene advertencias de optimización, accesibilidad de vistas nativas y APIs desaconsejadas; no hay errores de Lint.
- GitHub Actions [Android 41348864304](https://github.com/TheMrAleX/SoulFight/actions/runs/41348864304): tests y build aprobados en un runner independiente.
- [Android device smoke 41348864322](https://github.com/TheMrAleX/SoulFight/actions/runs/41348864322): recorrido funcional y las 10 comprobaciones de instrumentación aprobados sobre Android 10 acelerado.
- Recorrido real de interfaz: menú → selección → preparación → combate → pausa → lista de movimientos → continuar → creación de personaje → dibujo de foto y quieto → guardado → personaje visible en la biblioteca. Orientación horizontal y ausencia de excepción fatal del paquete durante ese recorrido.
- Instrumentación: persistencia, paquetes de personaje/efectos estables, escenario de dos frames, dos conexiones TCP reales con 3600 ticks y hashes iguales, reutilización de caché sin retransmisión, reparación de caché corrupta, PNG ajustado con transparencia y centrado, 50 pasos de deshacer/rehacer, relleno, borrador, recuperación de datos corruptos y borrado persistente.
- Resultados nativos: `checks=10`, `passed=true`, `INSTRUMENTATION_CODE: -1`. Capturas, XML, registros y resultados están en el artefacto `device-evidence` del workflow. Copia local: `/tmp/fightforge-020-passed-device`.

## Firma y actualizaciones

El APK usa `com.fightforge.game`, firma v2 RSA 3072 y el certificado SHA-256 `a8c231eb36ba8c1682da32929d95e16180a4fa62482ef05ba8f4a4feac2073de`, idéntico a v0.1.0. El script de release aumenta versionCode mediante el historial de commits y verifica el APK antes de publicarlo.

La clave privada permanece exclusivamente en `/workspace/fightforge-signing`, fuera del repositorio, con permisos 700 para el directorio y 600 para sus archivos. El certificado público está en `signing/release-certificate.pem`.

La instalación superpuesta con esta identidad ya se comprobó en Android mediante `adb install -r`, desde v0.1.0 (código 7) al candidato 0.1.1 (código 19), conservando el UID. El resultado histórico está en `/workspace/fightforge-builds/UPDATE_CHECK.txt`. Esa comprobación histórica no se presenta como una instalación de 0.2.0.

## Límites de la evidencia

No se han medido 60 FPS sostenidos en teléfonos de gama media ni probado el descubrimiento NSD entre dos teléfonos físicos. El test TCP usa dos peers en un mismo emulador. Audio y vibración en fabricantes distintos y las filas manuales de TRACEABILITY.md requieren revisión en ese hardware.

Los fallos encontrados durante el desarrollo se corrigieron y se repitieron sus comprobaciones: recreación de superficie tras pausa, restauración de pantalla completa después de diálogos, acciones de animación fuera de pantalla y arranque/formato de salida del runner de instrumentación. No se eliminaron sus aserciones.
