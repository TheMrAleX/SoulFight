package com.fightforge.game
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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

class NetworkMatch(val socket: Socket,val host: Boolean,val own: FighterDef,val store: Storage,val chosenStage: Int=0): Closeable {
 private val input=DataInputStream(BufferedInputStream(socket.getInputStream())); private val output=DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
 var stageDef: StageDef?=null; private set
 var stageIndex=chosenStage; private set
 var sentAssetBytes=0L; private set
 val index=if(host) 0 else 1
 init {socket.tcpNoDelay=true; socket.soTimeout=10000}
 private fun writePacket(bytes: ByteArray) {require(bytes.size<=8*1024*1024); sentAssetBytes+=bytes.size; output.writeInt(bytes.size); output.write(MessageDigest.getInstance("SHA-256").digest(bytes)); output.write(bytes); output.flush()}
 private fun readPacket(): ByteArray {val length=input.readInt(); require(length in 1..8*1024*1024); val checksum=ByteArray(32); input.readFully(checksum); val bytes=ByteArray(length); input.readFully(bytes); require(MessageDigest.getInstance("SHA-256").digest(bytes).contentEquals(checksum)){"Paquete dañado"}; return bytes}
 private fun sendCharacter(){sendCached(CharacterPackages.encode(store,own))}
 private fun receiveCharacter(): FighterDef=CharacterPackages.decode(store,receiveCached())
 private fun sendCached(bytes: ByteArray) {
  val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
  output.writeUTF(hash); output.flush(); if(!input.readBoolean()) writePacket(bytes)
 }
 private fun receiveCached(): ByteArray {
  val hash=input.readUTF(); require(hash.matches(Regex("[a-f0-9]{64}")))
  val cache=File(store.context.cacheDir,"network/$hash.zip"); val exists=cache.isFile && MessageDigest.getInstance("SHA-256").digest(cache.readBytes()).joinToString(""){"%02x".format(it)}==hash
  output.writeBoolean(exists); output.flush(); val bytes=if(exists) cache.readBytes() else readPacket()
  require(MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}==hash)
  if(!exists){cache.parentFile!!.mkdirs(); cache.writeBytes(bytes)}; return bytes
 }
 fun handshake(): Combat {
  output.writeInt(0x46464732); output.flush(); require(input.readInt()==0x46464732){"Versiones de juego incompatibles"}
  val other=if(host) {sendCharacter(); receiveCharacter()} else {val f=receiveCharacter(); sendCharacter(); f}
  if(host) {stageDef=store.data.stages.getOrNull(chosenStage-3); stageIndex=if(stageDef==null) chosenStage.coerceIn(0,2) else chosenStage; output.writeInt(stageIndex); output.flush(); stageDef?.let {sendCached(StagePackages.encode(store,it))}}
  else {stageIndex=input.readInt(); require(stageIndex in 0..10000); if(stageIndex>=3) stageDef=StagePackages.decode(store,receiveCached())}
  return if(host) Combat(own,other) else Combat(other,own)
 }
 @Volatile var nextRoundRequested=false
 private val delayed=InputDelay()
 @Synchronized fun exchange(local: Input,game: Combat): Pair<Input,Input> {
  val sent=delayed.advance(local)
  output.writeInt(game.tick); output.writeInt(sent.horizontal); output.writeInt(sent.vertical); output.writeInt(sent.buttons); output.writeBoolean(nextRoundRequested)
  if(game.tick%60==0) output.writeLong(game.checksum()); output.flush()
  require(input.readInt()==game.tick){"Desincronización de frames"}
  val other=Input(input.readInt(),input.readInt(),input.readInt()); require(other.horizontal in -1..1 && other.vertical in -1..1 && other.buttons in 0..15){"Entrada remota inválida"}
  val remoteNext=input.readBoolean()
  if(game.tick%60==0) require(input.readLong()==game.checksum()){"Desincronización del combate"}
  if(game.roundOver && !game.matchOver && (nextRoundRequested || remoteNext)) {game.nextRound(); nextRoundRequested=false}
  return if(host) sent to other else other to sent
 }
 override fun close() {socket.close()}
}
class LanService(context: Context,val status: (String)->Unit): Closeable {
 private val manager=context.getSystemService(NsdManager::class.java)
 private var registration: NsdManager.RegistrationListener?=null
 private var discovery: NsdManager.DiscoveryListener?=null
 var server: ServerSocket?=null; private set
 fun stopAdvertising(){registration?.let {runCatching {manager.unregisterService(it)}}; registration=null; server?.close(); server=null}
 fun advertise(name: String): Int {
  stopAdvertising(); val socket=ServerSocket(0); server=socket
  val listener=object: NsdManager.RegistrationListener {
   override fun onServiceRegistered(info: NsdServiceInfo){status("Sala ${info.serviceName} anunciada")}
   override fun onRegistrationFailed(info: NsdServiceInfo,error: Int){status("No se pudo anunciar la sala ($error); usa la IP manual")}
   override fun onServiceUnregistered(info: NsdServiceInfo){status("Sala cerrada")}
   override fun onUnregistrationFailed(info: NsdServiceInfo,error: Int){status("No se pudo retirar el anuncio ($error)")}
  }
  registration=listener; manager.registerService(NsdServiceInfo().apply {serviceName=name; serviceType="_fightforge._tcp."; port=socket.localPort},NsdManager.PROTOCOL_DNS_SD,listener); return socket.localPort
 }
 fun discover(found: (String,String,Int)->Unit,lost: (String)->Unit) {
  val listener=object: NsdManager.DiscoveryListener {
   override fun onDiscoveryStarted(type: String){status("Buscando salas WiFi…")}
   override fun onDiscoveryStopped(type: String){status("Búsqueda detenida")}
   override fun onStartDiscoveryFailed(type: String,error: Int){status("Descubrimiento no disponible ($error); usa IP o código")}
   override fun onStopDiscoveryFailed(type: String,error: Int){status("Error al detener búsqueda ($error)")}
   override fun onServiceLost(info: NsdServiceInfo){lost(info.serviceName)}
   override fun onServiceFound(info: NsdServiceInfo){manager.resolveService(info,object: NsdManager.ResolveListener {
    override fun onResolveFailed(info: NsdServiceInfo,error: Int){status("No se pudo resolver ${info.serviceName} ($error)")}
    override fun onServiceResolved(resolved: NsdServiceInfo){resolved.host.hostAddress?.let {found(resolved.serviceName,it,resolved.port)}}
   })}
  }
  discovery=listener; manager.discoverServices("_fightforge._tcp.",NsdManager.PROTOCOL_DNS_SD,listener)
 }
 override fun close(){stopAdvertising(); discovery?.let {runCatching {manager.stopServiceDiscovery(it)}}; discovery=null}
 fun addresses(): List<String> = java.net.NetworkInterface.getNetworkInterfaces().toList().flatMap {it.inetAddresses.toList()}.filter {it is java.net.Inet4Address && !it.isLoopbackAddress && !it.isLinkLocalAddress}.mapNotNull {it.hostAddress}
}
@Composable fun LanScreen(store: Storage,selected: FighterDef,back: ()->Unit,message: (String)->Unit) {
 val scope=rememberCoroutineScope()
 var status by remember {mutableStateOf("Conecta ambos dispositivos a la misma WiFi")}
 val service=remember {LanService(store.context){text -> scope.launch {status=text}}}
 var fighter by remember {mutableStateOf(selected)}; var stage by remember {mutableIntStateOf(0)}
 var address by remember {mutableStateOf("")}; var room by remember {mutableStateOf("Sala Fight Forge")}; var port by remember {mutableStateOf("45454")}; var code by remember {mutableStateOf("")}; var busy by remember {mutableStateOf(false)}
 var rooms by remember {mutableStateOf(emptyList<Triple<String,String,Int>>())}; var match by remember {mutableStateOf<NetworkMatch?>(null)}; var combat by remember {mutableStateOf<Combat?>(null)}
 val audio=remember {ProceduralAudio(store.context)}
 DisposableEffect(Unit){service.discover({name,ip,p -> scope.launch {rooms=rooms.filterNot {it.first==name}+Triple(name,ip,p)}},{name -> scope.launch {rooms=rooms.filterNot {it.first==name}}}); onDispose {service.close(); match?.close(); audio.close()}}
 LaunchedEffect(Unit){withContext(Dispatchers.IO){audio.prepare()}}
 suspend fun connect(socket: Socket,host: Boolean){val session=NetworkMatch(socket,host,fighter,store,stage); try {val game=withContext(Dispatchers.IO){session.handshake()}; service.stopAdvertising(); match=session; combat=game; audio.music(true,store.data.settings.music,session.stageDef?.music?.takeIf {it.isNotBlank()} ?: store.data.settings.fightTrack); status="Conectado"} catch(e: Exception){session.close(); status=e.message ?: "Error de conexión"}; busy=false}
 if(combat!=null && match!=null){AndroidView(factory={FightView(it,combat!!,Ai(1),store.data.settings,match!!.stageIndex,audio,{match?.close(); combat=null; status="Partida cancelada"},{winner -> val won=winner==match!!.index; if(won) store.update(store.data.copy(coins=store.data.coins+80)); match?.close(); combat=null; message(if(won) "Victoria LAN · +80 monedas" else "Fin del combate LAN")},match,{error -> scope.launch {match?.close(); combat=null; status=error; message(error)}})},modifier=Modifier.fillMaxSize()); return}
 Frame("MULTIJUGADOR LAN",back){Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("PERSONAJE: ${fighter.name}"); Row(Modifier.horizontalScroll(rememberScrollState())){store.roster().filter {store.owned(it)}.forEach {f -> TextButton({fighter=f}){Text(f.name)}}}
   (listOf("AZOTEA NEÓN","SANTUARIO","PUERTO")+store.data.stages.map {it.name}).forEachIndexed {i,name -> TextButton({stage=i}){Text(name,color=if(i==stage) ForgeColors.gold else ForgeColors.text)}}
   OutlinedTextField(room,{room=it.take(32)},label={Text("NOMBRE DE SALA")})
   ForgeButton("CREAR SALA",enabled=!busy){busy=true; scope.launch {runCatching {val p=service.advertise(room); port=p.toString(); val addresses=service.addresses(); code=addresses.firstOrNull()?.let {RoomCode.encode(it,p)} ?: ""; status="IP: ${addresses.joinToString()} · Puerto $p"; val socket=withContext(Dispatchers.IO){service.server!!.accept()}; connect(socket,true)}.onFailure {busy=false; status=it.message ?: "No se pudo crear la sala"}}}
   Text(status,color=ForgeColors.secondary); if(code.isNotBlank()) Text("CÓDIGO: $code",color=ForgeColors.gold)
  }
  Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Text("SALAS ENCONTRADAS: ${rooms.size}"); rooms.forEach {(name,ip,p) -> TextButton({address=ip; port=p.toString()}){Text("$name · $ip:$p")}}
   OutlinedTextField(address,{address=it},label={Text("IP DEL ANFITRIÓN")}); OutlinedTextField(port,{port=it.filter(Char::isDigit)},label={Text("PUERTO")}); OutlinedTextField(code,{code=it},label={Text("CÓDIGO DE SALA")})
   ForgeButton("UNIRSE A SALA",enabled=!busy){busy=true; scope.launch {status="Conectando…"; try {if(address.isBlank() && code.isNotBlank()){val endpoint=RoomCode.decode(code); address=endpoint.first; port=endpoint.second.toString()}; val socket=withContext(Dispatchers.IO){Socket().apply {connect(java.net.InetSocketAddress(address,port.toInt()),5000)}}; connect(socket,false)} catch(e: Exception){busy=false; status="No se pudo conectar: ${e.message}"}}}
  }
 }}
}
