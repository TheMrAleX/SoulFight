package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.provider.MediaStore
import android.content.ContentValues
import com.fightforge.engine.*
import com.fightforge.engine.Rect
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable data class Settings(val music: Int=65,val effects: Int=80,val vibration: Boolean=true,val opacity: Int=80,val hitboxes: Boolean=false,val inputs: Boolean=false,val menuTrack: String="",val fightTrack: String="")
@Serializable data class SaveData(val schemaVersion: Int=1,val coins: Int=0,val purchased: Set<String> = setOf("default_0","default_1"),val settings: Settings=Settings(),val characters: List<FighterDef> = emptyList(),val stages: List<StageDef> = emptyList())
@Serializable data class StageDef(val id: String,val name: String,val frames: List<String>,val fps: Int=12)
class Storage(val context: Context) {
 val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
 private val save=File(context.filesDir,"save.json")
 var warning: String?=null
 var data: SaveData=try { if(save.exists()) json.decodeFromString<SaveData>(save.readText()) else SaveData() } catch(e: Exception) { warning="Datos dañados: se conservó una copia"; save.copyTo(File(context.filesDir,"save-corrupt-${System.currentTimeMillis()}.json")); SaveData() }; private set
 @Synchronized fun update(next: SaveData) { val temp=File(save.parentFile,"save.tmp"); temp.outputStream().use { stream -> stream.write(json.encodeToString(next).toByteArray()); stream.fd.sync() }; check(temp.renameTo(save)); data=next }
 fun roster()=Defaults.roster()+data.characters
 fun owned(f: FighterDef)=f.custom || f.id in data.purchased
 fun buy(f: FighterDef): Boolean { val price=Defaults.prices[Defaults.names.indexOf(f.name)]; if(owned(f) || data.coins<price) return false; update(data.copy(coins=data.coins-price,purchased=data.purchased+f.id)); return true }
 fun character(f: FighterDef) { require(f.stats.valid()); require(f.name.isNotBlank() && f.name.length<=16); val chars=data.characters.filterNot { it.id==f.id }+f; update(data.copy(characters=chars)); val directory=File(context.filesDir,"characters/${f.id}"); directory.mkdirs(); val file=File(directory,"character.json"); val temp=File(directory,"character.tmp"); temp.writeText(json.encodeToString(f)); check(temp.renameTo(file)) }
 fun importImage(uri: android.net.Uri,folder: String): String {
  val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }; context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,options) }
  require(options.outWidth>0 && options.outHeight>0) { "Imagen no válida" }
  var sample=1; while(options.outWidth/sample>4096 || options.outHeight/sample>4096) sample*=2
  val bitmap=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) } ?: error("No se pudo leer la imagen")
  val f=File(context.filesDir,"$folder/${UUID.randomUUID()}.png"); f.parentFile!!.mkdirs(); f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle(); return f.absolutePath
 }
 fun exportPng(name: String,bitmap: Bitmap) {
  val values=ContentValues().apply { put(MediaStore.Images.Media.DISPLAY_NAME,name); put(MediaStore.Images.Media.MIME_TYPE,"image/png"); put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/FightForge"); put(MediaStore.Images.Media.IS_PENDING,1) }
  val uri=context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("No se pudo crear la imagen")
  try { context.contentResolver.openOutputStream(uri).use { requireNotNull(it); check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }; values.clear(); values.put(MediaStore.Images.Media.IS_PENDING,0); context.contentResolver.update(uri,values,null,null) } catch(e: Exception) { context.contentResolver.delete(uri,null,null); throw e }
 }
 fun movementTemplate(f: FighterDef,m: Move) {
  val bounds=TemplateGeometry.movement(Rect(-f.width/2,-f.height,f.width,f.height),Rect(400,-400,200,400),m.boxes)
  require(bounds.w in 1..4096 && bounds.h in 1..4096)
  val bitmap=Bitmap.createBitmap(bounds.w,bounds.h,Bitmap.Config.ARGB_8888)
  exportPng("${m.name}_lienzo.png",bitmap)
  val canvas=Canvas(bitmap); canvas.translate(-bounds.x.toFloat(),-bounds.y.toFloat()); canvas.scale(2f,2f)
  val paint=Paint().apply {style=Paint.Style.STROKE; strokeWidth=2f}
  m.boxes.forEach {b -> paint.color=HitboxView.colors[b.kind.ordinal]; canvas.drawRect(b.rect.x.toFloat(),b.rect.y.toFloat(),(b.rect.x+b.rect.w).toFloat(),(b.rect.y+b.rect.h).toFloat(),paint)}
  paint.color=android.graphics.Color.CYAN; canvas.drawRect(-f.width/2f,-f.height.toFloat(),f.width/2f,0f,paint)
  exportPng("${m.name}_guia.png",bitmap); bitmap.recycle()
 }
 fun template(f: FighterDef,slot: String="quieto") {
  if(slot=="foto") {val b=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888); exportPng("${f.name}_foto_lienzo.png",b); val p=Paint().apply {color=android.graphics.Color.CYAN; style=Paint.Style.STROKE; strokeWidth=2f}; Canvas(b).drawRect(1f,1f,255f,255f,p); exportPng("${f.name}_foto_guia.png",b); b.recycle(); return}
  val t=TemplateGeometry.cached(f.width,f.height); val clear=Bitmap.createBitmap(t.pixelsWide,t.pixelsHigh,Bitmap.Config.ARGB_8888)
  exportPng("${f.name}_lienzo.png",clear)
  val canvas=Canvas(clear); val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=android.graphics.Color.CYAN; style=Paint.Style.STROKE; strokeWidth=2f }
  for(x in 0 until t.pixelsWide step 100) canvas.drawLine(x.toFloat(),0f,x.toFloat(),t.pixelsHigh.toFloat(),p)
  for(y in 0 until t.pixelsHigh step 100) canvas.drawLine(0f,y.toFloat(),t.pixelsWide.toFloat(),y.toFloat(),p)
  p.color=android.graphics.Color.RED; val b=t.body; canvas.drawRect(b.x.toFloat(),b.y.toFloat(),(b.x+b.w).toFloat(),(b.y+b.h).toFloat(),p)
  p.style=Paint.Style.FILL; p.textSize=18f; canvas.drawText("HITBOX (no es el límite del lienzo)",4f,24f,p); canvas.drawText("${t.pixelsWide} × ${t.pixelsHigh} px · ${f.name}",4f,48f,p)
  exportPng("${f.name}_guia.png",clear); clear.recycle()
 }
}
