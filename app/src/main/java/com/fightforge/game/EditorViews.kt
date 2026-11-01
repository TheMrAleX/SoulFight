package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.*
import com.fightforge.engine.*
import com.fightforge.engine.Rect
import kotlin.math.*
import java.io.File
import java.util.UUID
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
class HitboxView(context: Context,val fighter: FighterDef,var boxes: List<Box>,val kind: ()->BoxKind,val add: (Rect)->Unit,val edit: (Int,Rect)->Unit,val select: (Int)->Unit): View(context) {
 private val p=Paint(Paint.ANTI_ALIAS_FLAG); var selected=-1; private var down: Pair<Int,Int>?=null; private var current: Pair<Int,Int>?=null; private var original: Rect?=null; private var handle=-1
 private fun point(x: Float,y: Float)=((x-width*.35f)/scale()).toInt() to ((y-height*.9f)/scale()).toInt()
 private fun scale()=min(width/1200f,height/950f)
 override fun onDraw(c: Canvas) {c.drawColor(0xFF141B2D.toInt()); c.save(); c.translate(width*.35f,height*.9f); c.scale(scale(),scale()); ProceduralSprites.fighter(c,0f,0f,fighter.width.toFloat(),fighter.height.toFloat(),fighter.color,State.IDLE,0,1); ProceduralSprites.fighter(c,500f,0f,200f,400f,1,State.IDLE,0,-1); p.style=Paint.Style.STROKE; p.strokeWidth=5f
  boxes.forEach {b -> p.color=colors[b.kind.ordinal]; c.drawRect(b.rect.x.toFloat(),b.rect.y.toFloat(),(b.rect.x+b.rect.w).toFloat(),(b.rect.y+b.rect.h).toFloat(),p)
   if(b.kind==BoxKind.MOVEMENT) {val (dx,dy)=MotionGeometry.displacement(b.rect,MotionGeometry.body(fighter.width,fighter.height)); c.drawLine(0f,-fighter.height/2f,dx.toFloat(),-fighter.height/2f+dy,p); c.drawCircle(dx.toFloat(),-fighter.height/2f+dy,10f,p)}
  }
  if(selected in boxes.indices) {val r=boxes[selected].rect; p.style=Paint.Style.FILL; p.color=Color.WHITE; listOf(r.x to r.y+r.h/2,r.x+r.w to r.y+r.h/2,r.x+r.w/2 to r.y,r.x+r.w/2 to r.y+r.h).forEach {(x,y) -> c.drawCircle(x.toFloat(),y.toFloat(),14f,p)}; p.style=Paint.Style.STROKE}
  val a=down; val b=current; if(a!=null && b!=null) {p.color=colors[kind().ordinal]; c.drawRect(min(a.first,b.first).toFloat(),min(a.second,b.second).toFloat(),max(a.first,b.first).toFloat(),max(a.second,b.second).toFloat(),p)}; c.restore()
 }
 override fun onTouchEvent(e: MotionEvent): Boolean {
  val pos=point(e.x,e.y)
  when(e.actionMasked) {
   MotionEvent.ACTION_DOWN -> {
    down=pos; current=pos; original=null; handle=-1
    val index=boxes.indices.reversed().firstOrNull {i -> val r=boxes[i].rect; pos.first in r.x-25..r.x+r.w+25 && pos.second in r.y-25..r.y+r.h+25}
    if(index!=null) {selected=index; select(index); original=boxes[index].rect; val r=original!!; val handles=listOf(r.x to r.y+r.h/2,r.x+r.w to r.y+r.h/2,r.x+r.w/2 to r.y,r.x+r.w/2 to r.y+r.h); handle=handles.indices.firstOrNull {hypot((handles[it].first-pos.first).toDouble(),(handles[it].second-pos.second).toDouble())<35} ?: -1}
    parent.requestDisallowInterceptTouchEvent(true)
   }
   MotionEvent.ACTION_MOVE -> {current=pos; invalidate()}
   MotionEvent.ACTION_UP -> {
    val a=down ?: pos; val old=original
    var r=if(old==null) Rect(min(a.first,pos.first),min(a.second,pos.second),abs(a.first-pos.first).coerceIn(20,800),abs(a.second-pos.second).coerceIn(20,800))
    else when(handle) {0 -> old.copy(x=pos.first,w=(old.x+old.w-pos.first).coerceIn(20,800)); 1 -> old.copy(w=(pos.first-old.x).coerceIn(20,800)); 2 -> old.copy(y=pos.second,h=(old.y+old.h-pos.second).coerceIn(20,800)); 3 -> old.copy(h=(pos.second-old.y).coerceIn(20,800)); else -> old.copy(x=old.x+pos.first-a.first,y=old.y+pos.second-a.second)}
    val type=if(old==null) kind() else boxes[selected].kind
    if(type in listOf(BoxKind.DAMAGE,BoxKind.MOVEMENT,BoxKind.PUNISH)) r=MotionGeometry.snap(r,MotionGeometry.body(fighter.width,fighter.height))
    if(type==BoxKind.TELEPORT) r=r.copy(w=fighter.width,h=fighter.height)
    if(type==BoxKind.GRAB) r=MotionGeometry.grab(fighter.width,fighter.height,fighter.stats.reach)
    if(old==null) add(r) else edit(selected,r)
    down=null; current=null; original=null; parent.requestDisallowInterceptTouchEvent(false); invalidate()
   }
   MotionEvent.ACTION_CANCEL -> {down=null; current=null; original=null}
  }; return true
 }
 companion object {val colors=intArrayOf(0xFFE53935.toInt(),0xFF4FC3F7.toInt(),0xFFFFFFFF.toInt(),0xFFFB8C00.toInt(),0xFF00BFA5.toInt(),0xFF43A047.toInt(),0xFFD81B60.toInt(),0xFF6D4C41.toInt())}
}
class PixelView(context: Context,val bitmap: Bitmap): View(context) {
 init {contentDescription="LIENZO DE PÍXELES"}
 private var zoom=1f; private var panX=0f; private var panY=0f; private var lastCenter: Pair<Float,Float>?=null
 private val scaling=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener(){override fun onScale(detector: ScaleGestureDetector): Boolean {zoom=(zoom*detector.scaleFactor).coerceIn(1f,8f); invalidate(); return true}})
 var onion: Bitmap?=null; var guide: Rect?=null; var editable=true
 var tool="LÁPIZ"; var color=Color.WHITE; var brush=4; var picked: (Int)->Unit={}; private val p=Paint(); private val journal=File(context.cacheDir,"pixel-history/${UUID.randomUUID()}").apply {mkdirs()}; private val history=ArrayDeque<File>(); private val future=ArrayDeque<File>(); private var start=0f to 0f; private var previous=0f to 0f
 private fun scale()=min(width.toFloat()/bitmap.width,height.toFloat()/bitmap.height)*zoom
 private fun point(e: MotionEvent)=((e.x-panX-(width-bitmap.width*scale())/2)/scale()).coerceIn(0f,bitmap.width-1f) to ((e.y-panY-(height-bitmap.height*scale())/2)/scale()).coerceIn(0f,bitmap.height-1f)
 override fun onDraw(c: Canvas) {c.drawColor(0xFF2A3556.toInt()); c.save(); c.translate(panX+(width-bitmap.width*scale())/2,panY+(height-bitmap.height*scale())/2); c.scale(scale(),scale()); onion?.let {p.alpha=77; c.drawBitmap(it,0f,0f,p); p.alpha=255}; c.drawBitmap(bitmap,0f,0f,p)
  guide?.let {r -> p.color=Color.CYAN; p.style=Paint.Style.STROKE; p.strokeWidth=1f/scale(); p.pathEffect=DashPathEffect(floatArrayOf(8f,8f),0f); c.drawRect(r.x.toFloat(),r.y.toFloat(),(r.x+r.w).toFloat(),(r.y+r.h).toFloat(),p); p.pathEffect=null; p.style=Paint.Style.FILL}; c.restore()}
 private fun archive(): File {val f=File(journal,"${UUID.randomUUID()}.png"); f.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}; return f}
 private fun snapshot() {history.addLast(archive()); if(history.size>50) history.removeFirst().delete(); future.forEach {it.delete()}; future.clear()}
 fun undo() {if(history.isEmpty()) return; future.addLast(archive()); restore(history.removeLast())}
 fun redo() {if(future.isEmpty()) return; history.addLast(archive()); restore(future.removeLast())}
 private fun restore(file: File) {val b=BitmapFactory.decodeFile(file.absolutePath) ?: return; file.delete(); val c=Canvas(bitmap); c.drawColor(Color.TRANSPARENT,PorterDuff.Mode.CLEAR); c.drawBitmap(b,0f,0f,null); b.recycle(); invalidate()}
 override fun onDetachedFromWindow() {super.onDetachedFromWindow(); journal.deleteRecursively()}
 private fun fill(x: Int,y: Int) {val old=bitmap.getPixel(x,y); if(old==color) return; val queue=ArrayDeque<Pair<Int,Int>>(); queue.add(x to y); bitmap.setPixel(x,y,color); while(queue.isNotEmpty()) {val (a,b)=queue.removeFirst(); for((dx,dy) in listOf(1 to 0,-1 to 0,0 to 1,0 to -1)) {val nx=a+dx; val ny=b+dy; if(nx in 0 until bitmap.width && ny in 0 until bitmap.height && bitmap.getPixel(nx,ny)==old) {bitmap.setPixel(nx,ny,color); queue.add(nx to ny)}}}}
 override fun onTouchEvent(e: MotionEvent): Boolean {
  if(!editable) return true
  scaling.onTouchEvent(e)
  if(e.pointerCount>=2) {val center=(e.getX(0)+e.getX(1))/2 to (e.getY(0)+e.getY(1))/2; lastCenter?.let {panX+=center.first-it.first; panY+=center.second-it.second}; lastCenter=center; invalidate(); return true}
  if(e.actionMasked==MotionEvent.ACTION_POINTER_UP || e.actionMasked==MotionEvent.ACTION_UP) lastCenter=null
  val pos=point(e); val canvas=Canvas(bitmap); p.color=color; p.strokeWidth=brush.toFloat(); p.strokeCap=Paint.Cap.ROUND; p.style=Paint.Style.STROKE; p.xfermode=if(tool=="BORRADOR") PorterDuffXfermode(PorterDuff.Mode.CLEAR) else null
  when(e.actionMasked) {MotionEvent.ACTION_DOWN -> {parent.requestDisallowInterceptTouchEvent(true); if(tool!="CUENTAGOTAS") snapshot(); start=pos; previous=pos; when(tool) {"RELLENO" -> fill(pos.first.toInt(),pos.second.toInt()); "CUENTAGOTAS" -> picked(bitmap.getPixel(pos.first.toInt(),pos.second.toInt())); "LÁPIZ","BORRADOR" -> canvas.drawPoint(pos.first,pos.second,p)}}; MotionEvent.ACTION_MOVE -> {if(tool in listOf("LÁPIZ","BORRADOR")) canvas.drawLine(previous.first,previous.second,pos.first,pos.second,p); previous=pos}; MotionEvent.ACTION_UP -> {if(tool=="LÍNEA") canvas.drawLine(start.first,start.second,pos.first,pos.second,p); if(tool=="RECTÁNGULO") canvas.drawRect(min(start.first,pos.first),min(start.second,pos.second),max(start.first,pos.first),max(start.second,pos.second),p); parent.requestDisallowInterceptTouchEvent(false)} }; p.xfermode=null; invalidate(); return true
 }
}
