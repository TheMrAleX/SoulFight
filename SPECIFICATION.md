# PROMPT PARA CODEX CLOUD — FIGHT FORGE

Juego de pelea 2D sandbox, Android nativo, solo horizontal. Pega todo lo que sigue en Codex Cloud.

## 0. Reglas de ejecución (obligatorias)
- Eres un ingeniero senior de juegos Android. Construye el juego COMPLETO en este repositorio de GitHub (ya estás dentro). NO hagas preguntas ni pidas confirmaciones: ante cualquier ambigüedad aplica lo escrito aquí o la opción más razonable y regístrala en DECISIONS.md.
- Prohibido: TODO, stubs, funciones vacías, pantallas «próximamente», botones sin acción, código comentado, datos falsos en pantallas finales. Cada botón hace algo real.
- Si el repo contiene una versión anterior: audítala, conserva solo lo que cumpla esta especificación y reescribe o elimina el resto. Si está vacío, parte de cero.
- Las secciones 1–11 son el diseño final. La sección 12 resuelve 36 errores de la versión anterior y son requisitos duros. Crea TRACEABILITY.md con tabla: error → archivo → test o checklist.
- Meta de calidad: que parezca un juego comercial (sección 2), 60 FPS en gama media, sin crashes, sin ANR, sin saltos de layout.
- No descargues assets externos: todo arte, icono, sonido y música por defecto se genera por código (sección 10).
- Entrega: commits por fase (sección 13), `./gradlew test` en verde, `./gradlew assembleDebug` compila, workflow de GitHub Actions que construye y sube el APK, README con instrucciones. Si el sandbox no tiene Android SDK, escribe igualmente el código con rigor y asegura que `:engine` compila y pasa sus tests en JVM.
- Si el tiempo no alcanza, prioridad: :engine y combate > editor de personajes y movimientos > fondos/interfaz > multijugador > extras.

## 1. Stack y arquitectura
- Kotlin 2.0.20, AGP 8.5.2, Gradle 8.9 (commitea gradlew y wrapper), JDK 17, compileSdk/targetSdk 34, minSdk 29, applicationId com.fightforge.game. Si una versión no resuelve, usa la estable compatible más cercana.
- Módulo `:engine` (Kotlin JVM puro, SIN android.*): simulación determinista, modelos de datos, costes de puntos, geometría de plantillas, IA, lógica de netcode. Módulo `:app`: Jetpack Compose para menús y editores, SurfaceView para el combate, E/S Android.
- Librerías permitidas: Compose BOM 2024.09.00, Navigation Compose, kotlinx-serialization-json, kotlinx-coroutines, JUnit4. Nada más (sin Firebase, anuncios ni analítica).
- Manifest: `screenOrientation=sensorLandscape` en toda actividad, modo inmersivo sticky, displayCutoutMode shortEdges, keepScreenOn en combate. Permisos solo INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE. Sin permisos de almacenamiento: importar con GetContent/GetMultipleContents y exportar a la galería con MediaStore en Pictures/FightForge.
- Simulación: tick fijo 60 Hz con acumulador, render interpolado. 60 frames = 1 s, 600 frames = 10 s. Aritmética entera en punto fijo (1 unidad = 16 subunidades); prohibido Float/Double y Random sin semilla dentro de `:engine` para garantizar determinismo en lockstep.
- Mundo virtual 1920×1080 unidades (u), suelo en y=920, escenario de 3200 u de ancho, cámara sigue el punto medio de los luchadores con zoom 1.0–1.25 y ambos siempre visibles; escalado con letterbox sin deformar. Personaje estándar: hitbox 200×400 u.

