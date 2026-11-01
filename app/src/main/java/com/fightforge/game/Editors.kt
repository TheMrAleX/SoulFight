package com.fightforge.game
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
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
 if(move!=null) { MoveEditor(move!!,f,{ m -> val next=f.moves.toMutableList(); if(moveIndex<0) next.add(m) else next[moveIndex]=m; edit(f.copy(moves=next)); move=null },{move=null},message); return }
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
      Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ForgeButton("IMPORTAR",secondary=true) {imports.launch("image/*")}; ForgeButton("FABRICAR",secondary=true) {painting=true}; ForgeButton("DESCARGAR",secondary=true) { runCatching { store.template(f) }.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message(it.message ?: "Error al guardar")} } }
      f.animations[slot]?.forEachIndexed { i,path -> Row(verticalAlignment=Alignment.CenterVertically) { Text("FRAME ${i+1}",Modifier.weight(1f)); TextButton({ val frames=f.animations[slot]!!.toMutableList(); if(i>0) {java.util.Collections.swap(frames,i,i-1); edit(f.copy(animations=f.animations+(slot to frames)))}}) {Text("↑")}; TextButton({edit(f.copy(animations=f.animations+(slot to (f.animations[slot]!!+path))))}){Text("DUPLICAR")}; TextButton({val frames=f.animations[slot]!!.toMutableList(); frames.removeAt(i); edit(f.copy(animations=f.animations+(slot to frames)))}){Text("BORRAR")} } }
      if(section=="ANIMACIONES EXTRA" && slot in slots) ForgeButton("BORRAR ANIMACIÓN",secondary=true) { edit(f.copy(animations=f.animations-slot)); slot="quieto" }
     }
     "MOVIMIENTOS" -> { ForgeButton("CREAR MOVIMIENTO") {moveIndex=-1; move=Move("",emptyList())}; f.moves.forEachIndexed { i,m -> Row(verticalAlignment=Alignment.CenterVertically) { Text("${m.name} · ${m.command.joinToString(" ")}",Modifier.weight(1f)); TextButton({moveIndex=i; move=m}){Text("EDITAR")}; TextButton({edit(f.copy(moves=f.moves.filterIndexed { j,_ -> i!=j }))}){Text("BORRAR")} } }
    }
   }
  }
 }
 if(leave) AlertDialog(onDismissRequest={leave=false},title={Text("CAMBIOS SIN GUARDAR")},text={Text("¿Quieres guardar el personaje?")},confirmButton={TextButton({if(save()) back(); leave=false}){Text("GUARDAR Y SALIR")}},dismissButton={Row {TextButton({leave=false; back()}){Text("SALIR")}; TextButton({leave=false}){Text("NO SALIR")}}})
}
@Composable fun MoveEditor(initial: Move,f: FighterDef,save: (Move)->Unit,back: ()->Unit,message: (String)->Unit) {
 var m by remember {mutableStateOf(initial)}; var boxKind by remember {mutableStateOf(BoxKind.DAMAGE)}; var drawing by remember {mutableStateOf(false)}; var selected by remember {mutableIntStateOf(-1)}
 fun candidate(n: Move) { if(Economy.balance(n,f.stats)<0) message("No hay puntos suficientes: añade inicio o recuperación") else m=n }
 if(drawing) {
  Frame("DIBUJAR HITBOXES",{drawing=false},action={ForgeButton("BORRAR ÚLTIMA",secondary=true){if(m.boxes.isNotEmpty()) m=m.copy(boxes=m.boxes.dropLast(1))}}) {
   Row(horizontalArrangement=Arrangement.spacedBy(4.dp),modifier=Modifier.horizontalScroll(rememberScrollState())) { BoxKind.entries.forEach { kind -> TextButton({boxKind=kind}){Text(kind.name,color=if(kind==boxKind) ForgeColors.gold else ForgeColors.secondary)} } }
   AndroidView(factory={ HitboxView(it,f,m.boxes,{boxKind},{rect ->
    val b=Box(boxKind,rect); candidate(m.copy(boxes=if(boxKind==BoxKind.GRAB) listOf(b) else m.boxes+b))
   }) },update={it.boxes=m.boxes},modifier=Modifier.weight(1f).fillMaxWidth())
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
    m.boxes.forEachIndexed { i,b -> Row { Text("${b.kind} ${b.rect.w}×${b.rect.h}",Modifier.weight(1f)); TextButton({m=m.copy(boxes=m.boxes.filterIndexed { j,_ -> i!=j })}){Text("✕")} } }
   }
   Column(Modifier.weight(.62f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(m.name,{m=m.copy(name=it.take(24))},label={Text("NOMBRE")})
    Row { Kind.entries.forEach {kind -> TextButton({candidate(m.copy(kind=kind))}){Text(kind.name,color=if(m.kind==kind) ForgeColors.gold else ForgeColors.secondary)} } }
    Counter("INICIO",m.startup,1,600) {candidate(m.copy(startup=it))}; Counter("RECUPERACIÓN",m.recovery,1,600){candidate(m.copy(recovery=it))}; Counter("DAÑO EXTRA %",m.extra,-50,100,5){candidate(m.copy(extra=it))}
    Text("ANIMACIÓN: ${m.animation}"); (animationSlots+f.animations.keys.filter {it !in animationSlots}).forEach { name -> TextButton({m=m.copy(animation=name)}){Text(name)} }
    if(m.kind!=Kind.BASIC) { CheckRow("ANTI-AÉREO",m.antiAir){candidate(m.copy(antiAir=it))}; CheckRow("ANTI-SUELO",m.antiGround){candidate(m.copy(antiGround=it))} }
    if(m.kind==Kind.ULTIMATE) {Counter("CONSUMO %",m.powerPercent,50,100,5){candidate(m.copy(powerPercent=it))}; CheckRow("INVULNERABILIDAD",m.invulnerable){candidate(m.copy(invulnerable=it))} }
   }
  }
 }
}
@Composable fun Counter(label: String,value: Int,min: Int,max: Int,step: Int=1,change: (Int)->Unit) { Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) { Text(label,Modifier.weight(1f)); ForgeButton("−",secondary=true){change((value-step).coerceAtLeast(min))}; Text(value.toString()); ForgeButton("+",secondary=true){change((value+step).coerceAtMost(max))} } }
@Composable fun CheckRow(label: String,value: Boolean,change: (Boolean)->Unit) { Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(value,change); Text(label) } }
@Composable fun PixelEditor(store: Storage,f: FighterDef,slot: String,save: (String)->Unit,back: ()->Unit) {
 val geometry=TemplateGeometry.cached(f.width,f.height); val bitmap=remember {Bitmap.createBitmap(if(slot=="foto") 256 else geometry.pixelsWide,if(slot=="foto") 256 else geometry.pixelsHigh,Bitmap.Config.ARGB_8888)}
 var tool by remember {mutableStateOf("LÁPIZ")}; var color by remember {mutableIntStateOf(android.graphics.Color.WHITE)}; var brush by remember {mutableIntStateOf(4)}; var view by remember {mutableStateOf<PixelView?>(null)}
 Frame("FABRICAR · $slot",back,action={ForgeButton("GUARDAR FRAME") {val path=File(store.context.filesDir,"characters/${f.id}/animations/$slot/${UUID.randomUUID()}.png"); path.parentFile!!.mkdirs(); path.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}; save(path.absolutePath)}}) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("LÁPIZ","BORRADOR","RELLENO","LÍNEA","RECTÁNGULO","CUENTAGOTAS").forEach { t -> TextButton({tool=t}){Text(t,color=if(t==tool) ForgeColors.gold else ForgeColors.secondary)} }; TextButton({view?.undo()}){Text("↶")}; TextButton({view?.redo()}){Text("↷")} }
  Row(Modifier.weight(1f)) { AndroidView(factory={PixelView(it,bitmap).also {v -> view=v}},update={it.tool=tool; it.color=color; it.brush=brush; it.picked={c -> color=c}},modifier=Modifier.weight(1f).fillMaxHeight()); Column(Modifier.width(140.dp).verticalScroll(rememberScrollState())) { Counter("PX",brush,1,16){brush=it}; for(c in ProceduralSprites.colors+intArrayOf(android.graphics.Color.WHITE,android.graphics.Color.BLACK)) TextButton({color=c}){Text("████",color=androidx.compose.ui.graphics.Color(c))} } }
 }
}
