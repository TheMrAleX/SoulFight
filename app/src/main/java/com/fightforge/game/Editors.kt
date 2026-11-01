package com.fightforge.game
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
import com.fightforge.engine.State
import java.io.File
import java.util.UUID

val animationSlots=listOf("quieto","caminar adelante","caminar atrás","correr adelante","correr atrás","agacharse","agachado quieto","agacharse→levantarse","salto","caída","correr aire adelante","correr aire atrás","bloqueo parado","bloqueo agachado","bloqueo aire","daño","noqueado","K.O.","tumbado","levantarse","mareado")
@Composable fun CharacterEditor(store: Storage,initial: FighterDef,back: ()->Unit,message: (String)->Unit) {
 var request by remember {mutableStateOf<ImportRequest?>(null)}
 var f by remember { mutableStateOf(initial) }; var section by remember { mutableStateOf("NOMBRE") }; var dirty by remember { mutableStateOf(false) }; var leave by remember { mutableStateOf(false) }
 var slot by remember { mutableStateOf("quieto") }; var extra by remember { mutableStateOf("") }; var move by remember { mutableStateOf<Move?>(null) }; var moveIndex by remember { mutableIntStateOf(-1) }; var painting by remember { mutableStateOf(false) }
 fun edit(next: FighterDef) { f=next; dirty=true }
 fun save(): Boolean {
  if(f.name.isBlank() || f.name.length>16 || !f.stats.valid()) { message("Nombre de 1–16 caracteres y exactamente 20 puntos requeridos"); return false }
  if(f.animations["quieto"].isNullOrEmpty() || f.animations["foto"].isNullOrEmpty()) { message("Importa o fabrica una foto de batalla y al menos un frame QUIETO"); return false }
  val invalid=f.moves.flatMap {Economy.errors(it,f.stats,MotionGeometry.body(f.width,f.height))}; if(invalid.isNotEmpty()) {message("Revisa los movimientos: "+invalid.distinct().joinToString("; ")); return false }; store.character(f); dirty=false; return true
 }
 fun attemptExit() { if(dirty) leave=true else back() }
 BackHandler { if(move!=null) move=null else attemptExit() }
 val imports=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris ->
  if(uris.isNotEmpty()) {val selectedSlot=slot; val geometry=TemplateGeometry.cached(f.width,f.height); val config=f.animationSettings[slot]; val w=if(slot=="foto") 256 else config?.canvasWidth?.takeIf {it>0} ?: geometry.pixelsWide; val h=if(slot=="foto") 256 else config?.canvasHeight?.takeIf {it>0} ?: geometry.pixelsHigh
   val fit=minOf(1.0,4096.0/maxOf(w,h)); request=ImportRequest(uris.take(if(slot=="foto") 1 else 24),"characters/${f.id}/animations/${slot.hashCode()}",(w*fit).toInt().coerceAtLeast(1),(h*fit).toInt().coerceAtLeast(1)){paths -> edit(f.copy(animations=f.animations+(selectedSlot to paths)))}
  }
 }
 LaunchedEffect(section){if(section=="ANIMACIONES" && slot !in animationSlots) slot="quieto"}
 if(move!=null) { MoveEditor(store,move!!,f,{ m -> val next=f.moves.toMutableList(); if(moveIndex<0) next.add(m) else next[moveIndex]=m; val bounds=TemplateGeometry.movement(MotionGeometry.body(f.width,f.height),Rect(400,-400,200,400),m.boxes); edit(f.copy(moves=next,animationSettings=if(m.animation !in animationSlots && f.animations[m.animation].isNullOrEmpty()) f.animationSettings+(m.animation to AnimationSettings(canvasWidth=bounds.w,canvasHeight=bounds.h,offsetX=bounds.x,offsetY=bounds.y)) else f.animationSettings,animations=if(m.animation !in animationSlots && m.animation !in f.animations) f.animations+(m.animation to emptyList()) else f.animations)); move=null },{move=null},message); return }
 if(painting) { PixelEditor(store,f,slot,{ paths,fps -> edit(f.copy(animations=f.animations+(slot to paths),animationSettings=f.animationSettings+(slot to (f.animationSettings[slot] ?: AnimationSettings()).copy(fps=fps)))); painting=false },{painting=false}); return }
 Frame("EDITOR DE PERSONAJE",::attemptExit,action={ForgeButton("GUARDAR") { if(save()) message("Personaje guardado") }}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   LazyColumn(Modifier.weight(.32f),verticalArrangement=Arrangement.spacedBy(8.dp)) { val sections=listOf("NOMBRE","TAMAÑO","ESTADÍSTICAS","FOTO","ANIMACIONES","ANIMACIONES EXTRA","EFECTOS","MOVIMIENTOS"); items(sections.size) { i -> val ready=when(sections[i]) {"NOMBRE" -> f.name.isNotBlank(); "TAMAÑO" -> f.width in 100..400 && f.height in 200..800; "ESTADÍSTICAS" -> f.stats.valid(); "FOTO" -> !f.animations["foto"].isNullOrEmpty(); "ANIMACIONES" -> !f.animations["quieto"].isNullOrEmpty(); "ANIMACIONES EXTRA" -> f.animations.keys.any {it!="foto" && it !in animationSlots}; "EFECTOS" -> true; else -> f.moves.isNotEmpty() && f.moves.all {Economy.errors(it,f.stats,MotionGeometry.body(f.width,f.height)).isEmpty()}}
    ForgeButton(sections[i],Modifier.fillMaxWidth(),section!=sections[i],leading=if(ready) ForgeIcons.Check else ForgeIcons.Pending,leadingColor=if(ready) ForgeColors.ok else ForgeColors.muted) { section=sections[i] } } }
   Column(Modifier.weight(.68f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    when(section) {
     "NOMBRE" -> { OutlinedTextField(f.name,{edit(f.copy(name=it.take(16)))},label={Text("NOMBRE")}); OutlinedTextField(f.description,{edit(f.copy(description=it.take(120)))},label={Text("DESCRIPCIÓN")}); Text("CLASE"); Defaults.names.indices.forEach { i -> ForgeButton(listOf("EQUILIBRADO","GRAPPLER","ZONER","RUSHDOWN","TANQUE","ASESINO")[i],secondary=true) {edit(f.copy(stats=Defaults.classes[i]))} } }
     "TAMAÑO" -> { Text("${f.width} × ${f.height} u"); AndroidView(factory={ SizeView(it,f.width,f.height) { w,h -> edit(f.copy(width=w,height=h)) } },modifier=Modifier.fillMaxWidth().height(240.dp)); Text("Arrastra las asas centrales de cada lado. Ancho 100–400 u, alto 200–800 u.",color=ForgeColors.muted) }
     "ESTADÍSTICAS" -> { val values=listOf(f.stats.damage,f.stats.life,f.stats.speed,f.stats.reach); Text("PUNTOS DISPONIBLES: ${20-values.sum()}",color=ForgeColors.gold); listOf("DAÑO","VIDA","VELOCIDAD","ALCANCE").forEachIndexed { i,label -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text(label,Modifier.weight(1f)); ForgeButton("−",secondary=true) { if(values[i]>0) {val n=values.toMutableList(); n[i]--; edit(f.copy(stats=Stats(n[0],n[1],n[2],n[3]))) } }; Text(values[i].toString()); ForgeButton("+",secondary=true) { if(values[i]<10 && values.sum()<20) {val n=values.toMutableList(); n[i]++; edit(f.copy(stats=Stats(n[0],n[1],n[2],n[3]))) } } } } }
     "FOTO","ANIMACIONES","ANIMACIONES EXTRA" -> {
      if(section=="FOTO") slot="foto"
      val slots=when(section) { "FOTO" -> listOf("foto"); "ANIMACIONES" -> animationSlots; else -> f.animations.keys.filter { it !in animationSlots && it!="foto" } }
      if(section=="ANIMACIONES EXTRA") { OutlinedTextField(extra,{extra=it},label={Text("NOMBRE DE ANIMACIÓN")}); ForgeButton("AÑADIR ANIMACIÓN") { if(extra.isNotBlank() && extra !in f.animations && extra !in animationSlots) {edit(f.copy(animations=f.animations+(extra to emptyList()))); slot=extra; extra=""} }; if(slots.isEmpty()) Text("Crea una animación extra para asignarla a tus movimientos.") }
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {slots.forEach { name -> ForgeButton("$name · ${f.animations[name]?.size ?: 0} FRAMES",secondary=slot!=name) {slot=name} }}
      Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { ForgeButton("IMPORTAR",secondary=true) {imports.launch("image/png")}; ForgeButton("FABRICAR",secondary=true) {painting=true}; ForgeButton("DESCARGAR",secondary=true) { runCatching { store.template(f,slot) }.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message(it.message ?: "Error al guardar")} } }
      if(slot!="foto") {
       val config=f.animationSettings[slot] ?: AnimationSettings()
       Counter("FPS",config.fps,1,30){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(fps=it))))}
       CheckRow("BUCLE",config.loop){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(loop=it))))}
       if(slot.startsWith("correr")) CheckRow("HABILITADO",config.enabled){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(enabled=it))))}
      }
      if(section=="ANIMACIONES EXTRA" && slot in slots) ForgeButton("RENOMBRAR",secondary=true){if(extra.isNotBlank() && extra !in f.animations && extra !in animationSlots) {val old=slot; edit(f.copy(animations=(f.animations-old)+(extra to f.animations.getValue(old)),animationSettings=(f.animationSettings-old)+(extra to (f.animationSettings[old] ?: AnimationSettings())),moves=f.moves.map {it.copy(animation=if(it.animation==old) extra else it.animation,introAnimation=if(it.introAnimation==old) extra else it.introAnimation)})); slot=extra; extra=""}}
      f.animations[slot]?.forEachIndexed { i,path -> Row(verticalAlignment=Alignment.CenterVertically) { Text("FRAME ${i+1}",Modifier.weight(1f)); TextButton({ val frames=f.animations[slot]!!.toMutableList(); if(i>0) {java.util.Collections.swap(frames,i,i-1); edit(f.copy(animations=f.animations+(slot to frames)))}}) {Text("↑")}; TextButton({edit(f.copy(animations=f.animations+(slot to ((f.animations[slot]!!+path).take(24)))))}){Text("DUPLICAR")}; TextButton({val frames=f.animations[slot]!!.toMutableList(); frames.removeAt(i); edit(f.copy(animations=f.animations+(slot to frames)))}){Text("BORRAR")} } }
      if(section=="ANIMACIONES EXTRA" && slot in slots) ForgeButton("BORRAR ANIMACIÓN",secondary=true) { edit(f.copy(animations=f.animations-slot,animationSettings=f.animationSettings-slot,moves=f.moves.map {it.copy(animation=if(it.animation==slot) "quieto" else it.animation,introAnimation=if(it.introAnimation==slot) "" else it.introAnimation)})); slot="quieto" }
     }
     "EFECTOS" -> {
      Text("EFECTO AL GOLPEAR"); (listOf("sparks" to "CHISPAS","shock" to "IMPACTO","burst" to "AURA")+store.data.effectsLibrary.map {it.id to it.name}).forEach {(id,name) -> TextButton({edit(f.copy(hitEffect=id))}){Text(name,color=if(f.hitEffect==id) ForgeColors.gold else ForgeColors.text)}}
      Text("EFECTO AL RECIBIR DAÑO"); (listOf("sparks" to "CHISPAS","shock" to "IMPACTO","burst" to "AURA")+store.data.effectsLibrary.map {it.id to it.name}).forEach {(id,name) -> TextButton({edit(f.copy(hurtEffect=id))}){Text(name,color=if(f.hurtEffect==id) ForgeColors.gold else ForgeColors.text)}}
     }
     "MOVIMIENTOS" -> { ForgeButton("CREAR MOVIMIENTO") {moveIndex=-1; move=Move("",emptyList())}; f.moves.forEachIndexed { i,m -> Row(verticalAlignment=Alignment.CenterVertically) { Text("${m.name} · ${m.command.joinToString(" ")}",Modifier.weight(1f)); TextButton({moveIndex=i; move=m}){Text("EDITAR")}; TextButton({edit(f.copy(moves=f.moves.filterIndexed { j,_ -> i!=j }))}){Text("BORRAR")} } } }
    }
   }
  }
 }
 ImageImportDialog(store,request,{request=null},message)
 if(leave) AlertDialog(onDismissRequest={leave=false},title={Text("CAMBIOS SIN GUARDAR")},text={Text("¿Quieres guardar el personaje?")},confirmButton={TextButton({if(save()) back(); leave=false}){Text("GUARDAR Y SALIR")}},dismissButton={Row {TextButton({leave=false; back()}){Text("SALIR")}; TextButton({leave=false}){Text("NO SALIR")}}})
}
@Composable fun MoveEditor(store: Storage,initial: Move,f: FighterDef,save: (Move)->Unit,back: ()->Unit,message: (String)->Unit) {
 val body=MotionGeometry.body(f.width,f.height)
 var previewTick by remember {mutableIntStateOf(0)}
 LaunchedEffect(Unit){while(true){delay(1000L/60); previewTick++}}
 var commandConfirmed by remember {mutableStateOf(initial.command.isNotEmpty())}
 var m by remember {mutableStateOf(initial)}; var newAnimation by remember {mutableStateOf("")}; var pendingGrab by remember {mutableStateOf<Box?>(null)}; var boxKind by remember {mutableStateOf(BoxKind.DAMAGE)}; var drawing by remember {mutableStateOf(false)}; var preview by remember {mutableStateOf(false)}; var selected by remember {mutableIntStateOf(-1)}
 val photoImport=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null && selected in m.boxes.indices) runCatching {val path=store.importImage(uri,"characters/${f.id}/objects"); val list=m.boxes.toMutableList(); list[selected]=list[selected].copy(photo=path); m=m.copy(boxes=list)}.onFailure {message("Imagen no válida")} }
 fun candidate(n: Move) { if(Economy.balance(n,f.stats,body)<0 && Economy.balance(n,f.stats,body)<=Economy.balance(m,f.stats,body)) message("No hay puntos suficientes: añade inicio o recuperación") else m=n }
 if(!commandConfirmed) {
  Frame("¿QUÉ COMANDO TE GUSTARÍA PARA ESTE MOVIMIENTO?",back) {
   Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
    Column(Modifier.weight(.4f).verticalScroll(rememberScrollState())){CommandPad(m.command){m=m.copy(command=it)}}
    Column(Modifier.weight(.6f),verticalArrangement=Arrangement.spacedBy(16.dp)) {Text(m.command.joinToString(" → ").ifEmpty {"ELIGE DIRECCIONES Y BOTONES"},color=ForgeColors.gold); Text("Hasta 8 pasos. Usa al menos un botón A, B, C o D."); ForgeButton("CONTINUAR"){val c=Commands.normalized(m.command); val kind=if(Commands.valid(c,Kind.BASIC)) Kind.BASIC else Kind.COMMAND; if(!Commands.valid(c,kind)) message("Comando inválido") else if(f.moves.filterNot {it===initial}.any {Commands.normalized(it.command)==c}) message("Ese comando ya existe") else {m=m.copy(command=c,kind=kind); commandConfirmed=true}}}
   }
  }; return
 }
 if(preview) {MovePreview(f,m){preview=false}; return}
 BackHandler {if(drawing) drawing=false else back()}
 if(drawing) {
  Frame("DIBUJAR HITBOXES",{drawing=false},action={ForgeButton("BORRAR ÚLTIMA",secondary=true){if(m.boxes.isNotEmpty()) m=m.copy(boxes=m.boxes.dropLast(1))}}) {
   HitboxPalette(boxKind,f.stats){boxKind=it}
   AndroidView(factory={ HitboxView(it,f,m.boxes,{boxKind},{rect ->
    val b=Box(boxKind,rect)
    if(boxKind==BoxKind.TELEPORT && m.boxes.any {it.kind==BoxKind.TELEPORT}) message("Solo se permite un teletransporte")
    else if(boxKind==BoxKind.GRAB && m.boxes.isNotEmpty()) pendingGrab=b else candidate(m.copy(boxes=if(boxKind==BoxKind.GRAB) listOf(b) else m.boxes+b))
   },{index,rect -> val list=m.boxes.toMutableList(); list[index]=list[index].copy(rect=rect); candidate(m.copy(boxes=list))},{index -> selected=index}) },update={it.boxes=m.boxes; it.selected=selected},modifier=Modifier.weight(1f).fillMaxWidth())
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {FloatingActionButton(onClick={if(selected in m.boxes.indices){m=m.copy(boxes=m.boxes.filterIndexed {index,_ -> index!=selected}); selected=-1} else message("Selecciona una hitbox para borrarla")},modifier=Modifier.size(56.dp),containerColor=ForgeColors.high,contentColor=ForgeColors.danger){Icon(ForgeIcons.Trash,"BORRAR HITBOX SELECCIONADA")}}
  }
  pendingGrab?.let {grab -> AlertDialog(onDismissRequest={pendingGrab=null},title={Text("AÑADIR AGARRE")},text={Text("Se eliminarán las otras hitboxes de este movimiento.")},confirmButton={TextButton({candidate(m.copy(boxes=listOf(grab))); pendingGrab=null}){Text("CONTINUAR")}},dismissButton={TextButton({pendingGrab=null}){Text("CANCELAR")}})}
  return
 }
 Frame("MOVIMIENTO · PUNTOS ${points(maxOf(0,Economy.balance(m,f.stats,body)))}",back,action={ForgeButton("GUARDAR") {
  val errors=Economy.errors(m,f.stats,Rect(-f.width/2,-f.height,f.width,f.height)).toMutableList()
  if(f.moves.any {it.name==m.name && it!==initial}) errors.add("Nombre duplicado")
  if(f.moves.any {Commands.normalized(it.command)==Commands.normalized(m.command) && it!==initial}) errors.add("Comando duplicado")
  if(errors.isEmpty()) save(m) else message(errors.joinToString("\n"))
 }}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   Column(Modifier.weight(.38f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    key(m){AndroidView(factory={MovePreviewView(it,f,m)},update={it.tick=previewTick; it.invalidate()},modifier=Modifier.fillMaxWidth().height(160.dp))}
    Text("COMANDO: ${m.command.joinToString(" → ")}",color=ForgeColors.gold)
    CommandPad(m.command){m=m.copy(command=it)}
    ForgeButton("BORRAR ÚLTIMO",secondary=true) {m=m.copy(command=m.command.dropLast(1))}
    ForgeButton("DIBUJAR HITBOXES") {drawing=true}
    ForgeButton("VER ANIMACIÓN",secondary=true){preview=true}
    ForgeButton("DESCARGAR PLANTILLA",secondary=true){runCatching {store.movementTemplate(f,m)}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}}
    m.boxes.forEachIndexed { i,b -> Row { TextButton({selected=i}) {Text("${b.kind.title()} ${b.rect.w}×${b.rect.h}",color=if(selected==i) ForgeColors.gold else ForgeColors.secondary)}; TextButton({m=m.copy(boxes=m.boxes.filterIndexed { j,_ -> i!=j }); selected=-1}){Text("✕")} } }
   }
   Column(Modifier.weight(.62f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(m.name,{m=m.copy(name=it.take(24))},label={Text("NOMBRE")})
    Row { Kind.entries.forEach {kind -> TextButton({candidate(m.copy(kind=kind))}){Text(kind.name,color=if(m.kind==kind) ForgeColors.gold else ForgeColors.secondary)} } }
    if(Economy.balance(m,f.stats,body)<0) Text("Reequilibra ${-Economy.balance(m,f.stats,body)/1000} puntos tras cambiar estadísticas",color=ForgeColors.danger)
    Counter("INICIO",m.startup,1,600) {candidate(m.copy(startup=it))}; Counter("RECUPERACIÓN",m.recovery,1,600){candidate(m.copy(recovery=it))}; Counter("DAÑO EXTRA %",m.extra,-50,100,5){candidate(m.copy(extra=it))}
    if(selected in m.boxes.indices) {
     val box=m.boxes[selected]
     fun updateBox(next: Box) {val list=m.boxes.toMutableList(); list[selected]=next; candidate(m.copy(boxes=list))}
     Text("HITBOX ${selected+1}: ${box.kind.title()}",color=ForgeColors.gold)
     if(box.kind!=BoxKind.GRAB) {
     Counter("X",box.rect.x,-800,800,20){updateBox(box.copy(rect=box.rect.copy(x=it)))}
     Counter("Y",box.rect.y,-800,800,20){updateBox(box.copy(rect=box.rect.copy(y=it)))}
     if(box.kind!=BoxKind.TELEPORT) {Counter("ANCHO",box.rect.w,20,800,20){updateBox(box.copy(rect=box.rect.copy(w=it)))}
     Counter("ALTO",box.rect.h,20,800,20){updateBox(box.copy(rect=box.rect.copy(h=it)))}
     }}
     if(box.kind in listOf(BoxKind.MOVEMENT,BoxKind.TELEPORT)) CheckRow("MOVER AL RIVAL",box.rival){updateBox(box.copy(rival=it,rect=if(box.kind==BoxKind.TELEPORT) box.rect.copy(w=if(it) 200 else f.width,h=if(it) 400 else f.height) else box.rect))}
     if(box.kind==BoxKind.MOVEMENT) {
      CheckRow("IDA Y VUELTA",box.returnTrip){updateBox(box.copy(returnTrip=it))}
      listOf("player","projectile","solid","ghost").forEach {entity -> TextButton({updateBox(box.copy(entity=entity))}){Text(entity.uppercase(),color=if(entity==box.entity) ForgeColors.gold else ForgeColors.secondary)}}
     }
     if(box.kind in listOf(BoxKind.PROJECTILE,BoxKind.SOLID,BoxKind.GHOST)) {
      ForgeButton("IMPORTAR FOTO (GRATIS)",secondary=true){photoImport.launch("image/*")}
      CheckRow("GRAVEDAD",box.gravity){updateBox(box.copy(gravity=it))}
      Counter("SIMULTÁNEOS",box.slots,1,4){updateBox(box.copy(slots=it))}
      Counter("VELOCIDAD",box.speed,0,1800,100){updateBox(box.copy(speed=it))}
      if(box.kind==BoxKind.PROJECTILE) {CheckRow("DIRECCIÓN LIBRE",box.freeDirection){updateBox(box.copy(freeDirection=it))}; if(box.freeDirection){Counter("DIRECCIÓN X %",box.directionX,-100,100,10){updateBox(box.copy(directionX=it))}; Counter("DIRECCIÓN Y %",box.directionY,-100,100,10){updateBox(box.copy(directionY=it))}}}
      else {
       if(box.kind==BoxKind.SOLID) Counter("VIDA",box.hp,50,1000,50){updateBox(box.copy(hp=it))}
       Text("MOVIMIENTO DEL OBJETO")
       listOf("ESTÁTICO" to 0,"BUCLE" to 6,"PERSIGUE RIVAL" to 14,"PERSIGUE JUGADOR" to 15).forEach {(label,cost) -> TextButton({updateBox(box.copy(behavior=cost))}){Text(label)}}
       Text("EFECTO")
       listOf("CURAR" to 8,"DAÑAR" to 6,"DRENAR" to 7,"DERRIBAR" to 10).forEach {(label,cost) -> TextButton({updateBox(box.copy(effect=cost))}){Text(label)}}
       CheckRow("AFECTA A AMBOS",box.both){updateBox(box.copy(both=it))}
       CheckRow("PERJUDICA AL CREADOR",box.harmfulToOwner){updateBox(box.copy(harmfulToOwner=it))}
      }
     }
    }
    OutlinedTextField(newAnimation,{newAnimation=it.take(32)},label={Text("+ NUEVA ANIMACIÓN")})
    ForgeButton("CREAR Y ASIGNAR",secondary=true){if(newAnimation.isNotBlank()) {m=m.copy(animation=newAnimation); message("Al guardar, la animación extra quedará disponible en el personaje")}}
    Text("ANIMACIÓN: ${m.animation}"); (animationSlots+f.animations.keys.filter {it !in animationSlots}).forEach { name -> TextButton({m=m.copy(animation=name)}){Text(name)} }
    if(m.kind!=Kind.BASIC) { CheckRow("ANTI-AÉREO",m.antiAir){candidate(m.copy(antiAir=it))}; CheckRow("ANTI-SUELO",m.antiGround){candidate(m.copy(antiGround=it))} }
    if(m.kind==Kind.ULTIMATE) {Text("ANIMACIÓN PREVIA GRATIS: ${m.introAnimation.ifBlank {"DESTELLOS"}}")
     TextButton({m=m.copy(introAnimation="")}){Text("DESTELLOS")}
     f.animations.keys.filter {it!="foto"}.forEach {name -> TextButton({m=m.copy(introAnimation=name)}){Text(name)}}
     Counter("CONSUMO %",m.powerPercent,50,100,5){candidate(m.copy(powerPercent=it))}; CheckRow("INVULNERABILIDAD",m.invulnerable){candidate(m.copy(invulnerable=it))} }
   }
  }
 }
}
@Composable fun RepeatControl(label: String,action: ()->Unit) {
 val current by rememberUpdatedState(action)
 Box(Modifier.size(48.dp).background(ForgeColors.high,androidx.compose.foundation.shape.CutCornerShape(10.dp)).pointerInput(label) {
  detectTapGestures(onPress={current(); coroutineScope {val job=launch {delay(400); var elapsed=400; while(true) {current(); val interval=if(elapsed>=1500) 30 else 80; delay(interval.toLong()); elapsed+=interval}}; tryAwaitRelease(); job.cancel()}})
 },contentAlignment=Alignment.Center){Text(label,color=ForgeColors.secondary)}
}
@Composable fun Counter(label: String,value: Int,min: Int,max: Int,step: Int=1,change: (Int)->Unit) {
 Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {Text(label,Modifier.weight(1f)); RepeatControl("−"){change((value-step).coerceAtLeast(min))}; Text(value.toString()); RepeatControl("+"){change((value+step).coerceAtMost(max))}}
}
@Composable fun CheckRow(label: String,value: Boolean,change: (Boolean)->Unit) { Row(Modifier.heightIn(min=48.dp).toggleable(value=value,role=Role.Checkbox,onValueChange=change),verticalAlignment=Alignment.CenterVertically) { Checkbox(value,null); Text(label) } }
@Composable fun PixelEditor(store: Storage,f: FighterDef,slot: String,save: (List<String>,Int)->Unit,back: ()->Unit) {PixelWorkbench(store,f,slot,save,back)}