## 2. Dirección visual (sistema de diseño obligatorio)
Tema oscuro, estética arcade moderna tipo juego de lucha comercial. Crea `Theme.kt` con estos tokens y úsalos en toda la UI (prohibidos colores sueltos, salvo hitboxes):
- bg #0B0F1A · surface #141B2D · surfaceHigh #1E2742 · borde #2A3556 · primary #FF4D5E · secondary #3DD9EB · gold #FFC83D · texto #F2F5FF · muted #8B95B8 · ok #3DDC84 · danger #FF3B4E.
- Tipografía: FontFamily `sans-serif-condensed` bold; títulos y botones en MAYÚSCULAS con letterSpacing 1sp. Escala: título 28sp, sección 20sp, cuerpo 14sp, etiqueta 12sp.
- Formas: botones con esquinas cortadas (CutCornerShape 12dp), degradado vertical primary → primary oscuro, borde claro 1.5dp. Tarjetas CutCorner 10dp, fondo surface, borde 1dp. Botón secundario: surfaceHigh con borde secondary. Glow sutil (radial secondary 20% alpha) en elementos enfocados.
- Táctil: mínimo 48dp, botones principales 56dp, espaciado base 8dp, márgenes seguros 24dp respetando cutout e insets.
- Movimiento: transición entre pantallas fade + slide horizontal 220 ms; botones escalan a 0.96 al pulsar (90 ms); listas con entrada escalonada de 30 ms; nada supera 400 ms.
- Iconos como ImageVector dibujados por código. Sonidos de UI sintetizados (sección 10). Cada lista tiene estado vacío, cargando y error. Diálogos modales con CutCorner; avisos con Snackbar estilizado.
- Layout pensado para 640×360dp hasta 915×412dp, sin scroll vertical en pantallas principales; el contenido largo va en dos columnas (lista izquierda 35% / detalle derecha 65%) con LazyColumn. Incluye @Preview en 640×360, 800×360 y 915×412.

## 3. Pantallas y navegación
- Splash 1.2 s: logo FIGHT FORGE con glow y barra de carga real (espera a la generación procedural de assets).
- Menú principal: izquierda 40% logo grande y versión; derecha columna de 4 botones 280×56dp separados 12dp: UN JUGADOR, MULTIJUGADOR, TIENDA, CREACIÓN. Fila inferior derecha: OPCIONES y SALIR (48dp, icono + texto). Arriba derecha chip de monedas. Fondo: escenario procedural con dos luchadores en idle y parallax lento.
- Opciones: volumen música, volumen efectos, vibración, opacidad de controles 30–100%, mostrar hitboxes en combate, mostrar entradas, restablecer datos (doble confirmación).
- Un jugador: dos tarjetas grandes CONTRA LA MÁQUINA y PRÁCTICA → selección de personaje → carrusel de fondos → dificultad (Fácil/Normal/Difícil) → combate.
- Selección de personaje: izquierda grilla de 4 columnas con tarjetas 96×96dp (foto de batalla, candado si no se compró); derecha panel 40%: foto grande, nombre, chip de clase, descripción, 4 barras de stats (0–10) y botón PELEAR 56dp abajo a la derecha.
- Creación: tres tarjetas grandes PERSONAJES, FONDOS, INTERFAZ y botón VOLVER.
- Personajes: tarjetas CREAR / EDITAR / BORRAR. Editar y Borrar listan los personajes con miniatura; Borrar pide confirmación.
- Fondos: CREAR / EDITAR / BORRAR. Crear: nombre, DESCARGAR PLANTILLA, IMPORTAR (1 imagen = estático; 2–12 = animado con fps 1–30 y vista previa), música opcional, guardar. Editar permite pasar de estático a animado y viceversa.
- Interfaz: pestañas MÚSICA | INTERFAZ | EFECTOS (sección 9).
- Tienda y modos: sección 10.
- VOLVER siempre arriba a la izquierda (48dp) y funciona también con el gesto atrás del sistema.
- Pausa en combate: botón pequeño arriba centro; menú CONTINUAR, REINICIAR, LISTA DE MOVIMIENTOS (comandos del personaje), SALIR.

