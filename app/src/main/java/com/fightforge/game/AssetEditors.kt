package com.fightforge.game

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable fun StageEditor(store: Storage,back: ()->Unit,message: (String)->Unit) {
 val scope=rememberCoroutineScope()
 var stage by remember {mutableStateOf(StageDef(UUID.randomUUID().toString(),"",emptyList()))}
 var request by remember {mutableStateOf<ImportRequest?>(null)}
 var deleting by remember {mutableStateOf<StageDef?>(null)}
 var tick by remember {mutableIntStateOf(0)}
 LaunchedEffect(stage.fps,stage.frames) {while(true){delay(1000L/stage.fps); tick++}}
 val images=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris -> if(uris.isNotEmpty()) request=ImportRequest(uris.take(12),"stages/${stage.id}",1920,1080){stage=stage.copy(frames=it)}}
 val audio=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) scope.launch {runCatching {val path=withContext(Dispatchers.IO){store.importAudio(uri,"stages/${stage.id}")}; stage=stage.copy(music=path)}.onFailure {message(it.message ?: "Audio no válido")}}}
 Frame("FONDOS",back,action={ForgeButton("GUARDAR"){if(stage.name.isBlank() || stage.frames.isEmpty()) message("Nombre y al menos una imagen requeridos") else {store.update(store.data.copy(stages=store.data.stages.filterNot {it.id==stage.id}+stage)); message("Fondo guardado")}}}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   Column(Modifier.weight(.35f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    ForgeButton("CREAR"){stage=StageDef(UUID.randomUUID().toString(),"",emptyList())}
    if(store.data.stages.isEmpty()) Text("Crea un fondo estático o una animación de hasta 12 frames.")
    store.data.stages.forEach {s -> Row {TextButton({stage=s}){Text(s.name)}; TextButton({deleting=s}){Text("BORRAR")}} }
   }
   Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    OutlinedTextField(stage.name,{stage=stage.copy(name=it.take(32))},label={Text("NOMBRE")})
    Counter("FPS",stage.fps,1,30){stage=stage.copy(fps=it)}
    Row(Modifier.horizontalScroll(rememberScrollState())) {ForgeButton("IMPORTAR",secondary=true){images.launch("image/png")}; ForgeButton("DESCARGAR PLANTILLA",secondary=true){runCatching {val b=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888); store.exportPng("fondo_lienzo.png",b); b.recycle()}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}}}
    if(stage.frames.isNotEmpty()) FrameImage(stage.frames[tick%stage.frames.size],Modifier.fillMaxWidth().height(120.dp))
    Row(Modifier.horizontalScroll(rememberScrollState())) {stage.frames.forEachIndexed {i,_ -> TextButton({stage=stage.copy(frames=stage.frames.filterIndexed {j,_ -> j!=i})}){Text("✕ ${i+1}")}}}
    ForgeButton("IMPORTAR MÚSICA",secondary=true){audio.launch("audio/*")}
    if(stage.music.isNotBlank()) ForgeButton("QUITAR MÚSICA",secondary=true){stage=stage.copy(music="")}
   }
  }
 }
 ImageImportDialog(store,request,{request=null},message)
 deleting?.let {s -> AlertDialog(onDismissRequest={deleting=null},title={Text("¿BORRAR ${s.name}?")},confirmButton={TextButton({store.update(store.data.copy(stages=store.data.stages.filterNot {it.id==s.id})); deleting=null}){Text("BORRAR")}},dismissButton={TextButton({deleting=null}){Text("CANCELAR")}})}
}
@Composable fun FrameImage(path: String,modifier: Modifier) {
 val bitmap=remember(path) {BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {inSampleSize=2})}
 DisposableEffect(bitmap){onDispose {bitmap?.recycle()}}
 AndroidView(factory={ImageView(it).apply {scaleType=ImageView.ScaleType.FIT_CENTER}},update={it.setImageBitmap(bitmap)},modifier=modifier)
}
@Composable fun InterfaceEditor(store: Storage,back: ()->Unit,message: (String)->Unit) {
 var tab by remember {mutableStateOf("INTERFAZ")}
 Frame("BIBLIOTECA",back) {
  Row {listOf("MÚSICA","INTERFAZ","EFECTOS").forEach {t -> TextButton({tab=t}){Text(t,color=if(t==tab) ForgeColors.gold else ForgeColors.secondary)}}}
  Box(Modifier.weight(1f)) {when(tab){"MÚSICA" -> MusicLibrary(store,message); "EFECTOS" -> EffectLibrary(store,message); else -> SkinLibrary(store,message)}}
 }
}
@Composable fun MusicLibrary(store: Storage,message: (String)->Unit) {
 val scope=rememberCoroutineScope()
 var name by remember {mutableStateOf("")}; var selected by remember {mutableStateOf<File?>(null)}; var revision by remember {mutableIntStateOf(0)}
 var player by remember {mutableStateOf<android.media.MediaPlayer?>(null)}
 DisposableEffect(Unit){onDispose {player?.release()}}
 val audio=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) scope.launch {runCatching {selected=withContext(Dispatchers.IO){File(store.importAudio(uri,"music"))}; name="PISTA ${revision+1}"; revision++}.onFailure {message(it.message ?: "Audio incompatible")}}}
 val files=remember(revision){File(store.context.filesDir,"music").listFiles()?.filter {it.extension=="audio"}?.sortedBy {it.name}.orEmpty()}
 Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  Column(Modifier.weight(.35f).verticalScroll(rememberScrollState())) {
   ForgeButton("IMPORTAR"){audio.launch("audio/*")}; if(files.isEmpty()) Text("No hay pistas importadas.")
   files.forEach {f -> TextButton({selected=f; name=f.nameWithoutExtension}){Text(f.nameWithoutExtension)}}
  }
  Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {selected?.let {file ->
   OutlinedTextField(name,{name=it.take(32)},label={Text("NOMBRE")})
   Row {TextButton({runCatching {player?.release(); player=android.media.MediaPlayer().apply {setDataSource(file.absolutePath); prepare(); start()}}.onFailure {message("Audio incompatible")}}){Text("REPRODUCIR")}; TextButton({player?.pause()}){Text("PARAR")}}
   ForgeButton("RENOMBRAR",secondary=true){val dest=File(file.parentFile,"${name.replace(Regex("[^A-Za-z0-9 _-]"),"_")}.audio"); if(name.isNotBlank() && (dest==file || !dest.exists()) && file.renameTo(dest)) {val s=store.data.settings; store.update(store.data.copy(settings=s.copy(menuTrack=if(s.menuTrack==file.absolutePath) dest.absolutePath else s.menuTrack,fightTrack=if(s.fightTrack==file.absolutePath) dest.absolutePath else s.fightTrack),stages=store.data.stages.map {if(it.music==file.absolutePath) it.copy(music=dest.absolutePath) else it})); selected=dest; revision++} else message("Nombre no válido o ya existente")}
   ForgeButton("ASIGNAR AL MENÚ",secondary=true){store.update(store.data.copy(settings=store.data.settings.copy(menuTrack=file.absolutePath))); message("Música del menú asignada")}
   ForgeButton("ASIGNAR AL COMBATE",secondary=true){store.update(store.data.copy(settings=store.data.settings.copy(fightTrack=file.absolutePath))); message("Música de combate asignada")}
   store.data.stages.forEach {s -> TextButton({store.update(store.data.copy(stages=store.data.stages.map {if(it.id==s.id) it.copy(music=file.absolutePath) else it})); message("Música asignada a ${s.name}")}){Text("ASIGNAR A ${s.name}")}}
   ForgeButton("BORRAR",secondary=true){player?.release(); player=null; file.delete(); val settings=store.data.settings; store.update(store.data.copy(settings=settings.copy(menuTrack=if(settings.menuTrack==file.absolutePath) "" else settings.menuTrack,fightTrack=if(settings.fightTrack==file.absolutePath) "" else settings.fightTrack),stages=store.data.stages.map {if(it.music==file.absolutePath) it.copy(music="") else it})); selected=null; revision++}
  } ?: Text("Selecciona una pista para reproducir, renombrar o asignar.")}
 }
}
@Composable fun EffectLibrary(store: Storage,message: (String)->Unit) {
 val scope=rememberCoroutineScope()
 var effect by remember {mutableStateOf(EffectDef(UUID.randomUUID().toString(),"",emptyList()))}
 var request by remember {mutableStateOf<ImportRequest?>(null)}; var tick by remember {mutableIntStateOf(0)}
 LaunchedEffect(effect.fps,effect.frames){while(true){delay(1000L/effect.fps); tick++}}
 val images=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris -> if(uris.isNotEmpty()) request=ImportRequest(uris.take(12),"effects/${effect.id}",256,256){effect=effect.copy(frames=it)}}
 val audio=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) scope.launch {runCatching {val path=withContext(Dispatchers.IO){store.importAudio(uri,"effects/${effect.id}")}; effect=effect.copy(sound=path)}.onFailure {message(it.message ?: "Audio incompatible")}}}
 Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  Column(Modifier.weight(.35f).verticalScroll(rememberScrollState())) {ForgeButton("CREAR EFECTO"){effect=EffectDef(UUID.randomUUID().toString(),"",emptyList())}; Text("INCLUIDOS: CHISPAS, IMPACTO Y AURA"); store.data.effectsLibrary.forEach {e -> TextButton({effect=e}){Text(e.name)}}}
  Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   OutlinedTextField(effect.name,{effect=effect.copy(name=it.take(32))},label={Text("NOMBRE")})
   Counter("FPS",effect.fps,1,30){effect=effect.copy(fps=it)}; Counter("ESCALA %",effect.scale,25,400,25){effect=effect.copy(scale=it)}
   if(effect.frames.isNotEmpty()) FrameImage(effect.frames[tick%effect.frames.size],Modifier.height(80.dp).fillMaxWidth())
   Row(Modifier.horizontalScroll(rememberScrollState())) {ForgeButton("IMPORTAR FRAMES",secondary=true){images.launch("image/png")}; ForgeButton("SONIDO",secondary=true){audio.launch("audio/*")}; ForgeButton("PLANTILLA",secondary=true){val b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888); runCatching {store.exportPng("efecto_lienzo.png",b)}.onFailure {message("No se pudo exportar")}; b.recycle()}}
   if(effect.sound.isNotEmpty()) TextButton({effect=effect.copy(sound="")}){Text("QUITAR SONIDO")}
   ForgeButton("GUARDAR"){runCatching {store.effect(effect)}.onSuccess {message("Efecto guardado")}.onFailure {message("Nombre y 1–12 frames requeridos")}}
   if(store.data.effectsLibrary.any {it.id==effect.id}) ForgeButton("BORRAR",secondary=true){store.update(store.data.copy(effectsLibrary=store.data.effectsLibrary.filterNot {it.id==effect.id},characters=store.data.characters.map {it.copy(hitEffect=if(it.hitEffect==effect.id) "sparks" else it.hitEffect,hurtEffect=if(it.hurtEffect==effect.id) "sparks" else it.hurtEffect)})); effect=EffectDef(UUID.randomUUID().toString(),"",emptyList())}
  }
 }
 ImageImportDialog(store,request,{request=null},message)
}
@Composable fun SkinLibrary(store: Storage,message: (String)->Unit) {
 var target by remember {mutableStateOf("Joystick_fondo")}; var revision by remember {mutableIntStateOf(0)}; var request by remember {mutableStateOf<ImportRequest?>(null)}
 val controls=mapOf("Barra_vida" to (800 to 64),"Barra_poder" to (800 to 32),"Joystick_fondo" to (280 to 280),"Joystick_mando" to (128 to 128),"Boton_A" to (136 to 136),"Boton_B" to (136 to 136),"Boton_C" to (136 to 136),"Boton_D" to (136 to 136))
 val image=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) {val selected=target; val size=controls.getValue(selected); request=ImportRequest(listOf(uri),"ui/imports",size.first,size.second){paths -> File(paths.first()).copyTo(File(store.context.filesDir,"ui/$selected.png"),true); revision++}}}
 Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
  Column(Modifier.weight(.35f).verticalScroll(rememberScrollState())){controls.keys.forEach {key -> TextButton({target=key}){Text(key,color=if(key==target) ForgeColors.gold else ForgeColors.text)}}}
  Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("$target · ${controls.getValue(target).first} × ${controls.getValue(target).second} px")
   Row(Modifier.horizontalScroll(rememberScrollState())) {ForgeButton("PLANTILLA",secondary=true){val size=controls.getValue(target); val b=Bitmap.createBitmap(size.first,size.second,Bitmap.Config.ARGB_8888); runCatching {store.exportPng("${target}_lienzo.png",b)}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}; b.recycle()}; ForgeButton("IMPORTAR",secondary=true){image.launch("image/png")}; ForgeButton("RESTABLECER",secondary=true){File(store.context.filesDir,"ui/$target.png").delete(); revision++}}
   key(revision){AndroidView(factory={HudPreview(it,store)},modifier=Modifier.fillMaxWidth().height(180.dp))}
  }
 }
 ImageImportDialog(store,request,{request=null},message)
}