@Composable fun AnimationPreview(f: FighterDef,slot: String,back: ()->Unit) {
 val frames=f.animations[slot] ?: emptyList(); var tick by remember {mutableIntStateOf(0)}
 LaunchedEffect(slot) {while(true){delay(1000L/(f.animationSettings[slot]?.fps ?: 12)); tick++}}
 BackHandler {back()}
 Box(Modifier.fillMaxSize().background(ForgeColors.bg).clickable {back()},contentAlignment=Alignment.Center) {
  if(frames.isNotEmpty()) AndroidView(factory={android.widget.ImageView(it).apply {scaleType=android.widget.ImageView.ScaleType.FIT_CENTER}},update={it.setImageBitmap(BitmapFactory.decodeFile(frames[tick%frames.size]))},modifier=Modifier.fillMaxSize())
  else AndroidView(factory={object: android.view.View(it) {override fun onDraw(c: android.graphics.Canvas){ProceduralSprites.fighter(c,width/2f,height*.95f,height*.35f,height*.85f,f.color,State.IDLE,tick,1)}}},update={it.invalidate()},modifier=Modifier.fillMaxSize())
 }
}

@Composable fun CommandPad(command: List<String>,change: (List<String>)->Unit) {
 val labels=mapOf("NW" to "↖","N" to "↑","NE" to "↗","W" to "←","E" to "→","SW" to "↙","S" to "↓","SE" to "↘")
 listOf(listOf("NW","N","NE"),listOf("W","⌫","E"),listOf("SW","S","SE"),listOf("A","B","C","D")).forEach {row -> Row {row.forEach {token -> TextButton({if(token=="⌫") change(command.dropLast(1)) else if(command.size<8) change(command+token)},Modifier.size(48.dp).semantics {contentDescription=token}){Text(labels[token] ?: token)}}}}
}