## 4. Controles táctiles y HUD de combate
- Joystick abajo izquierda, margen 24dp. Zona táctil 240×240dp, radio visual 60dp. Dibuja `Joystick_fondo` (círculo 140dp) y `Joystick_mando` (64dp). 8 direcciones con zona muerta del 18% e histéresis. Error 1: el joystick es ABSOLUTO: al tocar (ACTION_DOWN) en cualquier punto de la zona, incluida una esquina o borde, calcula el vector desde el centro fijo, mueve el mando a ese punto (limitado al radio) y emite la dirección en ese mismo frame, sin exigir arrastrar desde el centro.
- Botones A, B, C, D en rombo abajo derecha: A abajo, B derecha, C izquierda, D arriba; diámetro 68dp, 76dp entre centros, zona táctil +8dp, opacidad según Opciones. Multitouch con pointerId (joystick y botones simultáneos, varios botones a la vez). Vibración corta opcional.
- Direcciones siempre relativas al lado al que mira el personaje (adelante/atrás).
- Carrera: doble toque adelante o atrás (≤12 frames entre soltar y volver a pulsar) activa correr si el personaje lo permite. Error 2: correr usa su animación, velocidad 2× la de caminar y, si se corre hacia el rival, la barra de poder carga muy poco (+20/s). Se cancela al soltar o atacar.
- HUD (error 3): arriba, a cada lado, foto de perfil cuadrada 56dp con marco 2dp (P1 izquierda, P2 derecha); junto a ella la barra de vida (altura 22dp, forma de paralelogramo, ocupa ~38% del ancho, degradado verde → amarillo → rojo, con estela blanca de daño reciente); debajo la barra de poder (14dp, cian, con marcas en 50% y 100%); nombres bajo las barras; centro: temporizador en círculo de 56dp y puntos de ronda ganada; contador de combo con escala animada. P2 se espeja.

## 5. Motor de combate (`:engine`)
Estados: IDLE, WALK, RUN, CROUCH_DOWN, CROUCH, CROUCH_UP, JUMP, FALL, AIR_RUN, BLOCK_STAND, BLOCK_CROUCH, BLOCK_AIR, HURT, STUN, DIZZY, KO, DOWN, GETUP, ATTACK, GRABBED, TECH. Máquina de estados explícita y testeada.

Fórmulas (Daño, Vida, Velocidad, Alcance son 0–10; 5 = equilibrado, +5 = fuerte, −5 = mediocre; el total repartido es exactamente 20):
- Vida máx = 5000 × (1 + 0.08×(Vida−5)). (La base es 5× la anterior.)
- mulDaño = 0.6 + 0.08×Daño. Con 0 puntos es 0.6, nunca 0.
- mulVel = 1 + 0.06×(Vel−5). Caminar 504 u/s, correr 1080 u/s, ya incluyen el ×1.2 de velocidad global; multiplícalos por mulVel.
- costeAlcance = 1 − 0.06×(Alcance−5): multiplica el coste de TODAS las hitboxes (más Alcance = más barato).
- Salto v0 1500 u/s, gravedad 3000 u/s². Agacharse, levantarse y caer usan sus animaciones.
- Barra de poder máx 3000. Ganancias: golpe dado +60, golpe recibido +45, golpe bloqueado dado +30. (3× más lenta que antes.)
- Hitstun 14 f, blockstun 10 f, daño por bloqueo 10%, retroceso 60 u (golpe) / 40 u (bloqueo). Noqueado (STUN 45 f) si el golpe ≥ 8% de la vida máx. K.O. o lanzamiento 500 u si la vida llega a 0 o el golpe ≥ 20%. DOWN 40 f y GETUP 30 f con invulnerabilidad.
- Mareado: medidor 0–100; cada golpe suma (daño/VidaMax)×400; decae 15/s sin recibir golpes; al llegar a 100 → DIZZY 90 f.
- Escalado de combo: el golpe n multiplica el daño por max(0.4, 1−0.1×(n−1)).
- Ronda: mejor de 3, 99 s; al agotarse gana quien tenga mayor % de vida; empate → ronda extra.
- Ataque = inicio S frames (aún golpeable) + activos A (Básico 4, Comando 6, Ulti 8) + recuperación R (golpeable). Las hitboxes rojas solo golpean durante A y un golpe por activación. La animación se reparte uniformemente sobre S+A+R. Movimiento (celeste) se interpola durante A; teletransporte ocurre al final de S, justo antes del golpe.
- Castigo (naranja): extensión de la hurtbox del atacante durante R, con la huella de la hitbox roja; si el rival lo golpea, el atacante recibe daño. No se genera automáticamente si el movimiento solo lanza proyectil u objetos.
- Comandos: búfer de entrada, cada paso máximo 15 f tras el anterior; gana el comando coincidente más largo. Un comando se define como secuencia de direcciones y botones (máx 8 tokens).
- IA: usa la misma interfaz de entrada que el jugador y los movimientos reales del personaje. Fácil: reacción 20 f, errores 35%. Normal: 12 f, 15%. Difícil: 6 f, 5%. Determinista (semilla).
- Fallbacks de animación: si falta una, usa la cadena (correr atrás→caminar atrás→caminar adelante→quieto; bloqueos→quieto; mareado/noqueado→daño; tumbado/levantarse→KO→quieto; agacharse levantarse→agacharse invertida). Todo cae a QUIETO.

