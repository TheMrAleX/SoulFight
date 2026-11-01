Fight Forge 0.3.0 — combate aéreo, práctica y herramientas de creación.

- Corregido el personaje suspendido al atacar en el aire: gravedad y desplazamiento continúan durante el ataque; también sigue cayendo al recibir daño. La pausa de 60 frames de una ulti sigue siendo intencional.
- Recuperación de ataques con estado aéreo correcto y espera de aterrizaje antes de levantarse.
- Reinicio completo de combate sin conservar historial, efectos o estado de la partida anterior. Reiniciar posiciones en práctica conserva la vida y el poder, y cancela la acción y la velocidad.
- Práctica con hitboxes y entradas activadas inicialmente, opciones independientes y controles que reflejan sus cambios.
- Selección en cuatro columnas uniformes, retrato grande y estadísticas compactas. Tienda en cuadrícula con retratos y compras persistentes.
- Editor con indicadores de sección, iconos vectoriales, costes con decimales, paleta circular, flechas de movimiento y dos formas de borrar una hitbox: papelera y chip sobre la selección.
- Plantillas y previsualización corregidas para movimientos y teletransportes del rival.
- Sonidos personalizados al golpear y recibir daño; carga simultánea de efectos y fin de la repetición del último golpe al terminar la ronda.
- Reinicio de SurfaceView sin destruir la superficie activa, con restauración de IA e interpolación; prueba de contenido del framebuffer después de reiniciar.

LAN usa FFG3: ambos dispositivos deben actualizarse a esta versión. La firma sigue siendo la misma de v0.1.0 y v0.2.0; se puede instalar encima sin borrar los datos.

Los resultados y límites de validación están en VALIDATION.md y TRACEABILITY.md. No se han certificado FPS en teléfonos de gama media ni descubrimiento WiFi entre dos teléfonos físicos.
