package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.*
import com.fightforge.engine.*
import kotlin.math.*

class FightView(context: Context,val game: Combat,val ai: Ai,val settings: Settings,val stage: Int,val audio: ProceduralAudio,val paused: ()->Unit,val finished: (Int)->Unit): SurfaceView(context),SurfaceHolder.Callback,Runnable {
 @Volatile private var running=false
 private var worker: Thread?=null
 private val pointerPositions=mutableMapOf<Int,Pair<Float,Float>>()
 @Volatile private var input=Input()
 private var joystickId=-1; private var uiScale=1f; private var notified=false; private var lastHp=game.fighters.sumOf { it.hp }
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 init { holder.addCallback(this); keepScreenOn=true; isFocusable=true }
 override fun surfaceCreated(holder: SurfaceHolder) { running=true; worker=Thread(this,"fightforge-render").also { it.start() } }
 override fun surfaceDestroyed(holder: SurfaceHolder) { running=false; worker?.join(2000); worker=null; pointerPositions.clear(); input=Input() }
 override fun surfaceChanged(holder: SurfaceHolder,format: Int,width: Int,height: Int) { uiScale=min(width/1920f,height/1080f) }
 override fun run() {
  var previous=System.nanoTime(); var accumulator=0L; val step=1_000_000_000L/60
  while(running) {
   val now=System.nanoTime(); accumulator+=(now-previous).coerceAtMost(step*5); previous=now
   while(accumulator>=step) {
    val before=game.fighters.sumOf { it.hp }
    val dummy=when(game.dummy) { 1 -> Input(vertical=1); 2 -> Input(vertical=-1); 3 -> Input(horizontal=-game.fighters[1].facing); else -> Input() }
    game.step(input,if(game.practice) dummy else ai.input(game,1)); accumulator-=step
    if(game.fighters.sumOf { it.hp }<before) audio.effect("hit",settings.effects)
   }
   var canvas: Canvas?=null
   try { canvas=holder.lockCanvas(); if(canvas!=null) render(canvas) } finally { if(canvas!=null) holder.unlockCanvasAndPost(canvas) }
   if(game.matchOver && !notified) { notified=true; post { finished(game.winner) } }
   Thread.sleep(2)
  }
 }
 private fun render(c: Canvas) {
  c.drawColor(0xFF0B0F1A.toInt()); c.save(); c.translate((width-1920*uiScale)/2,(height-1080*uiScale)/2); c.scale(uiScale,uiScale)
  ProceduralSprites.stage(c,1920f,1080f,game.tick,stage)
  val center=(game.fighters[0].x+game.fighters[1].x)/2f/SCALE
  val camera=(center-960).coerceIn(0f,1280f)
  c.save(); c.translate(-camera,0f)
  for(f in game.fighters) {
   ProceduralSprites.fighter(c,f.x.toFloat()/SCALE,f.y.toFloat()/SCALE,f.definition.width.toFloat(),f.definition.height.toFloat(),f.definition.color,f.state,game.tick,f.facing)
   if(settings.hitboxes || game.practice) {
    paint.style=Paint.Style.STROKE; paint.strokeWidth=3f; paint.color=0xFF3DD9EB.toInt(); val b=f.body; c.drawRect(b.x.toFloat()/SCALE,b.y.toFloat()/SCALE,(b.x+b.w).toFloat()/SCALE,(b.y+b.h).toFloat()/SCALE,paint)
    f.move?.let { m -> if(f.attackFrame in m.startup until m.startup+m.active) { paint.color=0xFFE53935.toInt(); m.boxes.filter { it.kind==BoxKind.DAMAGE }.forEach { b -> val x=f.x.toFloat()/SCALE+if(f.facing>0) b.rect.x else -b.rect.x-b.rect.w; c.drawRect(x,f.y.toFloat()/SCALE+b.rect.y,x+b.rect.w,f.y.toFloat()/SCALE+b.rect.y+b.rect.h,paint) } } }; paint.style=Paint.Style.FILL
   }
  }
  paint.color=0xFF00BFA5.toInt(); for(p in game.spawns) c.drawOval(p.x.toFloat()/SCALE,p.y.toFloat()/SCALE,(p.x+p.w).toFloat()/SCALE,(p.y+p.h).toFloat()/SCALE,paint)
  c.restore()
  for(i in 0..1) {
   val f=game.fighters[i]; val left=if(i==0) 160f else 1120f; val right=left+640f
   paint.color=0xFF141B2D.toInt(); c.drawRect(left,42f,right,84f,paint)
   val ratio=f.hp.toFloat()/f.definition.stats.maxHp; paint.shader=LinearGradient(left,0f,right,0f,0xFFFF3B4E.toInt(),0xFF3DDC84.toInt(),Shader.TileMode.CLAMP)
   if(i==0) c.drawRect(left,42f,left+640*ratio,84f,paint) else c.drawRect(right-640*ratio,42f,right,84f,paint)
   paint.shader=null; paint.color=0xFF1E2742.toInt(); c.drawRect(left,92f,right,112f,paint); paint.color=0xFF3DD9EB.toInt(); c.drawRect(left,92f,left+640*f.power/3000f,112f,paint)
   paint.color=0xFFF2F5FF.toInt(); paint.textSize=26f; paint.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD); c.drawText(f.definition.name.uppercase(),left,145f,paint)
   c.drawText("${f.hp} / ${f.definition.stats.maxHp}",left+350,145f,paint)
   val portrait=if(i==0) 88f else 1830f; paint.color=0xFF1E2742.toInt(); c.drawRoundRect(portrait-50,35f,portrait+50,135f,8f,8f,paint); ProceduralSprites.fighter(c,portrait,128f,35f,85f,f.definition.color,State.IDLE,0,if(i==0) 1 else -1)
   if(f.combo>1) { paint.color=0xFFFFC83D.toInt(); paint.textSize=38f; c.drawText("${f.combo} HITS",left,225f,paint) }
  }
  paint.color=0xFF1E2742.toInt(); c.drawCircle(960f,78f,60f,paint); paint.color=0xFFF2F5FF.toInt(); paint.textSize=44f; paint.textAlign=Paint.Align.CENTER; c.drawText(if(game.practice) "∞" else game.seconds.toString(),960f,94f,paint)
  paint.textSize=24f; c.drawText("${game.wins[0]}  •  ${game.wins[1]}",960f,170f,paint); c.drawText("Ⅱ",960f,220f,paint)
  paint.alpha=settings.opacity*255/100; paint.color=0xFF3DD9EB.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=6f; c.drawCircle(240f,840f,150f,paint); paint.style=Paint.Style.FILL; c.drawCircle(240f+input.horizontal*95,840f+input.vertical*95,58f,paint)
  val centers=buttonCenters(); for(i in 0..3) { paint.color=ProceduralSprites.colors[i]; paint.alpha=if(input.buttons and (1 shl i)!=0) 255 else settings.opacity*255/100; val (x,y)=centers[i]; c.drawCircle(x,y,68f,paint); paint.color=0xFF0B0F1A.toInt(); paint.alpha=255; paint.textSize=44f; c.drawText(('A'+i).toString(),x,y+16,paint) }
  paint.alpha=255; paint.textAlign=Paint.Align.LEFT
  if(settings.inputs) { paint.color=0xFFF2F5FF.toInt(); paint.textSize=22f; c.drawText("${input.horizontal},${input.vertical} / ${input.buttons}",40f,650f,paint) }
  if(game.roundOver) { paint.color=0xBB0B0F1A.toInt(); c.drawRect(500f,340f,1420f,650f,paint); paint.color=0xFFFFC83D.toInt(); paint.textAlign=Paint.Align.CENTER; paint.textSize=66f; c.drawText(if(game.winner<0) "EMPATE" else "${game.fighters[game.winner].definition.name.uppercase()} GANA",960f,460f,paint); paint.textSize=28f; c.drawText(if(game.matchOver) "FIN DEL COMBATE" else "TOCA PARA LA SIGUIENTE RONDA",960f,550f,paint); paint.textAlign=Paint.Align.LEFT }
  c.restore()
 }
 private fun buttonCenters()=listOf(1640f to 940f,1770f to 810f,1510f to 810f,1640f to 680f)
 override fun onTouchEvent(event: MotionEvent): Boolean {
  val offsetX=(width-1920*uiScale)/2; val offsetY=(height-1080*uiScale)/2
  fun position(i: Int)=((event.getX(i)-offsetX)/uiScale) to ((event.getY(i)-offsetY)/uiScale)
  when(event.actionMasked) {
   MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN -> {
    val id=event.getPointerId(event.actionIndex); val p=position(event.actionIndex)
    if(p.first in 870f..1050f && p.second in 175f..270f) { paused(); return true }
    if(game.roundOver) { if(!game.matchOver) game.nextRound(); return true }
    pointerPositions[id]=p
    if(p.first in 0f..520f && p.second in 550f..1080f && joystickId<0) joystickId=id
   }
   MotionEvent.ACTION_MOVE -> for(i in 0 until event.pointerCount) { val id=event.getPointerId(i); if(id in pointerPositions) pointerPositions[id]=position(i) }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP -> { val id=event.getPointerId(event.actionIndex); pointerPositions.remove(id); if(id==joystickId) joystickId=-1 }
   MotionEvent.ACTION_CANCEL -> { pointerPositions.clear(); joystickId=-1 }
  }
  var next=pointerPositions[joystickId]?.let { Joystick.absolute(it.first.toInt(),it.second.toInt(),240,840,150) } ?: Input()
  var mask=0
  for((id,p) in pointerPositions) if(id!=joystickId) buttonCenters().forEachIndexed { i,b -> if(hypot(p.first-b.first,p.second-b.second)<88f) mask=mask or (1 shl i) }
  next=next.copy(buttons=mask); input=next
  return true
 }
}
