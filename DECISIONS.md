# Decisiones

- Android nativo Kotlin/Compose, orientación sensorLandscape, API 29–34. Motor JVM sin dependencias Android, punto fijo 1/16 y tick de 60 Hz.
- El repositorio remoto estaba vacío. Se conserva la especificación original en SPECIFICATION.md y se construye sobre el checkout existente, sin worktrees.
- La especificación pide simultáneamente carrera a 1080 u/s y exactamente dos veces caminar (504 u/s). Se prioriza el requisito duro 2×: 1008 u/s antes del multiplicador de velocidad.
- Los costes se representan en milipuntos; se compara el saldo completo, aunque el chip muestra puntos enteros.
- La cámara reduce la escala hasta 0,55 para mantener visibles luchadores separados por el escenario. Prioriza visibilidad frente al intervalo de zoom contradictorio para un escenario de 3200 u.
- Los objetos aplican su efecto cada 30 ticks de contacto. Los sólidos bloquean el paso y pueden destruirse; los translúcidos no. Ambos caducan tras 240 ticks.
- El paquete LAN limita a 8 MiB comprimidos/32 MiB descomprimidos, comprueba SHA-256, rechaza rutas que salgan de su directorio y guarda caché por hash.
- Las plantillas se exportan mediante MediaStore, sin permisos de almacenamiento. Los medios importados se copian a almacenamiento privado de la aplicación.
- La identidad de release es RSA 3072 / PKCS12, alias fightforge. Se conserva en /workspace/fightforge-signing; nunca en Git. No se debe regenerar para actualizaciones. El applicationId permanece com.fightforge.game y versionCode debe aumentar.
- GitHub permite acceso al repositorio, pero rechaza la administración de secretos Actions con HTTP 403. Por ello la release local usa la clave persistente. El workflow siempre construye debug; firma y publica tags únicamente cuando los secretos de firma estén disponibles.
- La primera distribución es preliminar: no representa la aceptación completa de todos los requisitos comerciales. Las diferencias y verificaciones pendientes están documentadas en README y TRACEABILITY.
