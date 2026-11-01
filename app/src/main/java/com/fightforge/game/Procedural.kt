package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*
import com.fightforge.engine.State
object ProceduralSprites {
 val colors=intArrayOf(0xFFFF4D5E.toInt(),0xFF3DD9EB.toInt(),0xFFC8A0FF.toInt(),0xFFFFC83D.toInt(),0xFF3DDC84.toInt(),0xFF8B95B8.toInt())
 fun fighter(c: Canvas,x: Float,y: Float,w: Float,h: Float,color: Int,state: State,tick: Int,facing: Int,age: Int=0) {
  c.save(); c.translate(x,y); c.scale(facing.toFloat(),1f)
  val p=Paint(Paint.ANTI_ALIAS_FLAG); val moving=state in listOf(State.WALK,State.RUN,State.JUMP,State.FALL,State.AIR_RUN); val sway=if(moving) sin(tick*.3f)*w*.22f else sin(tick*.08f)*w*.025f
  val crouch=when(state){State.CROUCH,State.BLOCK_CROUCH -> .62f; State.CROUCH_DOWN -> 1f-.38f*(age/5f).coerceIn(0f,1f); State.CROUCH_UP -> .62f+.38f*(age/5f).coerceIn(0f,1f); else -> 1f}
  if(state==State.GETUP) c.rotate(-80f*(1f-(age/30f).coerceIn(0f,1f)))
  if(state in listOf(State.HURT,State.STUN,State.GRABBED)) c.rotate(-12f)
  if(state==State.DIZZY) c.rotate(sin(tick*.14f)*12)
  if(state==State.KO || state==State.DOWN) { c.rotate(-80f); c.translate(0f,-h*.2f) }
  p.color=0x55000000; c.drawOval(-w*.7f,-12f,w*.7f,12f,p)
  c.scale(1f,crouch); p.strokeWidth=w*.20f; p.strokeCap=Paint.Cap.ROUND; p.color=0xFF1E2742.toInt()
  c.drawLine(-w*.18f,-h*.40f,-w*.30f+sway,-h*.03f,p); c.drawLine(w*.18f,-h*.40f,w*.30f-sway,-h*.03f,p)
  p.color=colors[Math.floorMod(color,colors.size)]; c.drawRoundRect(-w*.35f,-h*.77f,w*.35f,-h*.38f,w*.1f,w*.1f,p)
  p.color=0xFFF2D0B0.toInt(); c.drawCircle(0f,-h*.9f,w*.26f,p)
  p.strokeWidth=w*.17f
  val attack=state==State.ATTACK
  val block=state in listOf(State.BLOCK_STAND,State.BLOCK_CROUCH,State.BLOCK_AIR)
  c.drawLine(-w*.3f,-h*.70f,if(attack) w*.75f else if(block) w*.45f else -w*.5f,if(block) -h*.88f else -h*.5f+sway/2,p)
  c.drawLine(w*.3f,-h*.70f,if(attack) w*1.05f else w*.5f,if(block) -h*.9f else if(attack) -h*.67f else -h*.52f-sway/2,p)
  p.color=0xFF0B0F1A.toInt(); c.drawRect(-w*.28f,-h*.94f,w*.28f,-h*.87f,p); p.color=colors[Math.floorMod(color,colors.size)]; c.drawRect(-w*.42f,-h*.43f,w*.42f,-h*.37f,p)
  c.restore()
 }
 fun stage(c: Canvas,width: Float,height: Float,tick: Int,index: Int) {
  val p=Paint(); val top=intArrayOf(0xFF141B2D.toInt(),0xFF1A1330.toInt(),0xFF102B30.toInt())[index%3]
  p.shader=LinearGradient(0f,0f,0f,height,top,0xFF0B0F1A.toInt(),Shader.TileMode.CLAMP); c.drawRect(0f,0f,width,height,p); p.shader=null
  p.color=0xFF1E2742.toInt(); val drift=sin(tick*.002)*20
  for(i in 0..18) { val x=i*width/16+drift.toFloat(); val h=height*(.12f+(i*37%7)*.025f); c.drawRect(x,height*.72f-h,x+width*.045f,height*.8f,p) }
  p.color=0xFF2A3556.toInt(); c.drawRect(0f,height*.85f,width,height,p); p.color=0xFF3DD9EB.toInt(); p.alpha=80; p.strokeWidth=2f
  c.drawLine(0f,height*.85f,width,height*.85f,p)
  for(i in 0..12) c.drawLine(width/2,height*.85f,i*width/12,height,p)
  for(i in 0..4) { val y=height*(.85f+i*.04f); c.drawLine(0f,y,width,y,p) }
 }
}
class ProceduralAudio(val context: Context) {
 private val pool=SoundPool.Builder().setMaxStreams(6).build(); private val sounds=mutableMapOf<String,Int>(); private var player: MediaPlayer?=null
 fun prepare(progress: (Int)->Unit = {}) {
  val notes=doubleArrayOf(1.0,1.25,1.5,2.0); var completed=0
  val dir=File(context.filesDir,"audio").apply { mkdirs() }
  for((name,freq) in listOf("ui" to 880,"hit" to 140,"block" to 300,"ko" to 70,"ulti" to 440,"menu" to 220,"combat" to 165)) {
   val file=File(dir,"$name.wav"); if(!file.exists()) {
    val loop=name in listOf("menu","combat"); val rate=22050; val count=if(loop) rate*8 else rate/5
    val out=ByteBuffer.allocate(44+count*2).order(ByteOrder.LITTLE_ENDIAN)
    out.put("RIFF".toByteArray()); out.putInt(36+count*2); out.put("WAVEfmt ".toByteArray()); out.putInt(16); out.putShort(1); out.putShort(1); out.putInt(rate); out.putInt(rate*2); out.putShort(2); out.putShort(16); out.put("data".toByteArray()); out.putInt(count*2)
    repeat(count) { i -> val note=if(loop) notes[(i/(rate/2))%4] else 1.0; val envelope=if(loop) .3 else (1.0-i.toDouble()/count); val value=(sin(2*PI*freq*note*i/rate)*envelope*8000).toInt().toShort(); out.putShort(value) }
    file.writeBytes(out.array())
   }
   if(name !in listOf("menu","combat")) sounds[name]=pool.load(file.absolutePath,1)
   progress(++completed*100/7)
  }
 }
 fun custom(path: String,volume: Int) {
  if(path.isBlank()) return
  val id=sounds[path]
  if(id!=null) pool.play(id,volume/100f,volume/100f,1,0,1f)
  else {pool.setOnLoadCompleteListener {_,loaded,status -> if(status==0 && loaded==sounds[path]) pool.play(loaded,volume/100f,volume/100f,1,0,1f)}; sounds[path]=pool.load(path,1)}
 }
 fun volume(value: Int){player?.setVolume(value/100f,value/100f)}
 fun effect(name: String,volume: Int) { sounds[name]?.let { pool.play(it,volume/100f,volume/100f,1,0,1f) } }
 fun music(combat: Boolean,volume: Int,track: String="") {
  player?.release(); player=null
  val fallback=File(context.filesDir,"audio/${if(combat) "combat" else "menu"}.wav").absolutePath
  for(path in listOf(track,fallback).filter {it.isNotBlank()}.distinct()) {
   val candidate=MediaPlayer()
   try {candidate.setDataSource(path); candidate.isLooping=true; candidate.setVolume(volume/100f,volume/100f); candidate.prepare(); candidate.start(); player=candidate; return}
   catch(e: Exception) {candidate.release()}
  }
 }
 fun pause() { player?.pause() }
 fun resume() { player?.start() }
 fun close() { player?.release(); pool.release() }
}
