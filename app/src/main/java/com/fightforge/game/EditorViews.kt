package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.*
import com.fightforge.engine.*
import com.fightforge.engine.Rect
import kotlin.math.*
class SizeView(context: Context,var bodyWidth: Int,var bodyHeight: Int,val change: (Int,Int)->Unit): View(context) {
 private val p=Paint(Paint.ANTI_ALIAS_FLAG); private var handle=-1
 private fun scale()=min(width/600f,height/950f)
 override fun onDraw(c: Canvas) { super.onDraw(c); val s=scale(); val cx=width/2f; val bottom=height*.94f
  p.color=0xFF8B95B8.toInt(); p.style=Paint.Style.STROKE; p.strokeWidth=2f; p.pathEffect=DashPathEffect(floatArrayOf(8f,8f),0f); c.drawRect(cx-100*s,bottom-400*s,cx+100*s,bottom,p); p.pathEffect=null
  p.color=0xFF3DD9EB.toInt(); c.drawRect(cx-bodyWidth*s/2,bottom-bodyHeight*s,cx+bodyWidth*s/2,bottom,p); p.style=Paint.Style.FILL
  for(pt in handles()) c.drawCircle(pt.first,pt.second,14*resources.displayMetrics.density,p)
 }
 private fun handles(): List<Pair<Float,Float>> { val s=scale(); val cx=width/2f; val bottom=height*.94f; return listOf(cx-bodyWidth*s/2 to bottom-bodyHeight*s/2,cx+bodyWidth*s/2 to bottom-bodyHeight*s/2,cx to bottom-bodyHeight*s,cx to bottom) }
 override fun onTouchEvent(e: MotionEvent): Boolean { when(e.actionMasked) {MotionEvent.ACTION_DOWN -> {handle=handles().indices.minBy {i -> hypot(e.x-handles()[i].first,e.y-handles()[i].second)}; parent.requestDisallowInterceptTouchEvent(true)}; MotionEvent.ACTION_MOVE -> { val s=scale(); if(handle in 0..1) bodyWidth=(abs(e.x-width/2f)*2/s).toInt().coerceIn(100,400) else bodyHeight=(abs(e.y-height*.94f)/s).toInt().coerceIn(200,800); change(bodyWidth,bodyHeight); invalidate() }; MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> {handle=-1; parent.requestDisallowInterceptTouchEvent(false)} }; return true }
}
class HitboxView(context: Context,val fighter: FighterDef,var boxes: List<Box>,val kind: ()->BoxKind,val add: (Rect)->Unit): View(context) {
 private val p=Paint(Paint.ANTI_ALIAS_FLAG); private var down: Pair<Int,Int>?=null; private var current: Pair<Int,Int>?=null
 private fun point(x: Float,y: Float)=((x-width*.35f)/scale()).toInt() to ((y-height*.9f)/scale()).toInt()
 private fun scale()=min(width/1200f,height/950f)
 override fun onDraw(c: Canvas) {c.drawColor(0xFF141B2D.toInt()); c.save(); c.translate(width*.35f,height*.9f); c.scale(scale(),scale()); ProceduralSprites.fighter(c,0f,0f,fighter.width.toFloat(),fighter.height.toFloat(),fighter.color,State.IDLE,0,1); ProceduralSprites.fighter(c,500f,0f,200f,400f,1,State.IDLE,0,-1); p.style=Paint.Style.STROKE; p.strokeWidth=5f
  boxes.forEach {b -> p.color=colors[b.kind.ordinal]; c.drawRect(b.rect.x.toFloat(),b.rect.y.toFloat(),(b.rect.x+b.rect.w).toFloat(),(b.rect.y+b.rect.h).toFloat(),p) }
  val a=down; val b=current; if(a!=null && b!=null) {p.color=colors[kind().ordinal]; c.drawRect(min(a.first,b.first).toFloat(),min(a.second,b.second).toFloat(),max(a.first,b.first).toFloat(),max(a.second,b.second).toFloat(),p)}; c.restore()
 }
 override fun onTouchEvent(e: MotionEvent): Boolean { when(e.actionMasked) {MotionEvent.ACTION_DOWN -> {down=point(e.x,e.y); current=down; parent.requestDisallowInterceptTouchEvent(true)}; MotionEvent.ACTION_MOVE -> {current=point(e.x,e.y); invalidate()}; MotionEvent.ACTION_UP -> {val a=down!!; val b=point(e.x,e.y); var r=Rect(min(a.first,b.first),min(a.second,b.second),abs(a.first-b.first).coerceIn(20,800),abs(a.second-b.second).coerceIn(20,800)); if(kind() in listOf(BoxKind.DAMAGE,BoxKind.MOVEMENT,BoxKind.PUNISH)) {val edge=fighter.width/2; r=r.copy(x=if(r.x>=0) edge else -edge-r.w,y=r.y.coerceIn(-fighter.height,0))}; add(r); down=null; current=null; parent.requestDisallowInterceptTouchEvent(false); invalidate()}; MotionEvent.ACTION_CANCEL -> {down=null; current=null} }; return true }
 companion object {val colors=intArrayOf(0xFFE53935.toInt(),0xFF4FC3F7.toInt(),0xFFFFFFFF.toInt(),0xFFFB8C00.toInt(),0xFF00BFA5.toInt(),0xFF43A047.toInt(),0xFFD81B60.toInt(),0xFF6D4C41.toInt())}
}
class PixelView(context: Context,val bitmap: Bitmap): View(context) {
 var tool="LÁPIZ"; var color=Color.WHITE; var brush=4; var picked: (Int)->Unit={}; private val p=Paint(); private val history=ArrayDeque<Bitmap>(); private val future=ArrayDeque<Bitmap>(); private var start=0f to 0f; private var previous=0f to 0f
 private fun scale()=min(width.toFloat()/bitmap.width,height.toFloat()/bitmap.height)
 private fun point(e: MotionEvent)=((e.x-(width-bitmap.width*scale())/2)/scale()).coerceIn(0f,bitmap.width-1f) to ((e.y-(height-bitmap.height*scale())/2)/scale()).coerceIn(0f,bitmap.height-1f)
 override fun onDraw(c: Canvas) {c.drawColor(0xFF2A3556.toInt()); c.save(); c.translate((width-bitmap.width*scale())/2,(height-bitmap.height*scale())/2); c.scale(scale(),scale()); c.drawBitmap(bitmap,0f,0f,p); c.restore()}
 private fun snapshot() {history.addLast(bitmap.copy(Bitmap.Config.ARGB_8888,true)); if(history.size>50) history.removeFirst().recycle(); future.forEach {it.recycle()}; future.clear()}
 fun undo() {if(history.isEmpty()) return; future.addLast(bitmap.copy(Bitmap.Config.ARGB_8888,true)); restore(history.removeLast())}
 fun redo() {if(future.isEmpty()) return; history.addLast(bitmap.copy(Bitmap.Config.ARGB_8888,true)); restore(future.removeLast())}
 private fun restore(b: Bitmap) {val c=Canvas(bitmap); c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR); c.drawBitmap(b,0f,0f,null); b.recycle(); invalidate()}
 private fun fill(x: Int,y: Int) {val old=bitmap.getPixel(x,y); if(old==color) return; val queue=ArrayDeque<Pair<Int,Int>>(); queue.add(x to y); bitmap.setPixel(x,y,color); while(queue.isNotEmpty()) {val (a,b)=queue.removeFirst(); for((dx,dy) in listOf(1 to 0,-1 to 0,0 to 1,0 to -1)) {val nx=a+dx; val ny=b+dy; if(nx in 0 until bitmap.width && ny in 0 until bitmap.height && bitmap.getPixel(nx,ny)==old) {bitmap.setPixel(nx,ny,color); queue.add(nx to ny)}}}}
 override fun onTouchEvent(e: MotionEvent): Boolean { val pos=point(e); val canvas=Canvas(bitmap); p.color=color; p.strokeWidth=brush.toFloat(); p.strokeCap=Paint.Cap.ROUND; p.style=Paint.Style.STROKE; p.xfermode=if(tool=="BORRADOR") PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
  when(e.actionMasked) {MotionEvent.ACTION_DOWN -> {parent.requestDisallowInterceptTouchEvent(true); snapshot(); start=pos; previous=pos; when(tool) {"RELLENO" -> fill(pos.first.toInt(),pos.second.toInt()); "CUENTAGOTAS" -> picked(bitmap.getPixel(pos.first.toInt(),pos.second.toInt())); "LÁPIZ","BORRADOR" -> canvas.drawPoint(pos.first,pos.second,p)}}; MotionEvent.ACTION_MOVE -> {if(tool in listOf("LÁPIZ","BORRADOR")) canvas.drawLine(previous.first,previous.second,pos.first,pos.second,p); previous=pos}; MotionEvent.ACTION_UP -> {if(tool=="LÍNEA") canvas.drawLine(start.first,start.second,pos.first,pos.second,p); if(tool=="RECTÁNGULO") canvas.drawRect(min(start.first,pos.first),min(start.second,pos.second),max(start.first,pos.first),max(start.second,pos.second),p); parent.requestDisallowInterceptTouchEvent(false)} }; p.xfermode=null; invalidate(); return true
 }
}
