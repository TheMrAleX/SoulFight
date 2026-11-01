# Trazabilidad de los 36 requisitos duros

Las rutas indicadas están en `engine/src/main/kotlin/com/fightforge/engine` o `app/src/main/java/com/fightforge/game`. Las comprobaciones manuales son criterios de revisión, no resultados aprobados.

| Error | Archivo | Test o checklist | Cobertura |
|---|---|---|---|
| 1 | Models.kt / FightView.kt | EngineTest.absoluteJoystick | automática |
| 2 | Models.kt / Combat.kt | EngineTest.doubleTap; comprobar animación y ganancia al correr | parcial |
| 3 | FightView.kt | Comprobar retratos P1/P2 en Android | manual |
| 4 | AssetEditors.kt / FightView.kt | Importar y restablecer seis controles | manual |
| 5 | EditorViews.kt | Arrastrar asas independientes y comprobar límites | manual |
| 6 | Models.kt / Storage.kt / FightView.kt | EngineTest.geometry; comprobar alineación de PNG | parcial |
| 7 | Models.kt | EngineTest.geometry | automática |
| 8 | Editors.kt | IMPORTAR/FABRICAR/DESCARGAR por ranura | manual |
| 9 | Editors.kt / Combat.kt | CompletionTest: mapeo de todos los estados; revisar transición visual | manual |
| 10 | Editors.kt | Comprobar tres acciones de salida | manual |
| 11 | Models.kt | EngineTest.stats | automática |
| 12 | Combat.kt | Comprobar 60/45/30 y máximo 3000 | manual |
| 13 | Models.kt | DECISIONS: conflicto 1080 frente a 2×504 | decisión |
| 14 | Models.kt | EngineTest.frameEconomySpeed | automática |
| 15 | Models.kt / Combat.kt | CompletionTest: resistencia y desplazamiento real | parcial |
| 16 | Models.kt / Combat.kt | EngineTest.movementMultipliers | parcial |
| 17 | Models.kt | EngineTest.damageButtons | automática |
| 18 | Models.kt / Storage.kt | Comprobar guía, unión y leyenda completa | parcial |
| 19 | Combat.kt / Models.kt | EngineTest.grabClash; CompletionTest: alcance completo de empate | parcial |
| 20 | Models.kt | EngineTest.stats | automática |
| 21 | Editors.kt / EditorViews.kt | Borrar seleccionada y última hitbox | manual |
| 22 | Editors.kt | Añadir, renombrar y borrar animación extra | manual |
| 23 | Models.kt / Editors.kt | EngineTest.budget / rejectNegativeBalance | automática |
| 24 | EditorViews.kt / Combat.kt | CompletionTest: desplazamiento y plantillas; revisar flecha en HitboxView | parcial |
| 25 | Combat.kt | CompletionTest: teletransporte exacto X/Y; revisar alineación de PNG | manual |
| 26 | Models.kt / Editors.kt | Foto opcional no forma parte del coste | manual |
| 27 | Editors.kt | Redimensionar hitbox con saldo suficiente e insuficiente | manual |
| 28 | Editors.kt | Comprobar repetición 400/80/30 ms | manual |
| 29 | Models.kt / Editors.kt | Inicio y recuperación se editan independientemente | manual |
| 30 | Models.kt | EngineTest.budget | automática |
| 31 | Models.kt / Combat.kt | Comprobar pausa de 60 frames y animación gratis | manual |
| 32 | Models.kt / Editors.kt | Comprobar validación por entidad | manual |
| 33 | Models.kt / Editors.kt | Comprobar rechazo de un segundo teletransporte | manual |
| 34 | Models.kt / Combat.kt | EngineTest.recycleProjectiles | automática |
| 35 | Models.kt / Combat.kt | CompletionTest: costes de todas las hitboxes; revisar caída visual | parcial |
| 36 | Models.kt / EditorViews.kt | EngineTest.attachedValidation; ajuste magnético manual | parcial |

## Aceptación Android

El workflow `Android device smoke` ejecuta `scripts/device-smoke.py` sobre un Android 10 acelerado. Comprueba orientación horizontal, entrada al combate, HUD/pausa/lista de movimientos, creación de un personaje con foto y quieto dibujados, guardado y presencia en la biblioteca. Guarda capturas, XML de interfaz y logcat.

`ForgeInstrumentation` comprueba paquetes de personajes y efectos estables, intercambio de escenarios animados, dos pares TCP con 3600 ticks y hashes iguales, segunda conexión sin retransmitir recursos, reparación de caché corrupta, persistencia/recuperación y 50 pasos de deshacer/rehacer con relleno y borrador. Los resultados de una ejecución solo se consideran aprobados cuando el workflow termina con éxito.

Pendiente de validación en hardware: 60 FPS sostenidos en gama media, audio/vibración de cada fabricante y descubrimiento NSD entre dos teléfonos físicos. Las filas manuales anteriores son una lista de revisión; no implican que esas comprobaciones físicas hayan ocurrido.