## 6. Editor de personajes
Pantalla de dos paneles: izquierda lista de secciones con icono de estado (completo/pendiente); derecha detalle. Barra superior: VOLVER, título, GUARDAR. Secciones:
- Nombre (máx 16). Clase: Equilibrado 5/5/5/5, Grappler 7/7/3/3, Zoner 4/4/4/8, Rushdown 5/3/8/4, Tanque 5/9/2/4, Asesino 9/2/6/3 (Daño/Vida/Velocidad/Alcance). Elegir clase rellena las stats y luego son editables.
- Tamaño (error 5): editor con silueta estándar punteada de referencia y rectángulo editable con un círculo (asa 28dp) en el punto medio de cada lado; arrastrar un asa cambia ancho o alto por separado. Límites: ancho 100–400 u, alto 200–800 u (mitad a doble del estándar). Muestra medidas numéricas en vivo.
- Descripción (máx 120). Foto de batalla (256×256, plantilla descargable). Efecto al golpear y Efecto al recibir daño (selectores de la pestaña Efectos).
- Animaciones básicas, 21 ranuras: quieto, caminar adelante, caminar atrás, correr adelante*, correr atrás*, agacharse, agachado quieto, agacharse→levantarse (error 9), salto, caída, correr aire adelante*, correr aire atrás*, bloqueo parado, bloqueo agachado, bloqueo aire, daño, noqueado, K.O., tumbado, levantarse, mareado. (*con casilla Habilitado.) Cada ranura: 1–24 frames, fps (def. 12), bucle sí/no. Al tocar una ranura aparecen tres botones: IMPORTAR, FABRICAR y DESCARGAR (error 8).
- Estadísticas: pozo de 20 puntos con 4 filas −/+ (0–10) y barras; no deja gastar más de 20 ni guardar con puntos sin repartir.
- Movimientos: lista con nombre, tipo, comando; botón CREAR MOVIMIENTO (sección 7), editar y borrar.
- Salir (error 10): diálogo con tres botones exactos SALIR, GUARDAR Y SALIR, NO SALIR; si no hay cambios sale directo.
- Para guardar basta con: nombre, foto de batalla, quieto con ≥1 frame y 20 puntos repartidos.
- Animaciones personalizadas (error 22): sección «Animaciones extra» con botón AÑADIR ANIMACIÓN (nombre, y luego IMPORTAR/FABRICAR/DESCARGAR). El selector de animación de un movimiento lista las 21 básicas + las extras + «+ Nueva animación». Añadir, renombrar y borrar animaciones debe funcionar siempre.

## 7. Editor de movimientos y economía de puntos
Flujo: CREAR MOVIMIENTO → pregunta «¿Qué comando te gustaría para este movimiento?» con panel de 8 flechas + A B C D + borrar último; si la secuencia normalizada ya existe en el personaje, muestra aviso y no deja continuar. Básico: un solo botón con dirección opcional. Comando: 2–8 tokens con ≥1 botón. Ulti: 3–8 tokens con ≥1 botón. Luego se abre el editor.

