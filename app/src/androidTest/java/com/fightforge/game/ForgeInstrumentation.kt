package com.fightforge.game

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.provider.MediaStore
import android.view.MotionEvent
import com.fightforge.engine.*
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ForgeInstrumentation: Instrumentation() {
 override fun onStart() {
  val result=Bundle(); var count=0
  try {
   val context=targetContext; val store=Storage(context); val original=store.data
   val id="acceptance-${UUID.randomUUID()}"; val dir=File(context.filesDir,"characters/$id").apply {mkdirs()}
   try {
    fun png(name: String,w: Int,h: Int,color: Int): String {val b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); Canvas(b).drawColor(color); val file=File(dir,name); file.outputStream().use {check(b.compress(Bitmap.CompressFormat.PNG,100,it))}; b.recycle(); return file.absolutePath}
    val idle=png("idle.png",460,920,Color.CYAN); val photo=png("photo.png",256,256,Color.RED)
    val adjusted=store.importImage(android.net.Uri.fromFile(File(photo)),"characters/$id/import",128,256)
    val adjustedBitmap=android.graphics.BitmapFactory.decodeFile(adjusted)
    check(adjustedBitmap.width==128 && adjustedBitmap.height==256 && Color.alpha(adjustedBitmap.getPixel(0,0))==0 && adjustedBitmap.getPixel(64,128)==Color.RED); adjustedBitmap.recycle(); count++
    val effect=EffectDef(id,"IMPACTO DE PRUEBA",listOf(photo),15,150)
    val stage=StageDef(id,"ESCENARIO DE PRUEBA",listOf(png("stage0.png",1920,1080,Color.DKGRAY),png("stage1.png",1920,1080,Color.BLUE)),18)
    store.update(original.copy(effectsLibrary=original.effectsLibrary+effect,stages=listOf(stage)))
    val hero=Defaults.roster()[0].copy(id=id,name="T${id.takeLast(7)}",custom=true,animations=mapOf("quieto" to listOf(idle),"foto" to listOf(photo)),hitEffect=id,hurtEffect=id)
    store.character(hero); check(Storage(context).data.characters.any {it.id==id}); count++
    val bytes=CharacterPackages.encode(store,hero); check(bytes.contentEquals(CharacterPackages.encode(store,hero)))
    val decoded=CharacterPackages.decode(store,bytes); check(File(decoded.animations.getValue("quieto").first()).readBytes().contentEquals(File(idle).readBytes())); check(store.data.effectsLibrary.any {it.id==decoded.hitEffect && it.scale==150}); count++
    val stageBytes=StagePackages.encode(store,stage); val decodedStage=StagePackages.decode(store,stageBytes); check(decodedStage.fps==18 && decodedStage.frames.size==2); check(File(decodedStage.frames[1]).readBytes().contentEquals(File(stage.frames[1]).readBytes())); count++
    fun peerRun(ticks: Int): Long {
     val executor=Executors.newFixedThreadPool(2); val listener=ServerSocket(0)
     try {
      val host=executor.submit(Callable {
       NetworkMatch(listener.accept(),true,hero,store,3).use {session -> val g=session.handshake(); repeat(ticks){i -> val inputs=session.exchange(Input(horizontal=if(i%240<120) 1 else -1,buttons=if(i%900==0) 1 else 0),g); g.step(inputs.first,inputs.second)}; check(g.tick==ticks); session.sentAssetBytes to g.checksum()}
      })
      val client=executor.submit(Callable {
       NetworkMatch(Socket("127.0.0.1",listener.localPort),false,Defaults.roster()[1],store).use {session -> val g=session.handshake(); check(session.stageDef?.name==stage.name && session.stageDef?.frames?.size==2); repeat(ticks){i -> val inputs=session.exchange(Input(horizontal=if(i%240<120) -1 else 1),g); g.step(inputs.first,inputs.second)}; session.sentAssetBytes to g.checksum()}
      })
      val h=host.get(90,TimeUnit.SECONDS); val c=client.get(90,TimeUnit.SECONDS); check(h.second==c.second); return h.first+c.first
     } finally {listener.close(); executor.shutdownNow()}
    }
    check(peerRun(3600)>0); count++
    check(peerRun(120)==0L); count++
    File(context.cacheDir,"network").listFiles()?.firstOrNull()?.writeText("damaged cache")
    check(peerRun(120)>0); count++
    runOnMainSync {
     val bitmap=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888); val view=PixelView(context,bitmap); android.widget.FrameLayout(context).addView(view); view.layout(0,0,128,128); view.brush=1; view.color=Color.WHITE
     fun point(x: Float,y: Float){val now=android.os.SystemClock.uptimeMillis(); val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0); val up=MotionEvent.obtain(now,now+1,MotionEvent.ACTION_UP,x,y,0); view.onTouchEvent(down); view.onTouchEvent(up); down.recycle(); up.recycle()}
     repeat(51){i -> point(((i%16)*3+1.5f)*2,((i/16)*3+1.5f)*2)}
     repeat(50){view.undo()}; val pixels=IntArray(4096); bitmap.getPixels(pixels,0,64,0,0,64,64); check(pixels.count {Color.alpha(it)>0}==1)
     repeat(50){view.redo()}; bitmap.getPixels(pixels,0,64,0,0,64,64); check(pixels.count {Color.alpha(it)>0}==51)
     view.tool="RELLENO"; view.color=Color.RED; point(126f,126f); check(bitmap.getPixel(63,63)==Color.RED)
     view.tool="BORRADOR"; view.brush=4; point(126f,126f); check(Color.alpha(bitmap.getPixel(63,63))==0)
     bitmap.recycle()
    }; count++
    val broken=hero.copy(id="broken-$id",name="RECUPERADO"); store.character(broken)
    val file=File(context.filesDir,"save.json"); val raw=file.readText(); file.writeText("{ damaged save"); check(Storage(context).data.characters.any {it.id==broken.id}); file.writeText(raw); count++
    store.deleteCharacter(broken.id); check(!File(context.filesDir,"characters/${broken.id}").exists() && Storage(context).data.characters.none {it.id==broken.id}); count++
    result.putBoolean("passed",true); result.putString("stream","PASS: $count Android acceptance checks; TCP lockstep 3600 ticks, stage and character/effect transfer, SHA cache reuse and repair, persistence, pixel tools and 50 undo/redo steps.\n"); result.putInt("checks",count)
   } finally {store.update(original); File(context.filesDir,"characters/$id").deleteRecursively(); File(context.filesDir,"characters/broken-$id").deleteRecursively()}
   finish(Activity.RESULT_OK,result)
  } catch(e: Throwable) {result.putBoolean("passed",false); result.putString("stream",e.stackTraceToString()); finish(Activity.RESULT_CANCELED,result)}
 }
}
