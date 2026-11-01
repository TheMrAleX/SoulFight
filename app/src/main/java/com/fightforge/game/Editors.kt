package com.fightforge.game
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
import com.fightforge.engine.State
import java.io.File
import java.util.UUID

val animationSlots=listOf("quieto","caminar adelante","caminar atrás","correr adelante","correr atrás","agacharse","agachado quieto","agacharse→levantarse","salto","caída","correr aire adelante","correr aire atrás","bloqueo parado","bloqueo agachado","bloqueo aire","daño","noqueado","K.O.","tumbado","levantarse","mareado")
@Composable fun CharacterEditor(store: Storage,initial: FighterDef,back: ()->Unit,message: (String)->Unit) {
 var f by remember { mutableStateOf(initial) }; var section by remember { mutableStateOf("NOMBRE") }; var dirty by remember { mutableStateOf(false) }; var leave by remember { mutableStateOf(false) }
 var slot by remember { mutableStateOf("quieto") }; var extra by remember { mutableStateOf("") }; var move by remember { mutableStateOf<Move?>(null) }; var moveIndex by remember { mutableIntStateOf(-1) }; var painting by remember { mutableStateOf(false) }
 fun edit(next: FighterDef) { f=next; dirty=true }
 fun save(): Boolean {
  if(f.name.isBlank() || f.name.length>16 || !f.stats.valid()) { message("Nombre de 1–16 caracteres y exactamente 20 puntos requeridos"); return false }
  if(f.animations["quieto"].isNullOrEmpty() || f.animations["foto"].isNullOrEmpty()) { message("Importa o fabrica una foto de batalla y al menos un frame QUIETO"); return false }
  store.character(f); dirty=false; return true
 }
 fun attemptExit() { if(dirty) leave=true else back() }
 BackHandler { if(move!=null) move=null else attemptExit() }
 val imports=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
  runCatching { val frames=uris.take(if(slot=="foto") 1 else 24).map { store.importImage(it,"characters/${f.id}/animations/$slot") }; if(frames.isNotEmpty()) edit(f.copy(animations=f.animations+(slot to frames))) }.onFailure { message(it.message ?: "Error de importación") }
 }
 if(move!=null) { MoveEditor(store,move!!,f,{ m -> val next=f.moves.toMutableList(); if(moveIndex<0) next.add(m) else next[moveIndex]=m; edit(f.copy(moves=next)); move=null },{move=null},message); return }
 if(painting) { PixelEditor(store,f,slot,{ path -> edit(f.copy(animations=f.animations+(slot to ((f.animations[slot] ?: emptyList())+path)))); painting=false },{painting=false}); return }
 Frame("EDITOR DE PERSONAJE",::attemptExit,action={ForgeButton("GUARDAR") { if(save()) message("Personaje guardado") }}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   LazyColumn(Modifier.weight(.32f),verticalArrangement=Arrangement.spacedBy(8.dp)) { val sections=listOf("NOMBRE","TAMAÑO","ESTADÍSTICAS","FOTO","ANIMACIONES","ANIMACIONES EXTRA","MOVIMIENTOS"); items(sections.size) { i -> ForgeButton(sections[i],Modifier.fillMaxWidth(),section!=sections[i]) { section=sections[i] } } }
   Column(Modifier.weight(.68f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    when(section) {
     "NOMBRE" -> { OutlinedTextField(f.name,{edit(f.copy(name=it.take(16)))},label={Text("NOMBRE")}); OutlinedTextField(f.description,{edit(f.copy(description=it.take(120)))},label={Text("DESCRIPCIÓN")}); Text("CLASE"); Defaults.names.indices.forEach { i -> ForgeButton(listOf("EQUILIBRADO","GRAPPLER","ZONER","RUSHDOWN","TANQUE","ASESINO")[i],secondary=true) {edit(f.copy(stats=Defaults.classes[i]))} } }
     "TAMAÑO" -> { Text("${f.width} × ${f.height} u"); AndroidView(factory={ SizeView(it,f.width,f.height) { w,h -> edit(f.copy(width=w,height=h)) } },modifier=Modifier.fillMaxWidth().height(240.dp)); Text("Arrastra las asas centrales de cada lado. Ancho 100–400 u, alto 200–800 u.",color=ForgeColors.muted) }
     "ESTADÍSTICAS" -> { val values=listOf(f.stats.damage,f.stats.life,f.stats.speed,f.stats.reach); Text("PUNTOS DISPONIBLES: ${20-values.sum()}",color=ForgeColors.gold); listOf("DAÑO","VIDA","VELOCIDAD","ALCANCE").forEachIndexed { i,label -> Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text(label,Modifier.weight(1f)); ForgeButton("−",secondary=true) { if(values[i]>0) {val n=values.toMutableList(); n[i]--; edit(f.copy(stats=Stats(n[0],n[1],n[2],n[3]))) } }; Text(values[i].toString()); ForgeButton("+",secondary=true) { if(values[i]<10 && values.sum()<20) {val n=values.toMutableList(); n[i]++; edit(f.copy(stats=Stats(n[0],n[1],n[2],n[3]))) } } } } }
     "FOTO","ANIMACIONES","ANIMACIONES EXTRA" -> {
      if(section=="FOTO") slot="foto"
      val slots=when(section) { "FOTO" -> listOf("foto"); "ANIMACIONES" -> animationSlots; else -> f.animations.keys.filter { it !in animationSlots && it!="foto" } }
      if(section=="ANIMACIONES EXTRA") { OutlinedTextField(extra,{extra=it},label={Text("NOMBRE DE ANIMACIÓN")}); ForgeButton("AÑADIR ANIMACIÓN") { if(extra.isNotBlank() && extra !in f.animations && extra !in animationSlots) {edit(f.copy(animations=f.animations+(extra to emptyList()))); slot=extra; extra=""} }; if(slots.isEmpty()) Text("Crea una animación extra para asignarla a tus movimientos.") }
      slots.forEach { name -> ForgeButton("$name · ${f.animations[name]?.size ?: 0} FRAMES",Modifier.fillMaxWidth(),slot!=name) {slot=name} }
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ForgeButton("IMPORTAR",secondary=true) {imports.launch("image/*")}; ForgeButton("FABRICAR",secondary=true) {painting=true}; ForgeButton("DESCARGAR",secondary=true) { runCatching { store.template(f,slot) }.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message(it.message ?: "Error al guardar")} } }
      if(slot!="foto") {
       val config=f.animationSettings[slot] ?: AnimationSettings()
       Counter("FPS",config.fps,1,30){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(fps=it))))}
       CheckRow("BUCLE",config.loop){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(loop=it))))}
       if(slot.startsWith("correr")) CheckRow("HABILITADO",config.enabled){edit(f.copy(animationSettings=f.animationSettings+(slot to config.copy(enabled=it))))}
      }
      if(section=="ANIMACIONES EXTRA" && slot in slots) ForgeButton("RENOMBRAR",secondary=true){if(extra.isNotBlank() && extra !in f.animations && extra !in animationSlots) {val old=slot; edit(f.copy(animations=(f.animations-old)+(extra to f.animations.getValue(old)),moves=f.moves.map {if(it.animation==old) it.copy(animation=extra) else it})); slot=extra; extra=""}}
      f.animations[slot]?.forEachIndexed { i,path -> Row(verticalAlignment=Alignment.CenterVertically) { Text("FRAME ${i+1}",Modifier.weight(1f)); TextButton({ val frames=f.animations[slot]!!.toMutableList(); if(i>0) {java.util.Collections.swap(frames,i,i-1); edit(f.copy(animations=f.animations+(slot to frames)))}}) {Text("↑")}; TextButton({edit(f.copy(animations=f.animations+(slot to (f.animations[slot]!!+path))))}){Text("DUPLICAR")}; TextButton({val frames=f.animations[slot]!!.toMutableList(); frames.removeAt(i); edit(f.copy(animations=f.animations+(slot to frames)))}){Text("BORRAR")} } }
      if(section=="ANIMACIONES EXTRA" && slot in slots) ForgeButton("BORRAR ANIMACIÓN",secondary=true) { edit(f.copy(animations=f.animations-slot)); slot="quieto" }
     }
     "MOVIMIENTOS" -> { ForgeButton("CREAR MOVIMIENTO") {moveIndex=-1; move=Move("",emptyList())}; f.moves.forEachIndexed { i,m -> Row(verticalAlignment=Alignment.CenterVertically) { Text("${m.name} · ${m.command.joinToString(" ")}",Modifier.weight(1f)); TextButton({moveIndex=i; move=m}){Text("EDITAR")}; TextButton({edit(f.copy(moves=f.moves.filterIndexed { j,_ -> i!=j }))}){Text("BORRAR")} } } }
    }
   }
  }
 }
 if(leave) AlertDialog(onDismissRequest={leave=false},title={Text("CAMBIOS SIN GUARDAR")},text={Text("¿Quieres guardar el personaje?")},confirmButton={TextButton({if(save()) back(); leave=false}){Text("GUARDAR Y SALIR")}},dismissButton={Row {TextButton({leave=false; back()}){Text("SALIR")}; TextButton({leave=false}){Text("NO SALIR")}}})
}
@Composable fun MoveEditor(store: Storage,initial: Move,f: FighterDef,save: (Move)->Unit,back: ()->Unit,message: (String)->Unit) {
 var m by remember {mutableStateOf(initial)}; var boxKind by remember {mutableStateOf(BoxKind.DAMAGE)}; var drawing by remember {mutableStateOf(false)}; var preview by remember {mutableStateOf(false)}; var selected by remember {mutableIntStateOf(-1)}
 val photoImport=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null && selected in m.boxes.indices) runCatching {val path=store.importImage(uri,"characters/${f.id}/objects"); val list=m.boxes.toMutableList(); list[selected]=list[selected].copy(photo=path); m=m.copy(boxes=list)}.onFailure {message("Imagen no válida")} }
 fun candidate(n: Move) { if(Economy.balance(n,f.stats)<0) message("No hay puntos suficientes: añade inicio o recuperación") else m=n }
 if(preview) {AnimationPreview(f,m.animation){preview=false}; return}
 if(drawing) {
  Frame("DIBUJAR HITBOXES",{drawing=false},action={ForgeButton("BORRAR ÚLTIMA",secondary=true){if(m.boxes.isNotEmpty()) m=m.copy(boxes=m.boxes.dropLast(1))}}) {
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.horizontalScroll(rememberScrollState())) { BoxKind.entries.forEach { kind -> TextButton({boxKind=kind}){Text(kind.name,color=if(kind==boxKind) ForgeColors.gold else ForgeColors.secondary)} } }
   AndroidView(factory={ HitboxView(it,f,m.boxes,{boxKind},{rect ->
    val b=Box(boxKind,rect)
    if(boxKind==BoxKind.TELEPORT && m.boxes.any {it.kind==BoxKind.TELEPORT}) message("Solo se permite un teletransporte")
    else candidate(m.copy(boxes=if(boxKind==BoxKind.GRAB) listOf(b) else m.boxes+b))
   },{index,rect -> val list=m.boxes.toMutableList(); list[index]=list[index].copy(rect=rect); candidate(m.copy(boxes=list))},{index -> selected=index}) },update={it.boxes=m.boxes; it.selected=selected},modifier=Modifier.weight(1f).fillMaxWidth())
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) {ForgeButton("✕ BORRAR SELECCIONADA",enabled=selected in m.boxes.indices) {m=m.copy(boxes=m.boxes.filterIndexed {index,_ -> index!=selected}); selected=-1}}
  }; return
 }
 Frame("MOVIMIENTO · PUNTOS ${Economy.balance(m,f.stats)/1000}",back,action={ForgeButton("GUARDAR") {
  val errors=Economy.errors(m,f.stats,Rect(-f.width/2,-f.height,f.width,f.height)).toMutableList()
  if(f.moves.any {it.name==m.name && it!==initial}) errors.add("Nombre duplicado")
  if(f.moves.any {it.command==m.command && it!==initial}) errors.add("Comando duplicado")
  if(errors.isEmpty()) save(m) else message(errors.joinToString("\n"))
 }}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   Column(Modifier.weight(.38f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Text("COMANDO: ${m.command.joinToString(" → ")}",color=ForgeColors.gold)
    listOf(listOf("NW","N","NE"),listOf("W","S","E"),listOf("SW","A","SE"),listOf("B","C","D")).forEach { row -> Row { row.forEach { token -> TextButton({if(m.command.size<8) m=m.copy(command=m.command+token)}){Text(token)} } } }
    ForgeButton("BORRAR ÚLTIMO",secondary=true) {m=m.copy(command=m.command.dropLast(1))}
    ForgeButton("DIBUJAR HITBOXES") {drawing=true}
    ForgeButton("VER ANIMACIÓN",secondary=true){preview=true}
    ForgeButton("DESCARGAR PLANTILLA",secondary=true){runCatching {store.movementTemplate(f,m)}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}}
    m.boxes.forEachIndexed { i,b -> Row { TextButton({selected=i}) {Text("${b.kind} ${b.rect.w}×${b.rect.h}",color=if(selected==i) ForgeColors.gold else ForgeColors.secondary)}; TextButton({m=m.copy(boxes=m.boxes.filterIndexed { j,_ -> i!=j }); selected=-1}){Text("✕")} } }
   }
   Column(Modifier.weight(.62f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(m.name,{m=m.copy(name=it.take(24))},label={Text("NOMBRE")})
    Row { Kind.entries.forEach {kind -> TextButton({candidate(m.copy(kind=kind))}){Text(kind.name,color=if(m.kind==kind) ForgeColors.gold else ForgeColors.secondary)} } }
    Counter("INICIO",m.startup,1,600) {candidate(m.copy(startup=it))}; Counter("RECUPERACIÓN",m.recovery,1,600){candidate(m.copy(recovery=it))}; Counter("DAÑO EXTRA %",m.extra,-50,100,5){candidate(m.copy(extra=it))}
    if(selected in m.boxes.indices) {
     val box=m.boxes[selected]
     fun updateBox(next: Box) {val list=m.boxes.toMutableList(); list[selected]=next; candidate(m.copy(boxes=list))}
     Text("HITBOX ${selected+1}: ${box.kind}",color=ForgeColors.gold)
     Counter("X",box.rect.x,-800,800,20){updateBox(box.copy(rect=box.rect.copy(x=it)))}
     Counter("Y",box.rect.y,-800,800,20){updateBox(box.copy(rect=box.rect.copy(y=it)))}
     Counter("ANCHO",box.rect.w,20,800,20){updateBox(box.copy(rect=box.rect.copy(w=it)))}
     Counter("ALTO",box.rect.h,20,800,20){updateBox(box.copy(rect=box.rect.copy(h=it)))}
     if(box.kind in listOf(BoxKind.MOVEMENT,BoxKind.TELEPORT)) CheckRow("MOVER AL RIVAL",box.rival){updateBox(box.copy(rival=it))}
     if(box.kind==BoxKind.MOVEMENT) {
      CheckRow("IDA Y VUELTA",box.returnTrip){updateBox(box.copy(returnTrip=it))}
      listOf("player","projectile","solid","ghost").forEach {entity -> TextButton({updateBox(box.copy(entity=entity))}){Text(entity.uppercase(),color=if(entity==box.entity) ForgeColors.gold else ForgeColors.secondary)}}
     }
     if(box.kind in listOf(BoxKind.PROJECTILE,BoxKind.SOLID,BoxKind.GHOST)) {
      ForgeButton("IMPORTAR FOTO (GRATIS)",secondary=true){photoImport.launch("image/*")}
      CheckRow("GRAVEDAD",box.gravity){updateBox(box.copy(gravity=it))}
      Counter("SIMULTÁNEOS",box.slots,1,4){updateBox(box.copy(slots=it))}
      Counter("VELOCIDAD",box.speed,0,1800,100){updateBox(box.copy(speed=it))}
      if(box.kind==BoxKind.PROJECTILE) CheckRow("DIRECCIÓN LIBRE",box.freeDirection){updateBox(box.copy(freeDirection=it))}
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
@Composable fun CheckRow(label: String,value: Boolean,change: (Boolean)->Unit) { Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(value,change); Text(label) } }
@Composable fun PixelEditor(store: Storage,f: FighterDef,slot: String,save: (String)->Unit,back: ()->Unit) {
 val geometry=TemplateGeometry.cached(f.width,f.height); val bitmap=remember {Bitmap.createBitmap(if(slot=="foto") 256 else geometry.pixelsWide,if(slot=="foto") 256 else geometry.pixelsHigh,Bitmap.Config.ARGB_8888)}
 var tool by remember {mutableStateOf("LÁPIZ")}; var color by remember {mutableIntStateOf(android.graphics.Color.WHITE)}; var brush by remember {mutableIntStateOf(4)}; var hue by remember {mutableFloatStateOf(0f)}; var saturation by remember {mutableFloatStateOf(1f)}; var brightness by remember {mutableFloatStateOf(1f)}; var view by remember {mutableStateOf<PixelView?>(null)}
 Frame("FABRICAR · $slot",back,action={ForgeButton("GUARDAR FRAME") {val path=File(store.context.filesDir,"characters/${f.id}/animations/$slot/${UUID.randomUUID()}.png"); path.parentFile!!.mkdirs(); path.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}; save(path.absolutePath)}}) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("LÁPIZ","BORRADOR","RELLENO","LÍNEA","RECTÁNGULO","CUENTAGOTAS").forEach { t -> TextButton({tool=t}){Text(t,color=if(t==tool) ForgeColors.gold else ForgeColors.secondary)} }; TextButton({view?.undo()}){Text("↶")}; TextButton({view?.redo()}){Text("↷")} }
  Row(Modifier.weight(1f)) { AndroidView(factory={PixelView(it,bitmap).also {v -> view=v}},update={it.tool=tool; it.color=color; it.brush=brush; it.picked={c -> color=c}},modifier=Modifier.weight(1f).fillMaxHeight()); Column(Modifier.width(140.dp).verticalScroll(rememberScrollState())) { Counter("PX",brush,1,16){brush=it}; Text("TONO"); Slider(hue,{hue=it; color=android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness))},valueRange=0f..360f); Text("SATURACIÓN"); Slider(saturation,{saturation=it; color=android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness))}); Text("BRILLO"); Slider(brightness,{brightness=it; color=android.graphics.Color.HSVToColor(floatArrayOf(hue,saturation,brightness))}); for(c in (0 until 32).map {android.graphics.Color.HSVToColor(floatArrayOf((it%16)*22.5f,if(it<16) 1f else .4f,if(it%16==0) 0f else 1f))}) TextButton({color=c}){Text("████",color=androidx.compose.ui.graphics.Color(c))} } }
 }
}

@Composable fun AnimationPreview(f: FighterDef,slot: String,back: ()->Unit) {
 val frames=f.animations[slot] ?: emptyList(); var tick by remember {mutableIntStateOf(0)}
 LaunchedEffect(slot) {while(true){delay(1000L/(f.animationSettings[slot]?.fps ?: 12)); tick++}}
 BackHandler {back()}
 Box(Modifier.fillMaxSize().background(ForgeColors.bg).clickable {back()},contentAlignment=Alignment.Center) {
  if(frames.isNotEmpty()) AndroidView(factory={android.widget.ImageView(it).apply {scaleType=android.widget.ImageView.ScaleType.FIT_CENTER}},update={it.setImageBitmap(BitmapFactory.decodeFile(frames[tick%frames.size]))},modifier=Modifier.fillMaxSize())
  else AndroidView(factory={object: android.view.View(it) {override fun onDraw(c: android.graphics.Canvas){ProceduralSprites.fighter(c,width/2f,height*.95f,height*.35f,height*.85f,f.color,State.IDLE,tick,1)}}},update={it.invalidate()},modifier=Modifier.fillMaxSize())
 }
}