Pantalla dividida: izquierda (38%) vista previa en vivo con hitboxes (azul oscuro #1A237E tu personaje, dorado #FFB300 rival, rojo #E53935 daño, celeste #4FC3F7 movimiento, blanco #FFFFFF teletransporte, naranja #FB8C00 castigo, verde azulado #00BFA5 proyectil, verde #43A047 objeto sólido, magenta #D81B60 objeto translúcido, café #6D4C41 agarre). Derecha (62%): panel con scroll con las opciones. Siempre visible el chip «PUNTOS: n» (error 23: empieza en 0; nunca puede quedar negativo; cualquier acción que lo vuelva negativo se bloquea con aviso).

Opciones: Nombre (único por personaje), Animación (selector), VER (pantalla completa en bucle; se cierra al tocar), Tipo (Básico/Comando/Ulti; cada tipo incluye las opciones del anterior).

Economía (todas las hitboxes se multiplican por costeAlcance):
- Frames de inicio y de recuperación: base 5, mín 1, máx 600. Cada 2 frames sobre 5 dan +1 punto (+0.5/frame); cada frame bajo 5 cuesta 1 punto. La ganancia se multiplica por (1 + 0.1×(Velocidad−5)) (error 14). Inicio puede ser mayor que recuperación (error 29).
- Daño adicional: −50% a +100% en pasos de 5%; +5% cuesta 1 punto, −5% da 0.5.
- Daño base por tipo: Básico 120, Comando 220, Ulti 700. Daño final = base × mulDaño × (1+extra) × (1 + 0.2×(nBotones−1)) (error 17: +0.2 por cada botón del comando y −0.2 una vez), mínimo 1.
- Rojo daño: 1 punto por 1000 u² (mín 40×40 u).
- Celeste movimiento: 1 punto por cada 60 u de desplazamiento (|dx|+|dy|); ×2 si también va y vuelve (error 16); ×2 si mueve al rival (error 15); ambos se multiplican entre sí.
- Blanco teletransporte: 24 puntos fijos al personaje, 72 si mueve al rival (error 30). Solo una por movimiento (error 33).
- Naranja castigo manual: otorga +0.5 puntos por 1000 u², tope 15000 u² (máx +7.5).
- Proyectil: tamaño 1 punto/1000 u², velocidad 1 punto/100 u/s (máx 1800), dirección libre 2 puntos, gravedad 6 puntos (error 35), foto gratis (error 26). Vive 2 s.
- Objeto sólido: tamaño 1/1000 u², vida 1 punto/50 HP (50–1000), movimiento estático 0 / bucle 6 / persigue al rival 14 / persigue al jugador 14, efecto curar 8 / dañar 6 / drenar energía 7 / derribar 10, objetivo solo rival o ambos. Si el efecto perjudica a tu personaje (te daña, drena, derriba o cura al rival) otorga el mismo valor en puntos en vez de costar. Foto opcional. Gravedad +6.
- Objeto translúcido: igual que el sólido sin vida (no se puede golpear ni destruir), duración fija 4 s.
- Cantidad simultánea de proyectiles/objetos: 1 de cada tipo; cada espacio extra cuesta 15 puntos (máx 4); al superar el máximo se recicla el más antiguo (error 34).
- Café agarre (error 19): 30 puntos fijos; hitbox automática en el lado frontal del personaje, no editable (altura 80% de la hurtbox, ancho 50% de la hurtbox × (1 + 0.05×(Alcance−5))); ignora el bloqueo; el rival pasa a GRABBED (sin moverse ni ejecutar comandos) y al terminar la animación recibe 1.8× el daño del movimiento y sale lanzado muy lejos (1200 u). Al añadirla se eliminan las demás hitboxes del movimiento salvo las de los personajes, con diálogo de advertencia. Recuperación permitida hasta 60 frames para poder pagarla. Si dos agarres se tocan en el mismo frame: destello, ambos son separados 300 u, sin daño.
- Comando+: Anti-aéreo 12 puntos (quita el 40% superior de la hurtbox durante el ataque), Anti-suelo 25 puntos (quita el 40% inferior).
- Ulti+: consumo de barra 50–100% en pasos de 5%, da 20 + 0.8×(%−50) puntos (50% → 20, 100% → 60). Animación previa opcional gratis (error 31): pausa 60 frames, reproduce la animación y continúa; sin animación, destellos procedurales. Invulnerabilidad: 40 puntos, cubre inicio y activos.
- Editar una hitbox existente (mover, redimensionar, cambiar parámetros) recalcula su coste total (error 27): agrandar descuenta la diferencia, reducir devuelve la diferencia, y se bloquea si el saldo queda negativo.

Dibujo de hitboxes: modal a pantalla completa con los dos personajes; abajo paleta de chips circulares de 48dp con el color y el coste; elegir color y arrastrar el dedo crea el rectángulo; tocar uno lo selecciona (asas en los 4 lados, arrastrar el cuerpo lo mueve). Error 21: botón papelera flotante de 56dp abajo a la derecha siempre visible, más chip ✕ sobre la hitbox seleccionada y BORRAR ÚLTIMA. Mínimo 20×20 u, máximo 800 u por lado.

Reglas de posición (error 36): las hitboxes roja, celeste y naranja deben quedar pegadas a la hurtbox del personaje (al soltar se ajustan magnéticamente al borde más cercano; si no tocan, se rechazan). Proyectil, objetos y teletransporte se colocan libres a ≤800 u. Solo una hitbox de movimiento (celeste) por entidad: jugador, proyectil, objeto sólido y objeto translúcido (error 32). Error 24: el rectángulo celeste dibujado ES el área recorrida; el desplazamiento es su extensión desde la hurtbox y una flecha lo indica. Error 25: el rectángulo blanco es exactamente el destino del teletransporte. Los botones + y − aceptan mantener pulsado (error 28): tras 400 ms repiten cada 80 ms y aceleran a 30 ms después de 1.5 s.

Validación al guardar: comando único, nombre único, al menos una fuente de daño (roja, proyectil, objeto con efecto dañino o agarre) o un movimiento, saldo ≥ 0. Guardar el personaje persiste todo.

## 8. Plantillas, sprites y FABRICAR
- Constante K = 2 px por unidad. Error 6 y 7: lienzo = (ancho×1.15, alto×1.15) unidades × K, hitbox centrada; el estándar mide 460×920 px. La plantilla se genera bajo demanda con una función pura `TemplateGeometry(ancho, alto)` en `:engine` (no se guarda; se cachea por hash). Es la solución óptima: cada personaje tiene su plantilla exacta sin almacenar archivos.
- DESCARGAR guarda DOS PNG en la galería (Pictures/FightForge): `nombre_guia.png` (guías visibles) y `nombre_lienzo.png` (transparente, mismas medidas). La guía muestra: rectángulo de hitbox con etiqueta «HITBOX (no es el límite del lienzo)», margen seguro con trama diagonal tenue, línea de suelo, cuadrícula cada 50 u, cruz central, medidas en px y nombre. Confirma con Snackbar «Plantilla guardada en la galería».
- Plantilla de movimiento (error 18): lienzo = caja envolvente de la unión de tu hurtbox al inicio y al final (con movimiento y teletransporte), todas las hitboxes del movimiento en posiciones inicial y final, y la hurtbox del rival, ×1.15 y × K, dibujadas con sus colores y leyenda. El offset/tamaño del lienzo se guarda con la animación para que el motor alinee el sprite exacto.
- Otras plantillas: foto de batalla 256×256, fondo 1920×1080 por frame, barra vida 800×64, barra poder 800×32, Joystick_fondo 280×280, Joystick_mando 128×128, cada botón 136×136, efecto 256×256 por frame, proyectil/objeto = su hitbox ×1.15 × K.
- Los sprites se dibujan al tamaño del lienzo (1.15× la hitbox) y se alinean por el centro de la hitbox.
- IMPORTAR: GetMultipleContents (PNG con alfa), valida medidas; si no coinciden ofrece AJUSTAR (contain y centrado) o cancelar; máx 4096 px, se reescala si excede. Reordenar por arrastre, duplicar, borrar frames.
- FABRICAR: editor de píxeles integrado: lienzo a tamaño de plantilla, herramientas lápiz (1–16 px), borrador, relleno, cuentagotas, línea, rectángulo, deshacer/rehacer 50 pasos, paleta de 32 colores + selector HSV, zoom 1–8× por pellizco y paneo con 2 dedos, capa guía con hitbox, onion skin 30%, tira de frames (añadir, duplicar, borrar, mover), reproducción con fps. Exporta PNG por frame.

## 9. Fondos e Interfaz
- MÚSICA: lista de pistas con IMPORTAR (audio/*), renombrar, borrar, reproducir/parar; asignar a Menú, Combate o a un fondo concreto.
- INTERFAZ: barra de vida, barra de poder y CONTROLES (error 4): Joystick_fondo, Joystick_mando, Boton_A, Boton_B, Boton_C, Boton_D. Cada elemento tiene DESCARGAR PLANTILLA, IMPORTAR, RESTABLECER, con vista previa del HUD en vivo.
- EFECTOS: crear efecto (nombre, 1–12 frames, fps, escala, sonido opcional) para asignar a «Efecto al golpear» y «Efecto al recibir daño» de un personaje. Hay efectos por defecto.

## 10. Modos, tienda, multijugador y contenido por defecto
- Contra la máquina: mejor de 3, monedas: victoria +100 (Fácil +60, Difícil +150), derrota +20.
- Práctica: muñeco con modos de pie, agachado, saltando, bloqueando; vida infinita on/off; hitboxes visibles; entradas visibles; reiniciar posiciones.
- Multijugador (LAN WiFi): CREAR SALA (nombre, personaje, fondo, muestra IP y código; anuncia por NsdManager `_fightforge._tcp`) y UNIRSE A SALA (lista descubierta + IP manual). Protocolo TCP con TCP_NODELAY, lockstep determinista con retraso de entrada de 3 frames, checksum del estado cada 60 ticks; si hay desincronización o desconexión, vuelve al menú con mensaje claro. Antes de pelear intercambian los paquetes de personaje/fondo (JSON + PNG) con verificación SHA-256, sin reenviar lo ya cacheado. Victoria +80 monedas.
- Tienda: grilla con los 6 personajes por defecto, precios 0 (Kenji y Brutus, iniciales), 300, 500, 500, 800; compra con confirmación; estado COMPRADO persistente. Los personajes creados por el jugador son gratis.
- Personajes por defecto (clases): Kenji (Equilibrado), Brutus (Grappler), Lyra (Zoner), Raiden (Rushdown), Golem (Tanque), Sombra (Asesino). Cada uno con 4–6 movimientos reales (Básicos, Comandos y una Ulti), validados con las mismas reglas de puntos.
- Arte procedural: clase `ProceduralSprites` dibuja humanoides geométricos animados (poses por animación) con paleta distinta por personaje; tres fondos procedurales (gradiente + capas parallax); iconos vectoriales; efectos de golpe de chispas.
- Audio procedural: sintetiza WAV (efectos de UI, golpe, bloqueo, K.O., ulti) y dos loops de música simples (menú y combate) en el primer arranque, los cachea y los reproduce con SoundPool/MediaPlayer.

## 11. Persistencia
- filesDir/characters/{id}/character.json + animations/{animId}/{n}.png; stages, ui, music, effects análogos. `schemaVersion` y migraciones. Escritura atómica (archivo temporal + rename). Monedas, compras y opciones en DataStore o JSON.
- Serialización con kotlinx-serialization; si un archivo está corrupto, el juego lo omite, avisa y no se cierra. Exportar/importar personajes como paquete .zip (usado también por el multijugador).

## 12. Errores a corregir de la versión anterior (requisitos duros)
1. Joystick absoluto: reacciona al tocar bordes/esquinas (sección 4).
2. Doble toque adelante/atrás = correr (animación, 2× velocidad, +20/s de poder si corre hacia el rival).
3. Foto de perfil a la izquierda de la barra de P1 y a la derecha de la de P2.
4. Controles editables en Interfaz: Joystick_fondo, Joystick_mando, Boton_A, Boton_B, Boton_C, Boton_D.
5. Tamaño de hitbox con asas en el centro de cada lado, ancho y alto separados, límites ×0.5 a ×2.
6. Sprites 1.15× la hitbox y plantillas mostrando que la hitbox no es el límite.
7. Plantilla por personaje generada bajo demanda y cacheada (óptimo).
8. Tercera opción DESCARGAR junto a IMPORTAR y FABRICAR.
9. Animación «agacharse→levantarse».
10. Diálogo de salida con SALIR, GUARDAR Y SALIR, NO SALIR.
11. Vida base ×5 (5000).
12. Barra de poder 3× más lenta (máx 3000, ganancias 60/45/30).
13. Velocidad global ×1.2 (504 y 1080 u/s).
14. Más Velocidad = más puntos por frames extra.
15. Movimiento puede mover al rival, ×2 de coste, el rival puede resistirse manteniendo la dirección opuesta: desplazamiento efectivo = velocidad de atracción − 0.8 × su velocidad de caminar (mín 0).
16. Movimiento de ida y vuelta, ×2 de coste.
17. Daño × (1 + 0.2×(nBotones−1)).
18. Plantilla de movimiento con todas las hitboxes usadas.
19. Hitbox café de agarre.
20. 0 puntos de Daño = daño mínimo (×0.6), nunca 0.
21. Botón de borrar hitbox accesible.
22. Poder añadir animaciones siempre.
23. Cada movimiento empieza con 0 puntos.
24. El área de la hitbox de movimiento es el área recorrida.
25. El destino del teletransporte es donde se coloca la hitbox blanca.
26. La foto del proyectil no cuesta puntos.
27. Editar una hitbox también cuesta puntos.
28. Mantener + y − suma o resta rápido.
29. Frames de inicio pueden superar a los de recuperación.
30. Teletransporte aplicable al rival a mucho más coste.
31. La animación de la ulti no cuesta puntos.
32. Una sola hitbox de movimiento por jugador, proyectil, objeto sólido y translúcido.
33. Una sola hitbox de teletransporte por movimiento.
34. Los proyectiles y objetos se reciclan; más cantidad cuesta muchos puntos.
35. Opción de gravedad para proyectiles y objetos.
36. Las hitboxes de daño, movimiento y castigo van pegadas a la hurtbox.

## 13. Fases, pruebas y criterio de aceptación
Fases (un commit por fase): 1) Proyecto, tema y navegación vacía funcional. 2) `:engine`: modelos, fórmulas, estados, comandos, hitboxes, IA. 3) Renderizado de combate, HUD y controles. 4) Contenido por defecto procedural y modos Un jugador/Práctica. 5) Editor de personajes y animaciones (importar, fabricar, descargar). 6) Editor de movimientos y economía. 7) Fondos e Interfaz. 8) Tienda y monedas. 9) Multijugador LAN. 10) Pulido, README, DECISIONS.md, TRACEABILITY.md, GitHub Actions.

Tests unitarios obligatorios en `:engine`: fórmulas de stats, costes de todas las hitboxes y de frames, regla de daño por botones, parser y duplicados de comandos, validación de pegado a hurtbox, geometría de plantillas (460×920 px para el estándar, límites de tamaño), determinismo (mismas entradas → mismo hash tras 3600 ticks), empate de agarres, reciclado de proyectiles, saldo nunca negativo, fallback de animaciones, joystick absoluto y detección de doble toque.

Criterio de aceptación: se puede crear un personaje completo, crear movimientos de los tres tipos con puntos, pelear contra la máquina y en práctica, comprar en la tienda, crear un fondo, cambiar la interfaz y jugar en LAN, todo sin crashes, con el diseño de la sección 2 aplicado en cada pantalla.

## ANEXO (opcional)
Debajo puede pegarse el texto original del juego. Si algo contradice este prompt, prevalece este prompt.
