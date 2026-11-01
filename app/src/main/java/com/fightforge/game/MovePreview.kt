package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
import kotlinx.coroutines.delay
@Composable fun MovePreview(f: FighterDef,m: Move,back: ()->Unit) {
 var tick by remember {mutableIntStateOf(0)}
 LaunchedEffect(Unit){while(true){delay(1000L/60); tick++}}
 BackHandler {back()}
 AndroidView(factory={MovePreviewView(it,f,m)},update={it.tick=tick; it.invalidate()},modifier=Modifier.fillMaxSize().clickable {back()})
}
class MovePreviewView(context: Context,val f: FighterDef,val m: Move): View(context) {
 var tick=0
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private val paths=f.animations[m.animation].orEmpty(); private var loaded=-1; private var frame: Bitmap?=null
 private fun image(index: Int): Bitmap? {if(index!=loaded){frame?.recycle(); frame=BitmapFactory.decodeFile(paths[index],BitmapFactory.Options().apply {inSampleSize=2}); loaded=index}; return frame}
 override fun onDraw(c: Canvas) {
  ProceduralSprites.stage(c,width.toFloat(),height.toFloat(),tick,0)
  val s=minOf(width/1600f,height/1000f); c.save(); c.translate(width*.35f,height*.9f); c.scale(s,s)
  val phase=tick%(m.duration+30); val progress=((phase-m.startup).toFloat()/m.active).coerceIn(0f,1f)
  val motion=m.boxes.firstOrNull {it.kind==BoxKind.MOVEMENT && it.entity=="player"}
  val delta=motion?.let {MotionGeometry.displacement(it.rect,MotionGeometry.body(f.width,f.height))} ?: (0 to 0)
  val t=if(motion?.returnTrip==true) 1-kotlin.math.abs(progress*2-1) else progress
  val teleport=m.boxes.firstOrNull {it.kind==BoxKind.TELEPORT && !it.rival}
  val x=if(teleport!=null && phase>=m.startup) teleport.rect.x+teleport.rect.w/2f else delta.first*t
  val y=if(teleport!=null && phase>=m.startup) teleport.rect.y+teleport.rect.h.toFloat() else delta.second*t
  ProceduralSprites.fighter(c,500f,0f,200f,400f,1,com.fightforge.engine.State.IDLE,tick,-1)
  if(paths.isEmpty()) ProceduralSprites.fighter(c,x,y,f.width.toFloat(),f.height.toFloat(),f.color,if(phase<m.duration) com.fightforge.engine.State.ATTACK else com.fightforge.engine.State.IDLE,tick,1)
  else image((phase.coerceAtMost(m.duration-1)*paths.size/m.duration).coerceIn(paths.indices))?.let {b -> val config=f.animationSettings[m.animation]; val rawWidth=config?.canvasWidth?.takeIf {it>0} ?: (f.width*1.15f); val rawHeight=config?.canvasHeight?.takeIf {it>0} ?: (f.height*1.15f); val left=config?.offsetX?.takeIf {config.canvasWidth>0}?.toFloat() ?: (-f.width*.575f); val top=config?.offsetY?.takeIf {config.canvasHeight>0}?.toFloat() ?: (-f.height*1.075f); c.drawBitmap(b,null,RectF(x+left,y+top,x+left+rawWidth.toFloat(),y+top+rawHeight.toFloat()),paint)}
  paint.style=Paint.Style.STROKE; paint.strokeWidth=4f
  m.boxes.forEach {box -> paint.color=HitboxView.colors[box.kind.ordinal]; paint.alpha=if(box.kind==BoxKind.DAMAGE && phase !in m.startup until m.startup+m.active) 50 else 255; val r=box.rect; c.drawRect(x+r.x,y+r.y,x+r.x+r.w,y+r.y+r.h,paint)}
  paint.style=Paint.Style.FILL; paint.alpha=255; c.restore()
 }
 override fun onDetachedFromWindow(){super.onDetachedFromWindow(); frame?.recycle()}
}
