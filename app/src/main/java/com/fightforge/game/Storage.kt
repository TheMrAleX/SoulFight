package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.provider.MediaStore
import android.content.ContentValues
import com.fightforge.engine.*
import com.fightforge.engine.Rect
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import androidx.compose.runtime.*
import java.io.File
import java.util.UUID

@Serializable data class Settings(val music: Int=65,val effects: Int=80,val vibration: Boolean=true,val opacity: Int=80,val hitboxes: Boolean=false,val inputs: Boolean=false,val menuTrack: String="",val fightTrack: String="")
@Serializable data class SaveData(val schemaVersion: Int=1,val coins: Int=0,val purchased: Set<String> = setOf("default_0","default_1"),val settings: Settings=Settings(),val characters: List<FighterDef> = emptyList(),val stages: List<StageDef> = emptyList(),val effectsLibrary: List<EffectDef> = emptyList())
@Serializable data class StageDef(val id: String,val name: String,val frames: List<String>,val fps: Int=12,val music: String="")
@Serializable data class EffectDef(val id: String,val name: String,val frames: List<String>,val fps: Int=12,val scale: Int=100,val sound: String="")
class Storage(val context: Context) {
 val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
 private val save=File(context.filesDir,"save.json")
 var warning: String?=null
 var data: SaveData by mutableStateOf(load()); private set
 private fun load(): SaveData {
  if(!save.exists()) return SaveData()
  return try {
   val root=json.parseToJsonElement(save.readText()).jsonObject
   val version=root["schemaVersion"]?.jsonPrimitive?.intOrNull ?: 0
   require(version in 0..1) {"Versión de datos no compatible"}
   val characters=mutableListOf<FighterDef>()
   for(element in root["characters"]?.jsonArray ?: JsonArray(emptyList())) {
    try {val f=json.decodeFromJsonElement<FighterDef>(element); require(f.stats.valid()); characters.add(f.copy(schemaVersion=1))}
    catch(e: Exception) {warning="Se omitió un personaje dañado"}
   }
   val safe=JsonObject(root+("characters" to JsonArray(emptyList())))
   json.decodeFromJsonElement<SaveData>(safe).copy(schemaVersion=1,characters=characters)
  } catch(e: Exception) {
   warning="Datos dañados o incompatibles: se conservó una copia"
   runCatching {save.copyTo(File(context.filesDir,"save-corrupt-${System.currentTimeMillis()}.json"))}
   val recovered=File(context.filesDir,"characters").listFiles()?.mapNotNull {dir -> runCatching {json.decodeFromString<FighterDef>(File(dir,"character.json").readText()).takeIf {it.stats.valid()}}.getOrNull()} ?: emptyList()
   SaveData(characters=recovered)
  }
 }
 @Synchronized fun update(next: SaveData) { val temp=File(save.parentFile,"save.tmp"); temp.outputStream().use { stream -> stream.write(json.encodeToString(next).toByteArray()); stream.fd.sync() }; check(temp.renameTo(save)); data=next }
 fun roster()=Defaults.roster()+data.characters
 fun owned(f: FighterDef)=f.custom || f.id in data.purchased
 fun buy(f: FighterDef): Boolean { val price=Defaults.prices[Defaults.names.indexOf(f.name)]; if(owned(f) || data.coins<price) return false; update(data.copy(coins=data.coins-price,purchased=data.purchased+f.id)); return true }
 fun character(f: FighterDef) { require(f.stats.valid()); require(f.name.isNotBlank() && f.name.length<=16); val chars=data.characters.filterNot { it.id==f.id }+f; update(data.copy(characters=chars)); val directory=File(context.filesDir,"characters/${f.id}"); directory.mkdirs(); val file=File(directory,"character.json"); val temp=File(directory,"character.tmp"); temp.writeText(json.encodeToString(f)); check(temp.renameTo(file)) }
 fun importImage(uri: android.net.Uri,folder: String,expectedWidth: Int=0,expectedHeight: Int=0): String {
  val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }; context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }
  require(options.outWidth>0 && options.outHeight>0) { "Imagen no válida" }
  var sample=1; while(options.outWidth/sample>4096 || options.outHeight/sample>4096) sample*=2
  var bitmap=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) } ?: error("No se pudo leer la imagen")
  if(expectedWidth>0 && expectedHeight>0 && (bitmap.width!=expectedWidth || bitmap.height!=expectedHeight)) {
   val adjusted=Bitmap.createBitmap(expectedWidth,expectedHeight,Bitmap.Config.ARGB_8888)
   val factor=minOf(expectedWidth.toFloat()/bitmap.width,expectedHeight.toFloat()/bitmap.height)
   val w=bitmap.width*factor; val h=bitmap.height*factor
   Canvas(adjusted).drawBitmap(bitmap,null,RectF((expectedWidth-w)/2,(expectedHeight-h)/2,(expectedWidth+w)/2,(expectedHeight+h)/2),Paint(Paint.FILTER_BITMAP_FLAG))
   bitmap.recycle(); bitmap=adjusted
  }
  val f=File(context.filesDir,"$folder/${UUID.randomUUID()}.png"); f.parentFile!!.mkdirs(); f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle(); return f.absolutePath
 }
 fun dimensions(uri: android.net.Uri): Pair<Int,Int> {
  val options=BitmapFactory.Options().apply {inJustDecodeBounds=true}
  context.contentResolver.openInputStream(uri).use {BitmapFactory.decodeStream(it,null,options)}
  require(options.outWidth>0 && options.outHeight>0) {"Imagen no válida"}
  return options.outWidth to options.outHeight
 }
 fun importAudio(uri: android.net.Uri,folder: String): String {
  val f=File(context.filesDir,"$folder/${UUID.randomUUID()}.audio"); f.parentFile!!.mkdirs()
  context.contentResolver.openInputStream(uri).use {input -> requireNotNull(input); f.outputStream().use {input.copyTo(it)}}
  val player=android.media.MediaPlayer()
  try {player.setDataSource(f.absolutePath); player.prepare()} catch(e: Exception) {f.delete(); throw IllegalArgumentException("Audio incompatible",e)} finally {player.release()}
  return f.absolutePath
 }
 fun effect(effect: EffectDef) {require(effect.name.isNotBlank() && effect.frames.size in 1..12 && effect.fps in 1..30 && effect.scale in 25..400); update(data.copy(effectsLibrary=data.effectsLibrary.filterNot {it.id==effect.id}+effect))}
 fun exportPng(name: String,bitmap: Bitmap) {
  val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/png"); put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/FightForge"); put(MediaStore.Images.Media.IS_PENDING,1) }
  val uri=context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("No se pudo crear la imagen")
  try { context.contentResolver.openOutputStream(uri).use { requireNotNull(it); check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; values.clear(); values.put(MediaStore.Images.Media.IS_PENDING,0); context.contentResolver.update(uri,values,null,null) } catch(e: Exception) { context.contentResolver.delete(uri,null,null); throw e }
 }
 fun movementTemplate(f: FighterDef,m: Move) {
  val body=MotionGeometry.body(f.width,f.height); val rival=Rect(400,-400,200,400)
  val bounds=TemplateGeometry.movement(body,rival,m.boxes)
  val factor=minOf(1f,4096f/bounds.w,4096f/bounds.h)
  val bitmap=Bitmap.createBitmap((bounds.w*factor).toInt().coerceAtLeast(1),(bounds.h*factor).toInt().coerceAtLeast(1),Bitmap.Config.ARGB_8888)
  exportPng("${m.name}_lienzo.png",bitmap)
  val canvas=Canvas(bitmap); canvas.scale(factor,factor); canvas.translate(-bounds.x.toFloat(),-bounds.y.toFloat()); canvas.scale(2f,2f)
  val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply {style=Paint.Style.STROKE; strokeWidth=2f}
  fun rect(r: Rect,color: Int){paint.color=color; canvas.drawRect(r.x.toFloat(),r.y.toFloat(),(r.x+r.w).toFloat(),(r.y+r.h).toFloat(),paint)}
  val move=m.boxes.firstOrNull {it.kind==BoxKind.MOVEMENT && it.entity=="player"}; val delta=move?.let {MotionGeometry.displacement(it.rect,body)} ?: (0 to 0)
  val end=m.boxes.firstOrNull {it.kind==BoxKind.TELEPORT && !it.rival}?.rect ?: body.copy(x=body.x+delta.first,y=body.y+delta.second)
  rect(body,0xFF1A237E.toInt()); rect(end,0xFF1A237E.toInt()); rect(rival,0xFFFFB300.toInt())
  m.boxes.forEach {box -> rect(box.rect,HitboxView.colors[box.kind.ordinal]); rect(box.rect.copy(x=box.rect.x+end.x-body.x,y=box.rect.y+end.y-body.y),HitboxView.colors[box.kind.ordinal])}
  paint.style=Paint.Style.FILL; paint.textSize=12f; var line=bounds.y/2f+18
  listOf("PERSONAJE / RIVAL","ROJO: DAÑO · CELESTE: RECORRIDO","BLANCO: DESTINO · NARANJA: CASTIGO","VERDE: OBJETO · TURQUESA: PROYECTIL","MAGENTA: TRANSLÚCIDO · CAFÉ: AGARRE").forEach {paint.color=0xFFF2F5FF.toInt(); canvas.drawText(it,bounds.x/2f+8,line,paint); line+=16}
  exportPng("${m.name}_guia.png",bitmap); bitmap.recycle()
 }
 fun template(f: FighterDef,slot: String="quieto") {
  if(slot=="foto") {val b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888); exportPng("${f.name}_foto_lienzo.png",b); val p=Paint().apply {color=android.graphics.Color.CYAN; style=Paint.Style.STROKE; strokeWidth=2f}; Canvas(b).drawRect(1f,1f,255f,255f,p); exportPng("${f.name}_foto_guia.png",b); b.recycle(); return}
  val t=TemplateGeometry.cached(f.width,f.height); val clear=Bitmap.createBitmap(t.pixelsWide,t.pixelsHigh,Bitmap.Config.ARGB_8888)
  exportPng("${f.name}_lienzo.png",clear)
  val canvas=Canvas(clear); val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.CYAN; style=Paint.Style.STROKE; strokeWidth=2f }
  for(x in 0 until t.pixelsWide step 100) canvas.drawLine(x.toFloat(),0f,x.toFloat(),t.pixelsHigh.toFloat(),p)
  for(y in 0 until t.pixelsHigh step 100) canvas.drawLine(0f,y.toFloat(),t.pixelsWide.toFloat(),y.toFloat(),p)
  val b=t.body
  p.color=0x338B95B8; for(x in -t.pixelsHigh until t.pixelsWide step 16) {canvas.save(); canvas.clipOutRect(b.x.toFloat(),b.y.toFloat(),(b.x+b.w).toFloat(),(b.y+b.h).toFloat()); canvas.drawLine(x.toFloat(),0f,(x+t.pixelsHigh).toFloat(),t.pixelsHigh.toFloat(),p); canvas.restore()}
  p.color=android.graphics.Color.CYAN; canvas.drawLine(0f,(b.y+b.h).toFloat(),t.pixelsWide.toFloat(),(b.y+b.h).toFloat(),p); val cx=t.pixelsWide/2f; val cy=t.pixelsHigh/2f; canvas.drawLine(cx-12,cy,cx+12,cy,p); canvas.drawLine(cx,cy-12,cx,cy+12,p)
  p.color=android.graphics.Color.RED; canvas.drawRect(b.x.toFloat(),b.y.toFloat(),(b.x+b.w).toFloat(),(b.y+b.h).toFloat(),p)
  p.style=Paint.Style.FILL; p.textSize=18f; canvas.drawText("HITBOX (no es el límite del lienzo)",4f,24f,p); canvas.drawText("${t.pixelsWide} × ${t.pixelsHigh} px · ${f.name}",4f,48f,p)
  exportPng("${f.name}_guia.png",clear); clear.recycle()
 }
}
