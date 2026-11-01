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
@Composable fun StageEditor(store: Storage,back: ()->Unit,message: (String)->Unit) {
 var name by remember {mutableStateOf("")}; var fps by remember {mutableIntStateOf(12)}; var frames by remember {mutableStateOf(emptyList<String>())}; var id by remember {mutableStateOf(UUID.randomUUID().toString())}; var revision by remember {mutableIntStateOf(0)}
 val imports=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris -> runCatching {frames=uris.take(12).map {store.importImage(it,"stages/$id")}}.onFailure {message(it.message ?: "Imagen no válida")} }
 Frame("FONDOS",back,action={ForgeButton("GUARDAR") {if(name.isBlank() || frames.isEmpty()) message("Nombre y al menos una imagen requeridos") else {val s=StageDef(id,name,frames,fps); store.update(store.data.copy(stages=store.data.stages.filterNot {it.id==id}+s)); revision++; message("Fondo guardado")}}}) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   Column(Modifier.weight(.35f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {ForgeButton("CREAR") {id=UUID.randomUUID().toString(); name=""; frames=emptyList()}; if(store.data.stages.isEmpty()) Text("Importa de 1 a 12 imágenes para crear un escenario."); store.data.stages.forEach {s -> Row {TextButton({id=s.id; name=s.name; frames=s.frames; fps=s.fps}){Text(s.name)}; TextButton({store.update(store.data.copy(stages=store.data.stages.filterNot {it.id==s.id})); revision++}){Text("BORRAR")} } } }
   Column(Modifier.weight(.65f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text("BIBLIOTECA · $revision"); OutlinedTextField(name,{name=it.take(32)},label={Text("NOMBRE")}); Counter("FPS",fps,1,30){fps=it}; Text("${frames.size} FRAMES · ${if(frames.size>1) "ANIMADO" else "ESTÁTICO"}"); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {ForgeButton("IMPORTAR",secondary=true){imports.launch("image/*")}; ForgeButton("DESCARGAR PLANTILLA",secondary=true){runCatching {val b=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888); store.exportPng("fondo_lienzo.png",b); b.recycle()}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}}}; if(frames.isNotEmpty()) AndroidView(factory={ImageView(it).apply {scaleType=ImageView.ScaleType.FIT_CENTER}},update={it.setImageBitmap(BitmapFactory.decodeFile(frames.first()))},modifier=Modifier.fillMaxWidth().height(140.dp)) }
  }
 }
}
@Composable fun InterfaceEditor(store: Storage,back: ()->Unit,message: (String)->Unit) {
 var tab by remember {mutableStateOf("INTERFAZ")}; var target by remember {mutableStateOf("Joystick_fondo")}; var revision by remember {mutableIntStateOf(0)}; var name by remember {mutableStateOf("")}; var selectedFile by remember {mutableStateOf<File?>(null)}
 val controls=mapOf("Barra_vida" to (800 to 64),"Barra_poder" to (800 to 32),"Joystick_fondo" to (280 to 280),"Joystick_mando" to (128 to 128),"Boton_A" to (136 to 136),"Boton_B" to (136 to 136),"Boton_C" to (136 to 136),"Boton_D" to (136 to 136))
 val image=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) runCatching {val path=store.importImage(uri,"ui"); val file=File(store.context.filesDir,"ui/$target.png"); File(path).copyTo(file,true); revision++}.onFailure {message("No se pudo importar")} }
 val music=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) runCatching {val dir=File(store.context.filesDir,"music").apply {mkdirs()}; val file=File(dir,"${name.ifBlank {"Pista"}.replace(Regex("[^A-Za-z0-9_-]"),"_")}.audio"); store.context.contentResolver.openInputStream(uri).use {input -> requireNotNull(input); file.outputStream().use {input.copyTo(it)}}; revision++}.onFailure {message("No se pudo importar audio")} }
 val effects=rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {uris -> runCatching {uris.take(12).forEach {store.importImage(it,"effects/${name.ifBlank {"Efecto"}.replace(Regex("[^A-Za-z0-9_-]"),"_")}")}; revision++}.onFailure {message("No se pudo importar el efecto")} }
 var player by remember {mutableStateOf<android.media.MediaPlayer?>(null)}
 DisposableEffect(Unit) {onDispose {player?.release()}}
 Frame("MÚSICA · INTERFAZ · EFECTOS",back) {
  Row {listOf("MÚSICA","INTERFAZ","EFECTOS").forEach {t -> TextButton({tab=t}){Text(t,color=if(t==tab) ForgeColors.gold else ForgeColors.secondary)}}}
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
   Column(Modifier.weight(.4f).verticalScroll(rememberScrollState())) {
    if(tab=="INTERFAZ") controls.keys.forEach {key -> TextButton({target=key}){Text(key,color=if(target==key) ForgeColors.gold else ForgeColors.text)}}
    else { Text("ARCHIVOS · $revision"); val dir=File(store.context.filesDir,if(tab=="MÚSICA") "music" else "effects"); val files=dir.listFiles()?.toList() ?: emptyList(); if(files.isEmpty()) Text("No hay contenido importado."); files.forEach {f -> Row {TextButton({selectedFile=f; name=f.nameWithoutExtension}){Text(f.name)}; TextButton({f.deleteRecursively(); revision++}){Text("BORRAR")}; if(tab=="MÚSICA") TextButton({runCatching {player?.release(); player=android.media.MediaPlayer().apply {setDataSource(f.absolutePath); prepare(); start()}}.onFailure {message("Audio incompatible")}}){Text("▶")} } } }
   }
   Column(Modifier.weight(.6f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    if(tab=="INTERFAZ") {
     Text(target); val size=controls.getValue(target); Text("${size.first} × ${size.second} px")
     ForgeButton("DESCARGAR PLANTILLA",secondary=true){runCatching {val b=Bitmap.createBitmap(size.first,size.second,Bitmap.Config.ARGB_8888); store.exportPng("${target}_lienzo.png",b); b.recycle()}.onSuccess {message("Plantilla guardada en la galería")}.onFailure {message("No se pudo exportar")}}
     ForgeButton("IMPORTAR",secondary=true){image.launch("image/*")}; ForgeButton("RESTABLECER",secondary=true){File(store.context.filesDir,"ui/$target.png").delete(); revision++}
     val path=File(store.context.filesDir,"ui/$target.png"); if(path.exists()) AndroidView(factory={ImageView(it)},update={it.setImageBitmap(BitmapFactory.decodeFile(path.absolutePath))},modifier=Modifier.height(100.dp).fillMaxWidth())
    } else {OutlinedTextField(name,{name=it.take(32)},label={Text("NOMBRE")}); ForgeButton("IMPORTAR") {if(tab=="MÚSICA") music.launch("audio/*") else effects.launch("image/*")}; if(tab=="MÚSICA") {
     ForgeButton("PARAR",secondary=true){player?.pause()}
     selectedFile?.let {file ->
      ForgeButton("ASIGNAR AL MENÚ",secondary=true){store.update(store.data.copy(settings=store.data.settings.copy(menuTrack=file.absolutePath))); message("Música del menú actualizada")}
      ForgeButton("ASIGNAR AL COMBATE",secondary=true){store.update(store.data.copy(settings=store.data.settings.copy(fightTrack=file.absolutePath))); message("Música de combate actualizada")}
      ForgeButton("RENOMBRAR",secondary=true){val dest=File(file.parentFile,"${name.replace(Regex("[^A-Za-z0-9_-]"),"_")}.audio"); if(name.isNotBlank() && !dest.exists() && file.renameTo(dest)) {selectedFile=dest; revision++}}
     }
    } }
   }
  }
 }
}
