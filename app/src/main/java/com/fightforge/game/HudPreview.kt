package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.View
import com.fightforge.engine.*
import java.io.File
class HudPreview(context: Context,store: Storage): View(context) {
 private val game=Combat(store.roster().first(),Defaults.roster()[1],practice=true)
 private val images=mutableMapOf<String,Bitmap>()
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 init {listOf("Barra_vida","Barra_poder","Joystick_fondo","Joystick_mando","Boton_A","Boton_B","Boton_C","Boton_D").forEach {name -> BitmapFactory.decodeFile(File(context.filesDir,"ui/$name.png").absolutePath)?.let {images[name]=it}}}
 override fun onDraw(c: Canvas) {
  c.drawColor(0xFF0B0F1A.toInt()); c.save(); val s=minOf(width/1920f,height/1080f); c.scale(s,s)
  ProceduralSprites.stage(c,1920f,1080f,0,0)
  for(i in 0..1) {val x=if(i==0) 160f else 1120f; paint.color=0xFF3DDC84.toInt(); val r=RectF(x,40f,x+640,85f); val hp=images["Barra_vida"]; if(hp!=null) c.drawBitmap(hp,null,r,paint) else c.drawRect(r,paint); paint.color=0xFF3DD9EB.toInt(); val power=images["Barra_poder"]; val pr=RectF(x,95f,x+640,115f); if(power!=null) c.drawBitmap(power,null,pr,paint) else c.drawRect(pr,paint); ProceduralSprites.fighter(c,if(i==0) 750f else 1150f,920f,200f,400f,i,com.fightforge.engine.State.IDLE,0,if(i==0) 1 else -1)}
  val items=listOf(Triple("Joystick_fondo",240f to 840f,300f),Triple("Joystick_mando",240f to 840f,116f),Triple("Boton_A",1640f to 940f,136f),Triple("Boton_B",1770f to 810f,136f),Triple("Boton_C",1510f to 810f,136f),Triple("Boton_D",1640f to 680f,136f))
  items.forEach {(name,p,size) -> paint.color=0xFF3DD9EB.toInt(); val bitmap=images[name]; if(bitmap!=null) c.drawBitmap(bitmap,null,RectF(p.first-size/2,p.second-size/2,p.first+size/2,p.second+size/2),paint) else c.drawCircle(p.first,p.second,size/2,paint)}
  c.restore()
 }
 override fun onDetachedFromWindow(){super.onDetachedFromWindow(); images.values.forEach {it.recycle()}}
}
