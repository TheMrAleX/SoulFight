package com.fightforge.game

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import com.fightforge.engine.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity: ComponentActivity() {
 private lateinit var audio: ProceduralAudio
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
  window.decorView.systemUiVisibility=android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
  audio=ProceduralAudio(this)
  setContent { ForgeTheme { ForgeApp(remember { Storage(this) },audio) { finish() } } }
 }
 override fun onPause() { super.onPause(); audio.pause() }
 override fun onResume() { super.onResume(); if(::audio.isInitialized) audio.resume() }
 override fun onDestroy() { audio.close(); super.onDestroy() }
}
@Composable fun ForgeButton(label: String,modifier: Modifier=Modifier,secondary: Boolean=false,enabled: Boolean=true,onClick: ()->Unit) {
 Button(onClick,modifier.heightIn(min=48.dp),enabled=enabled,shape=CutCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=if(secondary) ForgeColors.high else ForgeColors.primary),border=BorderStroke(1.dp,if(secondary) ForgeColors.secondary else ForgeColors.text.copy(alpha=.4f))) { Text(label.uppercase()) }
}
@Composable fun Frame(title: String,back: ()->Unit,action: (@Composable ()->Unit)?=null,content: @Composable ColumnScope.()->Unit) {
 Column(Modifier.fillMaxSize().background(ForgeColors.bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal=24.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) { ForgeButton("‹ VOLVER",secondary=true,onClick=back); Text(title.uppercase(),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium); action?.invoke() }
  content()
 }
}
@Composable fun ForgeApp(store: Storage,audio: ProceduralAudio,exit: ()->Unit) {
 var screen by remember { mutableStateOf("splash") }; var data by remember { mutableStateOf(store.data) }; var practice by remember { mutableStateOf(false) }
 var selected by remember { mutableStateOf(store.roster().first()) }; var stage by remember { mutableIntStateOf(0) }; var difficulty by remember { mutableIntStateOf(1) }
 var game by remember { mutableStateOf<Combat?>(null) }; var paused by remember { mutableStateOf(false) }; var movesDialog by remember { mutableStateOf(false) }; var message by remember { mutableStateOf(store.warning ?: "") }
 var editing by remember { mutableStateOf<FighterDef?>(null) }; var delete by remember { mutableStateOf<FighterDef?>(null) }; var purchase by remember { mutableStateOf<FighterDef?>(null) }
 val scope=rememberCoroutineScope(); val context=LocalContext.current
 val appVersion=remember {context.packageManager.getPackageInfo(context.packageName,0).versionName ?: "—"}
 var exportBytes by remember {mutableStateOf<ByteArray?>(null)}
 val exportPackage=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) {uri -> if(uri!=null) runCatching {context.contentResolver.openOutputStream(uri).use {out -> requireNotNull(out); out.write(requireNotNull(exportBytes))}}.onSuccess {message="Paquete exportado"}.onFailure {message="No se pudo exportar"}; exportBytes=null}
 val importPackage=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {uri -> if(uri!=null) scope.launch {runCatching {withContext(Dispatchers.IO) {val bytes=context.contentResolver.openInputStream(uri).use {stream -> requireNotNull(stream); val buffer=java.io.ByteArrayOutputStream(); val chunk=ByteArray(8192); var n=stream.read(chunk); while(n>=0){require(buffer.size()+n<=8*1024*1024); buffer.write(chunk,0,n); n=stream.read(chunk)}; buffer.toByteArray()}; CharacterPackages.import(store,bytes)}}.onSuccess {data=store.data; message="Personaje importado"}.onFailure {message="Paquete inválido: ${it.message}"}}}
 fun sync() { data=store.data }
 fun navigate(next: String) { audio.effect("ui",data.settings.effects); screen=next }
 fun back() { when(screen) { "splash","menu" -> exit(); "fight" -> paused=true; "select","shop","options","creation","single","lan" -> navigate("menu"); "setup" -> navigate("select"); else -> navigate("creation") } }
 BackHandler { back() }
 LaunchedEffect(Unit) { withContext(Dispatchers.IO) { audio.prepare() }; audio.music(false,data.settings.music,data.settings.menuTrack); kotlinx.coroutines.delay(1200); screen="menu" }
 if(screen=="splash") Box(Modifier.fillMaxSize().background(ForgeColors.bg),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("FIGHT FORGE",style=MaterialTheme.typography.titleLarge,color=ForgeColors.primary); Spacer(Modifier.height(24.dp)); CircularProgressIndicator(color=ForgeColors.secondary) } }
 else when(screen) {
  "menu" -> Row(Modifier.fillMaxSize().background(ForgeColors.bg).windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),verticalAlignment=Alignment.CenterVertically) {
   Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("FIGHT",style=MaterialTheme.typography.titleLarge,color=ForgeColors.text); Text("FORGE",style=MaterialTheme.typography.titleLarge,color=ForgeColors.primary); Text("EL COMBATE ES TU CREACIÓN",color=ForgeColors.secondary); Text("v$appVersion · ANDROID",color=ForgeColors.muted) }
   Column(Modifier.width(280.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) { Text("◈ ${data.coins}",color=ForgeColors.gold); listOf("UN JUGADOR" to "single","MULTIJUGADOR" to "lan","TIENDA" to "shop","CREACIÓN" to "creation").forEach { (label,target) -> ForgeButton(label,Modifier.fillMaxWidth()) { navigate(target) } }; Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { ForgeButton("OPCIONES",Modifier.weight(1f),true) { navigate("options") }; ForgeButton("SALIR",secondary=true) { exit() } } }
  }
  "single" -> Frame("UN JUGADOR",::back) { Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) { ForgeButton("CONTRA LA MÁQUINA",Modifier.weight(1f).height(110.dp)) { practice=false; navigate("select") }; ForgeButton("PRÁCTICA",Modifier.weight(1f).height(110.dp),true) { practice=true; navigate("select") } } }
  "select" -> Frame("ELIGE TU LUCHADOR",::back) {
   Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(16.dp)) {
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) { items((store.roster().size+3)/4) { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { store.roster().drop(row*4).take(4).forEach { f -> Column(Modifier.weight(1f).height(86.dp).background(if(f.id==selected.id) ForgeColors.high else ForgeColors.surface,CutCornerShape(10.dp)).clickable { if(store.owned(f)) selected=f else message="Compra este personaje en la tienda" }.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text(if(store.owned(f)) "◆" else "🔒",color=ForgeColors.palette[f.color%6],style=MaterialTheme.typography.titleLarge); Text(f.name) } } } } }
    Column(Modifier.width(240.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(selected.name.uppercase(),style=MaterialTheme.typography.titleMedium); Text(selected.description,color=ForgeColors.secondary); listOf("DAÑO" to selected.stats.damage,"VIDA" to selected.stats.life,"VELOCIDAD" to selected.stats.speed,"ALCANCE" to selected.stats.reach).forEach { (label,value) -> Text("$label $value"); LinearProgressIndicator(progress={value/10f},modifier=Modifier.fillMaxWidth(),color=ForgeColors.secondary) }; Spacer(Modifier.weight(1f)); ForgeButton("CONTINUAR",Modifier.fillMaxWidth()) { navigate("setup") } }
   }
  }
  "setup" -> Frame("PREPARAR COMBATE",::back) { Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) { Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("ESCENARIO",color=ForgeColors.secondary); (listOf("AZOTEA NEÓN","SANTUARIO","PUERTO")+data.stages.map { it.name }).forEachIndexed { i,name -> ForgeButton((if(stage==i) "● " else "")+name,Modifier.fillMaxWidth(),stage!=i) { stage=i } } }; Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("DIFICULTAD",color=ForgeColors.secondary); listOf("FÁCIL","NORMAL","DIFÍCIL").forEachIndexed { i,name -> ForgeButton(name,Modifier.fillMaxWidth(),difficulty!=i) { difficulty=i } }; ForgeButton("PELEAR",Modifier.fillMaxWidth()) { game=Combat(selected,Defaults.roster()[(Defaults.names.indexOf(selected.name)+1).coerceAtLeast(0)%6],practice=practice); paused=false; audio.music(true,data.settings.music,data.settings.fightTrack); navigate("fight") } } } }
  "fight" -> {
   val match=game!!
   if(!paused) AndroidView(factory={ FightView(it,match,Ai(71,difficulty),data.settings,stage,audio,{ paused=true },{ winner -> if(!practice) { val award=if(winner==0) intArrayOf(60,100,150)[difficulty] else 20; store.update(store.data.copy(coins=store.data.coins+award)); sync(); message="${if(winner==0) "VICTORIA" else "DERROTA"} · +$award monedas" }; audio.music(false,data.settings.music,data.settings.menuTrack); screen="menu" }) },modifier=Modifier.fillMaxSize())
   else Frame("PAUSA",{ paused=false }) { Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(24.dp)) { Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) { ForgeButton("CONTINUAR",Modifier.fillMaxWidth()) { paused=false }; ForgeButton("REINICIAR",Modifier.fillMaxWidth(),true) { match.reset(); paused=false }; ForgeButton("LISTA DE MOVIMIENTOS",Modifier.fillMaxWidth(),true) { movesDialog=true }; ForgeButton("SALIR",Modifier.fillMaxWidth(),true) { audio.music(false,data.settings.music,data.settings.menuTrack); screen="menu" } }; if(practice) Column(Modifier.weight(1f)) { Text("MUÑECO"); Row { listOf("PIE","AGACHADO","SALTO","BLOQUEO").forEachIndexed { i,s -> TextButton({match.dummy=i}) { Text(s) } } }; Row(verticalAlignment=Alignment.CenterVertically) { Text("VIDA INFINITA"); Switch(match.infiniteLife,{match.infiniteLife=it}) }; ForgeButton("REINICIAR POSICIONES",secondary=true) { match.fighters[0].x=1000*SCALE; match.fighters[1].x=1600*SCALE } } } }
  }
  "creation" -> Frame("CREACIÓN",::back) { Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically) { listOf("PERSONAJES" to "characters","FONDOS" to "stages","INTERFAZ" to "interface").forEach { (label,target) -> ForgeButton(label,Modifier.weight(1f).height(100.dp)) { navigate(target) } } } }
  "characters" -> Frame("PERSONAJES",::back,action={ Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {ForgeButton("IMPORTAR ZIP",secondary=true){importPackage.launch("application/zip")}; ForgeButton("CREAR") { editing=FighterDef(UUID.randomUUID().toString(),"",custom=true,moves=Defaults.roster()[0].moves); navigate("editor") } } }) { LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(store.roster().size) { i -> val f=store.roster()[i]; Row(Modifier.fillMaxWidth().background(ForgeColors.surface).padding(8.dp),verticalAlignment=Alignment.CenterVertically) { Text(f.name,Modifier.weight(1f)); ForgeButton("ZIP",secondary=true) {runCatching {exportBytes=CharacterPackages.encode(store,f); exportPackage.launch("${f.name}.zip")}.onFailure {message="No se pudo exportar: ${it.message}"}}; ForgeButton("EDITAR",secondary=true) { editing=if(f.custom) f else f.copy(id=UUID.randomUUID().toString(),name=f.name.take(10)+" copia",custom=true); navigate("editor") }; if(f.custom) ForgeButton("BORRAR",secondary=true) { delete=f } } } } }
  "editor" -> CharacterEditor(store,editing!!,{ navigate("characters"); sync() },{ message=it })
  "stages" -> StageEditor(store,{navigate("creation"); sync()},{message=it})
  "interface" -> InterfaceEditor(store,{navigate("creation")},{message=it})
  "shop" -> Frame("TIENDA · ${data.coins} MONEDAS",::back) { LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(6) { i -> val f=Defaults.roster()[i]; Row(Modifier.fillMaxWidth().background(ForgeColors.surface).padding(8.dp),verticalAlignment=Alignment.CenterVertically) { Text("${f.name} · ${f.description}",Modifier.weight(1f)); ForgeButton(if(store.owned(f)) "COMPRADO" else "${Defaults.prices[i]} MONEDAS",enabled=!store.owned(f)) { purchase=f } } } } }
  "options" -> Options(data.settings,{ store.update(store.data.copy(settings=it)); sync() },{ store.update(SaveData()); sync(); message="Datos restablecidos" },::back)
  "lan" -> LanScreen(store,selected,::back,{message=it})
 }
 if(message.isNotEmpty()) AlertDialog(onDismissRequest={message=""},title={Text("FIGHT FORGE")},text={Text(message)},confirmButton={TextButton({message=""}){Text("ACEPTAR")}})
 if(delete!=null) AlertDialog(onDismissRequest={delete=null},title={Text("¿BORRAR ${delete!!.name}?")},text={Text("Se borrará el personaje creado.")},confirmButton={TextButton({store.update(store.data.copy(characters=store.data.characters.filterNot { it.id==delete!!.id })); sync(); delete=null}){Text("BORRAR")}},dismissButton={TextButton({delete=null}){Text("CANCELAR")}})
 if(purchase!=null) AlertDialog(onDismissRequest={purchase=null},title={Text("COMPRAR ${purchase!!.name}")},text={Text("Precio: ${Defaults.prices[Defaults.names.indexOf(purchase!!.name)]} monedas")},confirmButton={TextButton({if(!store.buy(purchase!!)) message="No tienes suficientes monedas"; sync(); purchase=null}){Text("COMPRAR")}},dismissButton={TextButton({purchase=null}){Text("CANCELAR")}})
 if(movesDialog) AlertDialog(onDismissRequest={movesDialog=false},title={Text("MOVIMIENTOS")},text={Column { selected.moves.forEach { Text("${it.name}: ${it.command.joinToString(" → ")}") } }},confirmButton={TextButton({movesDialog=false}){Text("CERRAR")}})
}
@Composable fun Options(settings: Settings,change: (Settings)->Unit,reset: ()->Unit,back: ()->Unit) {
 var confirm by remember { mutableIntStateOf(0) }
 Frame("OPCIONES",back) { Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) { Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Text("MÚSICA ${settings.music}%"); Slider(settings.music.toFloat(),{change(settings.copy(music=it.toInt()))},valueRange=0f..100f); Text("EFECTOS ${settings.effects}%"); Slider(settings.effects.toFloat(),{change(settings.copy(effects=it.toInt()))},valueRange=0f..100f); Text("CONTROLES ${settings.opacity}%"); Slider(settings.opacity.toFloat(),{change(settings.copy(opacity=it.toInt()))},valueRange=30f..100f) }; Column(Modifier.weight(1f)) { Row(verticalAlignment=Alignment.CenterVertically) { Switch(settings.vibration,{change(settings.copy(vibration=it))}); Text("VIBRACIÓN") }; Row(verticalAlignment=Alignment.CenterVertically) { Switch(settings.hitboxes,{change(settings.copy(hitboxes=it))}); Text("HITBOXES") }; Row(verticalAlignment=Alignment.CenterVertically) { Switch(settings.inputs,{change(settings.copy(inputs=it))}); Text("ENTRADAS") }; ForgeButton("RESTABLECER DATOS",secondary=true) { confirm=1 } } } }
 if(confirm>0) AlertDialog(onDismissRequest={confirm=0},title={Text(if(confirm==1) "¿RESTABLECER DATOS?" else "CONFIRMACIÓN FINAL")},text={Text("Se borrarán monedas, compras y creaciones.")},confirmButton={TextButton({if(confirm==1) confirm=2 else {reset(); confirm=0}}){Text("RESTABLECER")}},dismissButton={TextButton({confirm=0}){Text("CANCELAR")}})
}
