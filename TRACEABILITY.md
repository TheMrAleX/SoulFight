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
| 9 | Editors.kt / Combat.kt | Comprobar ranura y transición al levantarse | manual |
| 10 | Editors.kt | Comprobar tres acciones de salida | manual |
| 11 | Models.kt | EngineTest.stats | automática |
| 12 | Combat.kt | Comprobar 60/45/30 y máximo 3000 | manual |
| 13 | Models.kt | DECISIONS: conflicto 1080 frente a 2×504 | decisión |
| 14 | Models.kt | EngineTest.frameEconomySpeed | automática |
| 15 | Models.kt / Combat.kt | EngineTest.movementMultipliers; resistencia manual | parcial |
| 16 | Models.kt / Combat.kt | EngineTest.movementMultipliers | parcial |
| 17 | Models.kt | EngineTest.damageButtons | automática |
| 18 | Models.kt / Storage.kt | Comprobar guía, unión y leyenda completa | parcial |
| 19 | Combat.kt / Models.kt | EngineTest.grabClash; comprobar lanzamiento al terminar | parcial |
| 20 | Models.kt | EngineTest.stats | automática |
| 21 | Editors.kt / EditorViews.kt | Borrar seleccionada y última hitbox | manual |
| 22 | Editors.kt | Añadir, renombrar y borrar animación extra | manual |
| 23 | Models.kt / Editors.kt | EngineTest.budget / rejectNegativeBalance | automática |
| 24 | EditorViews.kt / Combat.kt | Falta flecha y ajuste completo de área recorrida | parcial |
| 25 | Combat.kt | Comprobar destino exacto y alineación de hurtbox | manual |
| 26 | Models.kt / Editors.kt | Foto opcional no forma parte del coste | manual |
| 27 | Editors.kt | Redimensionar hitbox con saldo suficiente e insuficiente | manual |
| 28 | Editors.kt | Comprobar repetición 400/80/30 ms | manual |
| 29 | Models.kt / Editors.kt | Inicio y recuperación se editan independientemente | manual |
| 30 | Models.kt | EngineTest.budget | automática |
| 31 | Models.kt / Combat.kt | Comprobar pausa de 60 frames y animación gratis | manual |
| 32 | Models.kt / Editors.kt | Comprobar validación por entidad | manual |
| 33 | Models.kt / Editors.kt | Comprobar rechazo de un segundo teletransporte | manual |
| 34 | Models.kt / Combat.kt | EngineTest.recycleProjectiles | automática |
| 35 | Models.kt / Combat.kt | EngineTest.budget; comprobar caída visual | parcial |
| 36 | Models.kt / EditorViews.kt | EngineTest.attachedValidation; ajuste magnético manual | parcial |
