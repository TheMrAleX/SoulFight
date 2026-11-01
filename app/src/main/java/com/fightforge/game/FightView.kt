package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.*
import com.fightforge.engine.*
import kotlin.math.*

class FightView(context: Context,val game: Combat,val ai: Ai,val settings: Settings,val stage: Int,val audio: ProceduralAudio,val paused: ()->Unit,val finished: (Int)->Unit,val network: NetworkMatch?=null,val networkError: (String)->Unit={}): SurfaceView(context),SurfaceHolder.Callback,Runnable {
 @Volatile private var running=false
 private var worker: Thread?=null
 private val pointerPositions=mutableMapOf<Int,Pair<Float,Float>>()
 @Volatile private var input=Input()
 private val previousX=FloatArray(2){game.fighters[it].x.toFloat()}; private val previousY=FloatArray(2){game.fighters[it].y.toFloat()}; private val trailingHp=FloatArray(2){game.fighters[it].hp.toFloat()}; private var joystickId=-1; private var uiScale=1f; private var notified=false; private var lastHp=game.fighters.sumOf { it.hp }
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private val images=object: android.util.LruCache<String,Bitmap>(32*1024*1024) {override fun sizeOf(key: String,value: Bitmap)=value.byteCount}
 private fun image(path: String): Bitmap? { images.get(path)?.let {return it}; val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}; BitmapFactory.decodeFile(path,bounds); var sample=1; while(bounds.outWidth/sample>1024 || bounds.outHeight/sample>1024) sample*=2; val decoded=BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {inSampleSize=sample}) ?: return null; images.put(path,decoded); return decoded }
 private fun customFrame(f: Fighter): Bitmap? {
  val name=if(game.superPause>0 && game.fighters[game.cinematicOwner]===f && !f.move?.introAnimation.isNullOrEmpty()) f.move!!.introAnimation else f.move?.animation ?: when(f.state) {State.WALK -> "caminar adelante"; State.RUN -> "correr adelante"; State.CROUCH,State.BLOCK_CROUCH -> "agachado quieto"; State.JUMP -> "salto"; State.FALL -> "caída"; State.HURT -> "daño"; State.KO -> "K.O."; State.DOWN -> "tumbado"; State.GETUP -> "levantarse"; State.DIZZY -> "mareado"; else -> "quieto"}
  val resolved=AnimationFallback.resolve(name,f.definition.animations.filterValues {it.isNotEmpty()}.keys)
  val frames=f.definition.animations[resolved] ?: return null
  if(frames.isEmpty()) return null
  val config=f.definition.animationSettings[resolved] ?: AnimationSettings()
  val elapsed=game.tick*config.fps/60
  val index=f.move?.let {f.attackFrame*frames.size/it.duration} ?: if(config.loop) elapsed%frames.size else elapsed.coerceAtMost(frames.lastIndex)
  return image(frames[index.coerceIn(0,frames.lastIndex)])
 }
 private val customStage=runCatching {Storage(context).data.stages.getOrNull(stage-3)}.getOrNull()
 private val customControls=listOf("Joystick_fondo","Joystick_mando","Boton_A","Boton_B","Boton_C","Boton_D").associateWith {name -> java.io.File(context.filesDir,"ui/$name.png").takeIf {it.exists()}?.absolutePath}
 private fun control(c: Canvas,name: String,x: Float,y: Float,size: Float): Boolean {val path=customControls[name] ?: return false; val bitmap=image(path) ?: return false; c.drawBitmap(bitmap,null,RectF(x-size/2,y-size/2,x+size/2,y+size/2),paint); return true}
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
    for(i in 0..1) {previousX[i]=game.fighters[i].x.toFloat(); previousY[i]=game.fighters[i].y.toFloat()}
    try {
     if(network!=null) { val pair=network.exchange(input,game); game.step(pair.first,pair.second) }
     else game.step(input,if(game.practice) dummy else ai.input(game,1))
    } catch(e: Exception) { running=false; post {networkError(e.message ?: "Conexión perdida")}; break }
    accumulator-=step
    if(game.fighters.sumOf { it.hp }<before) audio.effect("hit",settings.effects)
   }
   var canvas: Canvas?=null
   try { canvas=holder.lockCanvas(); if(canvas!=null) render(canvas,accumulator.toFloat()/step) } finally { if(canvas!=null) holder.unlockCanvasAndPost(canvas) }
   if(game.matchOver && !notified) { notified=true; post { finished(game.winner) } }
   val remaining=(step-accumulator).coerceAtLeast(1_000_000)
   Thread.sleep((remaining/1_000_000).coerceIn(1,16))
  }
 }
 private fun render(c: Canvas,alpha: Float) {
  c.drawColor(0xFF0B0F1A.toInt()); c.save(); c.translate((width-1920*uiScale)/2,(height-1080*uiScale)/2); c.scale(uiScale,uiScale)
  ProceduralSprites.stage(c,1920f,1080f,game.tick,stage%3)
  customStage?.let {s -> if(s.frames.isNotEmpty()) image(s.frames[(game.tick*s.fps/60)%s.frames.size])?.let {bitmap -> c.drawBitmap(bitmap,null,RectF(0f,0f,1920f,1080f),paint)} }
  val center=(game.fighters[0].x+game.fighters[1].x)/2f/SCALE
  val camera=(center-960).coerceIn(0f,1280f)
  val separation=abs(game.fighters[0].x-game.fighters[1].x).toFloat()/SCALE
  val zoom=(1920f/(separation+600f)).coerceIn(.55f,1.0f)
  c.save(); c.scale(zoom,zoom,960f,920f); c.translate(-camera,0f)
  for((index,f) in game.fighters.withIndex()) {
   val renderX=(previousX[index]+(f.x-previousX[index])*alpha)/SCALE
   val renderY=(previousY[index]+(f.y-previousY[index])*alpha)/SCALE
   val bitmap=customFrame(f)
   if(bitmap==null) ProceduralSprites.fighter(c,renderX,renderY,f.definition.width.toFloat(),f.definition.height.toFloat(),f.definition.color,f.state,game.tick,f.facing)
   else {val w=f.definition.width*1.15f; val h=f.definition.height*1.15f; val x=renderX; val y=renderY-f.definition.height/2f; c.save(); c.scale(f.facing.toFloat(),1f,x,y); c.drawBitmap(bitmap,null,RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint); c.restore()}
   if(settings.hitboxes || game.practice) {
    paint.style=Paint.Style.STROKE; paint.strokeWidth=3f; paint.color=0xFF3DD9EB.toInt(); val b=f.body; c.drawRect(b.x.toFloat()/SCALE,b.y.toFloat()/SCALE,(b.x+b.w).toFloat()/SCALE,(b.y+b.h).toFloat()/SCALE,paint)
    f.move?.let { m -> if(f.attackFrame in m.startup until m.startup+m.active) { paint.color=0xFFE53935.toInt(); m.boxes.filter { it.kind==BoxKind.DAMAGE }.forEach { b -> val x=f.x.toFloat()/SCALE+if(f.facing>0) b.rect.x else -b.rect.x-b.rect.w; c.drawRect(x,f.y.toFloat()/SCALE+b.rect.y,x+b.rect.w,f.y.toFloat()/SCALE+b.rect.y+b.rect.h,paint) } } }; paint.style=Paint.Style.FILL
   }
  }
  paint.color=0xFF00BFA5.toInt(); for(p in game.spawns) { val bitmap=if(p.photo.isEmpty()) null else image(p.photo); val r=RectF(p.x.toFloat()/SCALE,p.y.toFloat()/SCALE,(p.x+p.w).toFloat()/SCALE,(p.y+p.h).toFloat()/SCALE); if(bitmap!=null) c.drawBitmap(bitmap,null,r,paint) else c.drawOval(r,paint) }
  c.restore()
  if(game.superPause>0) {paint.color=0x55FFC83D; c.drawRect(0f,0f,1920f,1080f,paint)}
  for(i in 0..1) {
   val f=game.fighters[i]; val left=if(i==0) 160f else 1120f; val right=left+640f
   paint.color=0xFF141B2D.toInt(); c.drawRect(left,42f,right,84f,paint)
   trailingHp[i]=if(trailingHp[i]<f.hp) f.hp.toFloat() else (trailingHp[i]-(trailingHp[i]-f.hp)*.045f).coerceAtLeast(f.hp.toFloat())
   val trail=trailingHp[i]/f.definition.stats.maxHp
   paint.color=0xFFF2F5FF.toInt(); if(i==0) c.drawRect(left,42f,left+640*trail,84f,paint) else c.drawRect(right-640*trail,42f,right,84f,paint)
   val ratio=f.hp.toFloat()/f.definition.stats.maxHp; paint.shader=LinearGradient(if(i==0) left else right,0f,if(i==0) right else left,0f,intArrayOf(0xFFFF3B4E.toInt(),0xFFFFC83D.toInt(),0xFF3DDC84.toInt()),floatArrayOf(0f,.5f,1f),Shader.TileMode.CLAMP)
   if(i==0) c.drawRect(left,42f,left+640*ratio,84f,paint) else c.drawRect(right-640*ratio,42f,right,84f,paint)
   paint.shader=null; paint.color=0xFF1E2742.toInt(); c.drawRect(left,92f,right,112f,paint); paint.color=0xFF3DD9EB.toInt(); c.drawRect(left,92f,left+640*f.power/3000f,112f,paint)
   paint.color=0xFFF2F5FF.toInt(); paint.textSize=26f; paint.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD); c.drawText(f.definition.name.uppercase(),left,145f,paint)
   c.drawText("${f.hp} / ${f.definition.stats.maxHp}",left+350,145f,paint)
   val portrait=if(i==0) 88f else 1830f; paint.color=0xFF1E2742.toInt(); c.drawRoundRect(portrait-50,35f,portrait+50,135f,8f,8f,paint); val profile=f.definition.animations["foto"]?.firstOrNull()?.let {image(it)}
   if(profile!=null) c.drawBitmap(profile,null,RectF(portrait-48,37f,portrait+48,133f),paint) else ProceduralSprites.fighter(c,portrait,128f,35f,85f,f.definition.color,State.IDLE,0,if(i==0) 1 else -1)
   if(f.combo>1) { paint.color=0xFFFFC83D.toInt(); paint.textSize=38f; c.drawText("${f.combo} HITS",left,225f,paint) }
  }
  paint.color=0xFF1E2742.toInt(); c.drawCircle(960f,78f,60f,paint); paint.color=0xFFF2F5FF.toInt(); paint.textSize=44f; paint.textAlign=Paint.Align.CENTER; c.drawText(if(game.practice) "∞" else game.seconds.toString(),960f,94f,paint)
  paint.textSize=24f; c.drawText("${game.wins[0]}  •  ${game.wins[1]}",960f,170f,paint); c.drawText("Ⅱ",960f,220f,paint)
  paint.alpha=settings.opacity*255/100; paint.color=0xFF3DD9EB.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=6f; if(!control(c,"Joystick_fondo",240f,840f,300f)) c.drawCircle(240f,840f,150f,paint); paint.style=Paint.Style.FILL; if(!control(c,"Joystick_mando",240f+input.horizontal*95,840f+input.vertical*95,116f)) c.drawCircle(240f+input.horizontal*95,840f+input.vertical*95,58f,paint)
  val centers=buttonCenters(); for(i in 0..3) { paint.color=ProceduralSprites.colors[i]; paint.alpha=if(input.buttons and (1 shl i)!=0) 255 else settings.opacity*255/100; val (x,y)=centers[i]; if(!control(c,"Boton_${'A'+i}",x,y,136f)) c.drawCircle(x,y,68f,paint); paint.color=0xFF0B0F1A.toInt(); paint.alpha=255; paint.textSize=44f; c.drawText(('A'+i).toString(),x,y+16,paint) }
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
    if(game.roundOver) { if(!game.matchOver) {if(network==null) game.nextRound() else network.nextRoundRequested=true}; return true }
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
  next=next.copy(buttons=mask); if(settings.vibration && mask!=0 && mask!=input.buttons) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); input=next
  return true
 }
}
