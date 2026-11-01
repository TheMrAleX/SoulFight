package com.fightforge.game
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class NetworkMatch(val socket: Socket,val host: Boolean,val own: FighterDef,val store: Storage): Closeable {
 private val input=DataInputStream(BufferedInputStream(socket.getInputStream())); private val output=DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
 val index=if(host) 0 else 1
 init {socket.tcpNoDelay=true; socket.soTimeout=10000}
 private fun writePacket(bytes: ByteArray) {require(bytes.size<=8*1024*1024); output.writeInt(bytes.size); output.write(MessageDigest.getInstance("SHA-256").digest(bytes)); output.write(bytes); output.flush()}
 private fun readPacket(): ByteArray {val length=input.readInt(); require(length in 1..8*1024*1024); val checksum=ByteArray(32); input.readFully(checksum); val bytes=ByteArray(length); input.readFully(bytes); require(MessageDigest.getInstance("SHA-256").digest(bytes).contentEquals(checksum)){"Paquete dañado"}; return bytes}
 fun handshake(): Combat {
  val local=store.json.encodeToString(FighterDef.serializer(),own).toByteArray()
  val remote=if(host) {writePacket(local); readPacket()} else {val bytes=readPacket(); writePacket(local); bytes}
  val other=store.json.decodeFromString(FighterDef.serializer(),remote.toString(Charsets.UTF_8))
  require(other.stats.valid() && other.width in 100..400 && other.height in 200..800 && other.moves.size<=128){"Personaje remoto inválido"}
  return if(host) Combat(own,other) else Combat(other,own)
 }
 private val delayed=ArrayDeque<Input>().apply {repeat(3){add(Input())}}
 @Synchronized fun exchange(local: Input,game: Combat): Pair<Input,Input> {
  delayed.add(local); val sent=delayed.removeFirst()
  output.writeInt(game.tick); output.writeInt(sent.horizontal); output.writeInt(sent.vertical); output.writeInt(sent.buttons)
  if(game.tick%60==0) output.writeLong(game.checksum()); output.flush()
  require(input.readInt()==game.tick){"Desincronización de frames"}
  val other=Input(input.readInt(),input.readInt(),input.readInt()); require(other.horizontal in -1..1 && other.vertical in -1..1 && other.buttons in 0..15){"Entrada remota inválida"}
  if(game.tick%60==0) require(input.readLong()==game.checksum()){"Desincronización del combate"}
  return if(host) sent to other else other to sent
 }
 override fun close() {socket.close()}
}
class LanService(context: Context): Closeable {
 private val manager=context.getSystemService(NsdManager::class.java)
 private var registration: NsdManager.RegistrationListener?=null; private var discovery: NsdManager.DiscoveryListener?=null
 var server: ServerSocket?=null; private set
 fun advertise(name: String): Int {
  val s=ServerSocket(0); server=s
  val listener=object: NsdManager.RegistrationListener {override fun onServiceRegistered(info: NsdServiceInfo){}; override fun onRegistrationFailed(info: NsdServiceInfo,error: Int){}; override fun onServiceUnregistered(info: NsdServiceInfo){}; override fun onUnregistrationFailed(info: NsdServiceInfo,error: Int){}}
  registration=listener; manager.registerService(NsdServiceInfo().apply {serviceName=name; serviceType="_fightforge._tcp."; port=s.localPort},NsdManager.PROTOCOL_DNS_SD,listener); return s.localPort
 }
 fun discover(found: (String,String,Int)->Unit) {
  val listener=object: NsdManager.DiscoveryListener {
   override fun onDiscoveryStarted(type: String){}; override fun onDiscoveryStopped(type: String){}; override fun onStartDiscoveryFailed(type: String,error: Int){}; override fun onStopDiscoveryFailed(type: String,error: Int){}; override fun onServiceLost(info: NsdServiceInfo){}
   override fun onServiceFound(info: NsdServiceInfo) {manager.resolveService(info,object: NsdManager.ResolveListener {override fun onResolveFailed(info: NsdServiceInfo,error: Int){}; override fun onServiceResolved(resolved: NsdServiceInfo) {found(resolved.serviceName,resolved.host.hostAddress ?: return,resolved.port)}})}
  }; discovery=listener; manager.discoverServices("_fightforge._tcp.",NsdManager.PROTOCOL_DNS_SD,listener)
 }
 override fun close() {runCatching {registration?.let {manager.unregisterService(it)}}; runCatching {discovery?.let {manager.stopServiceDiscovery(it)}}; server?.close()}
}
@Composable fun LanScreen(store: Storage,selected: FighterDef,back: ()->Unit,message: (String)->Unit) {
 val scope=rememberCoroutineScope(); val service=remember {LanService(store.context)}
 var address by remember {mutableStateOf("")}; var room by remember {mutableStateOf("Sala Fight Forge")}; var port by remember {mutableStateOf("45454")}; var status by remember {mutableStateOf("Conecta ambos dispositivos a la misma WiFi")}
 var rooms by remember {mutableStateOf(emptyList<Triple<String,String,Int>>())}; var match by remember {mutableStateOf<NetworkMatch?>(null)}; var combat by remember {mutableStateOf<Combat?>(null)}; var paused by remember {mutableStateOf(false)}; val audio=remember {ProceduralAudio(store.context)}
 DisposableEffect(Unit) {service.discover {name,ip,p -> scope.launch {rooms=(rooms.filterNot {it.second==ip && it.third==p}+Triple(name,ip,p))}}; onDispose {service.close(); match?.close(); audio.close()}}
 suspend fun connect(socket: Socket,host: Boolean) {val session=NetworkMatch(socket,host,selected,store); try {val game=withContext(Dispatchers.IO){session.handshake()}; match=session; combat=game; status="Conectado"} catch(e: Exception) {session.close(); status=e.message ?: "Error de conexión"}}
 if(combat!=null && match!=null) {AndroidView(factory={FightView(it,combat!!,Ai(1),store.data.settings,0,audio,{match?.close(); combat=null; status="Partida cancelada"},{winner -> val won=winner==match!!.index; if(won) store.update(store.data.copy(coins=store.data.coins+80)); match?.close(); combat=null; message(if(won) "Victoria LAN · +80 monedas" else "Fin del combate LAN")},match,{error -> scope.launch {match?.close(); combat=null; status=error; message(error)}})},modifier=Modifier.fillMaxSize()); return}
 Frame("MULTIJUGADOR LAN",back) {
  Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text("PERSONAJE: ${selected.name}"); OutlinedTextField(room,{room=it.take(32)},label={Text("NOMBRE DE SALA")}); ForgeButton("CREAR SALA") {scope.launch {runCatching {val p=service.advertise(room); port=p.toString(); status="Puerto $p · esperando rival"; val socket=withContext(Dispatchers.IO){service.server!!.accept()}; connect(socket,true)}.onFailure {status=it.message ?: "No se pudo crear la sala"}}}; Text(status,color=ForgeColors.secondary); Text("Salas encontradas: ${rooms.size}"); rooms.forEach {(name,ip,p) -> TextButton({address=ip; port=p.toString()}){Text("$name · $ip:$p")}} }
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedTextField(address,{address=it},label={Text("IP DEL ANFITRIÓN")}); OutlinedTextField(port,{port=it.filter(Char::isDigit)},label={Text("PUERTO")}); ForgeButton("UNIRSE A SALA") {scope.launch {status="Conectando…"; try {val socket=withContext(Dispatchers.IO){Socket().apply {connect(java.net.InetSocketAddress(address,port.toInt()),5000)}}; connect(socket,false)} catch(e: Exception) {status="No se pudo conectar: ${e.message}"}}} }
  }
 }
}
