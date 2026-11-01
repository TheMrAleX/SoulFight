package com.fightforge.game
import android.content.Context
import android.graphics.*
import android.view.*
import com.fightforge.engine.*
import kotlin.math.*

class FightView(context: Context,val game: Combat,val ai: Ai,@Volatile var settings: Settings,val stage: Int,val audio: ProceduralAudio,val paused: ()->Unit,val finished: (Int)->Unit,val network: NetworkMatch?=null,val networkError: (String)->Unit): SurfaceView(context),SurfaceHolder.Callback,Runnable {
 @Volatile private var running=false
 @Volatile var suspended=false
  set(value){field=value; if(value){input=Input(); joystickId=-1; pointerPositions.clear(); joystickX=0f; joystickY=0f}}
 private var worker: Thread?=null
 private val pointerPositions=mutableMapOf<Int,Pair<Float,Float>>()
 @Volatile private var input=Input()
 private val previousX=FloatArray(2){game.fighters[it].x.toFloat()}; private val previousY=FloatArray(2){game.fighters[it].y.toFloat()}; private val trailingHp=FloatArray(2){game.fighters[it].hp.toFloat()}; @Volatile private var joystickX=0f; @Volatile private var joystickY=0f; private var joystickId=-1; private var uiScale=1f; private var notified=false; private var lastHp=game.fighters.sumOf { it.hp }
 private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
 private val images=object: android.util.LruCache<String,Bitmap>(32*1024*1024) {override fun sizeOf(key: String,value: Bitmap)=value.byteCount}
 private fun image(path: String): Bitmap? { images.get(path)?.let {return it}; val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}; BitmapFactory.decodeFile(path,bounds); var sample=1; while(bounds.outWidth/sample>1024 || bounds.outHeight/sample>1024) sample*=2; val decoded=BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {inSampleSize=sample}) ?: return null; images.put(path,decoded); return decoded }
 private fun customFrame(f: Fighter): Pair<Bitmap,AnimationSettings>? {
  val name=if(game.superPause>0 && game.fighters[game.cinematicOwner]===f && !f.move?.introAnimation.isNullOrEmpty()) f.move!!.introAnimation else f.move?.animation ?: MotionGeometry.animation(f.state,f.relativeMotion)
  val resolved=AnimationFallback.resolve(name,f.definition.animations.filterValues {it.isNotEmpty()}.keys)
  val frames=f.definition.animations[resolved] ?: return null
  if(frames.isEmpty()) return null
  val config=f.definition.animationSettings[resolved] ?: AnimationSettings()
  val elapsed=f.animationAge*config.fps/60
  val index=if(game.superPause>0 && name==f.move?.introAnimation) (60-game.superPause)*frames.size/60 else f.move?.let {f.attackFrame*frames.size/it.duration} ?: if(config.loop) elapsed%frames.size else elapsed.coerceAtMost(frames.lastIndex)
  val selected=if(f.state==State.CROUCH_UP && resolved=="agacharse") frames.lastIndex-index.coerceIn(frames.indices) else index.coerceIn(frames.indices)
  return image(frames[selected])?.let {it to config}
 }
 private val library=Storage(context).data
 private val customStage=network?.stageDef ?: library.stages.getOrNull(stage-3)
 private val customControls=listOf("Barra_vida","Barra_poder","Joystick_fondo","Joystick_mando","Boton_A","Boton_B","Boton_C","Boton_D").associateWith {name -> java.io.File(context.filesDir,"ui/$name.png").takeIf {it.exists()}?.absolutePath}
 private fun control(c: Canvas,name: String,x: Float,y: Float,size: Float): Boolean {val path=customControls[name] ?: return false; val bitmap=image(path) ?: return false; c.drawBitmap(bitmap,null,RectF(x-size/2,y-size/2,x+size/2,y+size/2),paint); return true}
 init { holder.addCallback(this); keepScreenOn=true; isFocusable=true }
 override fun surfaceCreated(holder: SurfaceHolder) { running=true; worker=Thread(this,"fightforge-render").also { it.start() } }
 override fun surfaceDestroyed(holder: SurfaceHolder) { running=false; worker?.join(2000); worker=null; pointerPositions.clear(); input=Input() }
 override fun surfaceChanged(holder: SurfaceHolder,format: Int,width: Int,height: Int) { uiScale=min(width/1920f,height/1080f) }
 override fun run() {
  var previous=System.nanoTime(); var accumulator=0L; val step=1_000_000_000L/60
  while(running) {
   if(suspended){previous=System.nanoTime(); accumulator=0L; Thread.sleep(16); continue}
   val now=System.nanoTime(); accumulator+=(now-previous).coerceAtMost(step*5); previous=now
   while(accumulator>=step) {
    val before=game.fighters.sumOf { it.hp }; val beforeTick=game.tick
    val dummy=when(game.dummy) { 1 -> Input(vertical=1); 2 -> Input(vertical=-1); 3 -> Input(horizontal=-game.fighters[1].facing); else -> Input() }
    for(i in 0..1) {previousX[i]=game.fighters[i].x.toFloat(); previousY[i]=game.fighters[i].y.toFloat()}
    try {
     if(network!=null) { val pair=network.exchange(input,game); game.step(pair.first,pair.second) }
     else game.step(input,if(game.practice) dummy else ai.input(game,1))
    } catch(e: Exception) { running=false; post {networkError(e.message ?: "Conexión perdida")}; break }
    accumulator-=step
    if(game.tick!=beforeTick && game.lastHitTick==game.tick) {audio.effect(if(game.lastHitBlocked) "block" else "hit",settings.effects); val target=game.fighters[game.lastHitTarget]; val attacker=game.fighters[1-game.lastHitTarget]; listOf(attacker.definition.hitEffect,target.definition.hurtEffect).distinct().forEach {id -> library.effectsLibrary.firstOrNull {it.id==id}?.let {audio.custom(it.sound,settings.effects)}}}
    if(game.superPause==60) audio.effect("ulti",settings.effects)
    if(game.fighters.any {it.hp==0} && game.fighters.sumOf {it.hp}<before) audio.effect("ko",settings.effects)
   }
   var canvas: Canvas?=null
   try { if(holder.surface.isValid) {canvas=holder.lockHardwareCanvas(); if(canvas!=null) render(canvas,accumulator.toFloat()/step)} } catch(e: IllegalArgumentException) {if(running) post {networkError("No se pudo dibujar la superficie")}} finally {if(canvas!=null && holder.surface.isValid) runCatching {holder.unlockCanvasAndPost(canvas)}}
   if(game.matchOver && !notified) { notified=true; post { finished(game.winner) } }
   val remaining=(step-accumulator-(System.nanoTime()-now)).coerceAtLeast(1_000_000)
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
   val frame=customFrame(f)
   if(frame==null) ProceduralSprites.fighter(c,renderX,renderY,f.definition.width.toFloat(),f.definition.height.toFloat(),f.definition.color,f.state,game.tick,f.facing,f.animationAge)
   else {val (bitmap,config)=frame; val w=if(config.canvasWidth>0) config.canvasWidth/2f else f.definition.width*1.15f; val h=if(config.canvasHeight>0) config.canvasHeight/2f else f.definition.height*1.15f; val left=if(config.canvasWidth>0) renderX+config.offsetX/2f else renderX-w/2; val top=if(config.canvasHeight>0) renderY+config.offsetY/2f else renderY-f.definition.height/2f-h/2; c.save(); c.scale(f.facing.toFloat(),1f,renderX,renderY); c.drawBitmap(bitmap,null,RectF(left,top,left+w,top+h),paint); c.restore()}

   if(settings.hitboxes) {
    paint.style=Paint.Style.STROKE; paint.strokeWidth=3f; paint.color=0xFF3DD9EB.toInt(); val b=f.body; c.drawRect(b.x.toFloat()/SCALE,b.y.toFloat()/SCALE,(b.x+b.w).toFloat()/SCALE,(b.y+b.h).toFloat()/SCALE,paint)
    f.move?.let { m -> if(f.attackFrame in m.startup until m.startup+m.active) { paint.color=0xFFE53935.toInt(); m.boxes.filter { it.kind==BoxKind.DAMAGE }.forEach { b -> val x=f.x.toFloat()/SCALE+if(f.facing>0) b.rect.x else -b.rect.x-b.rect.w; c.drawRect(x,f.y.toFloat()/SCALE+b.rect.y,x+b.rect.w,f.y.toFloat()/SCALE+b.rect.y+b.rect.h,paint) } } }; paint.style=Paint.Style.FILL
   }
  }
  paint.color=0xFF00BFA5.toInt(); for(p in game.spawns) { val bitmap=if(p.photo.isEmpty()) null else image(p.photo); val r=RectF(p.x.toFloat()/SCALE,p.y.toFloat()/SCALE,(p.x+p.w).toFloat()/SCALE,(p.y+p.h).toFloat()/SCALE); if(bitmap!=null) c.drawBitmap(bitmap,null,r,paint) else c.drawOval(r,paint) }
  if(game.tick-game.lastHitTick in 0..18) {
   val target=game.fighters[game.lastHitTarget]; val age=game.tick-game.lastHitTick
   val ids=listOf(target.definition.hurtEffect,game.fighters[1-game.lastHitTarget].definition.hitEffect).distinct()
   ids.forEach {id ->
    val effect=library.effectsLibrary.firstOrNull {it.id==id}
    val x=target.x.toFloat()/SCALE; val y=target.y.toFloat()/SCALE-target.definition.height*.6f
    if(effect!=null && effect.frames.isNotEmpty()) {val frame=age*effect.fps/60; if(frame<effect.frames.size) image(effect.frames[frame])?.let {bitmap -> val size=256f*effect.scale/100; paint.alpha=255; c.drawBitmap(bitmap,null,RectF(x-size/2,y-size/2,x+size/2,y+size/2),paint)}}
    else {paint.color=if(id=="shock") 0xFF3DD9EB.toInt() else if(id=="burst") 0xFFFF4D5E.toInt() else 0xFFFFC83D.toInt(); paint.strokeWidth=5f; paint.alpha=(255-age*12).coerceAtLeast(0); repeat(10){n -> val angle=n*Math.PI/5; val radius=20f+age*6; c.drawLine(x+cos(angle).toFloat()*radius*.5f,y+sin(angle).toFloat()*radius*.5f,x+cos(angle).toFloat()*radius,y+sin(angle).toFloat()*radius,paint)}; paint.alpha=255}
   }
  }
  if(game.tick-game.lastGrabClash in 0..10){paint.color=0xAAFFFFFF.toInt(); c.drawCircle(center,600f,(game.tick-game.lastGrabClash+1)*20f,paint)}
  c.restore()
  if(game.superPause>0) {paint.color=0x55FFC83D; c.drawRect(0f,0f,1920f,1080f,paint)}
  val d=dpUnit()
  val photo=56*d; val hpH=22*d; val powerH=14*d; val top=24*d
  val barW=minOf(1920f*.38f,960-88*d-28*d-12*d)
  for(i in 0..1) {
   val f=game.fighters[i]; val left=if(i==0) 88*d else 1920-88*d-barW; val right=left+barW
   val bottom=top+hpH; val powerTop=bottom+4*d; val powerBottom=powerTop+powerH
   val path=Path().apply {moveTo(left+8*d,top); lineTo(right,top); lineTo(right-8*d,bottom); lineTo(left,bottom); close()}
   c.save(); c.clipPath(path); paint.color=0xFF141B2D.toInt(); c.drawRect(left,top,right,bottom,paint)
   trailingHp[i]=if(trailingHp[i]<f.hp) f.hp.toFloat() else (trailingHp[i]-(trailingHp[i]-f.hp)*.045f).coerceAtLeast(f.hp.toFloat())
   val trail=trailingHp[i]/f.definition.stats.maxHp; paint.color=0xFFF2F5FF.toInt()
   c.drawRect(if(i==0) left else right-barW*trail,top,if(i==0) left+barW*trail else right,bottom,paint)
   val ratio=f.hp.toFloat()/f.definition.stats.maxHp
   paint.shader=LinearGradient(if(i==0) left else right,0f,if(i==0) right else left,0f,intArrayOf(0xFFFF3B4E.toInt(),0xFFFFC83D.toInt(),0xFF3DDC84.toInt()),floatArrayOf(0f,.5f,1f),Shader.TileMode.CLAMP)
   c.drawRect(if(i==0) left else right-barW*ratio,top,if(i==0) left+barW*ratio else right,bottom,paint); paint.shader=null
   customControls["Barra_vida"]?.let {p -> image(p)?.let {bitmap -> c.save(); c.clipRect(if(i==0) left else right-barW*ratio,top,if(i==0) left+barW*ratio else right,bottom); c.drawBitmap(bitmap,null,RectF(left,top,right,bottom),paint); c.restore()}}; c.restore()
   paint.color=0xFF1E2742.toInt(); c.drawRect(left,powerTop,right,powerBottom,paint)
   val energy=f.power/3000f; paint.color=0xFF3DD9EB.toInt(); val energyLeft=if(i==0) left else right-barW*energy; val energyRight=if(i==0) left+barW*energy else right
   c.drawRect(energyLeft,powerTop,energyRight,powerBottom,paint)
   customControls["Barra_poder"]?.let {p -> image(p)?.let {bitmap -> c.save(); c.clipRect(energyLeft,powerTop,energyRight,powerBottom); c.drawBitmap(bitmap,null,RectF(left,powerTop,right,powerBottom),paint); c.restore()}}
   paint.color=0xFFF2F5FF.toInt(); paint.strokeWidth=d; for(mark in listOf(.5f,1f)) c.drawLine(left+barW*mark,powerTop,left+barW*mark,powerBottom,paint)
   paint.textSize=12*d; paint.typeface=Typeface.create("sans-serif-condensed",Typeface.BOLD)
   paint.textAlign=if(i==0) Paint.Align.LEFT else Paint.Align.RIGHT
   c.drawText(f.definition.name.uppercase(),if(i==0) left else right,powerBottom+16*d,paint)
   paint.textAlign=if(i==0) Paint.Align.RIGHT else Paint.Align.LEFT
   c.drawText("${f.hp} / ${f.definition.stats.maxHp}",if(i==0) right else left,powerBottom+16*d,paint)
   val px=if(i==0) 24*d else 1920-80*d; val portrait=RectF(px,top,px+photo,top+photo)
   paint.color=0xFF1E2742.toInt(); c.drawRoundRect(portrait,4*d,4*d,paint)
   val profile=f.definition.animations["foto"]?.firstOrNull()?.let {image(it)}
   if(profile!=null) c.drawBitmap(profile,null,portrait,paint) else ProceduralSprites.fighter(c,px+photo/2,top+photo,photo*.4f,photo*.92f,f.definition.color,State.IDLE,0,if(i==0) 1 else -1)
   paint.color=0xFF3DD9EB.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=2*d; c.drawRect(portrait,paint); paint.style=Paint.Style.FILL
   if(f.combo>1){paint.color=0xFFFFC83D.toInt(); val age=game.tick-game.lastHitTick; paint.textSize=20*d*(1+.25f*(1-age/12f).coerceIn(0f,1f)); paint.textAlign=if(i==0) Paint.Align.LEFT else Paint.Align.RIGHT; c.drawText("${f.combo} HITS",if(i==0) left else right,112*d,paint)}
  }
  paint.color=0xFF1E2742.toInt(); c.drawCircle(960f,52*d,28*d,paint); paint.color=0xFFF2F5FF.toInt(); paint.textSize=26*d; paint.textAlign=Paint.Align.CENTER
  c.drawText(if(game.practice) "∞" else game.seconds.toString(),960f,61*d,paint)
  for(side in 0..1) repeat(2){n -> paint.color=if(n<game.wins[side]) 0xFFFFC83D.toInt() else 0xFF2A3556.toInt(); c.drawCircle(960+(if(side==0) -1 else 1)*(12+n*12)*d,92*d,4*d,paint)}
  paint.color=0xFFF2F5FF.toInt(); paint.textSize=22*d; c.drawText("Ⅱ",960f,132*d,paint)
  paint.alpha=settings.opacity*255/100; paint.color=0xFF3DD9EB.toInt(); paint.style=Paint.Style.STROKE; paint.strokeWidth=2*d
  val j=joystickCenter(); if(!control(c,"Joystick_fondo",j.first,j.second,140*d)) c.drawCircle(j.first,j.second,70*d,paint); paint.style=Paint.Style.FILL
  if(!control(c,"Joystick_mando",j.first+joystickX,j.second+joystickY,64*d)) c.drawCircle(j.first+joystickX,j.second+joystickY,32*d,paint)
  val centers=buttonCenters(); val colors=intArrayOf(0xFFFF4D5E.toInt(),0xFF3DD9EB.toInt(),0xFF3DDC84.toInt(),0xFFFFC83D.toInt())
  for(i in 0..3){val (x,y)=centers[i]; paint.color=colors[i]; paint.alpha=if(input.buttons and (1 shl i)!=0) 255 else settings.opacity*255/100; if(!control(c,"Boton_${'A'+i}",x,y,68*d)) c.drawCircle(x,y,34*d,paint); paint.alpha=255; paint.color=0xFF0B0F1A.toInt(); paint.textSize=24*d; c.drawText(('A'+i).toString(),x,y+8*d,paint)}
  paint.alpha=255; paint.textAlign=Paint.Align.LEFT
  if(settings.inputs) { paint.color=0xFFF2F5FF.toInt(); paint.textSize=22f; c.drawText("${input.horizontal},${input.vertical} / ${input.buttons}",40f,650f,paint) }
  if(game.roundOver) { paint.color=0xBB0B0F1A.toInt(); c.drawRect(500f,340f,1420f,650f,paint); paint.color=0xFFFFC83D.toInt(); paint.textAlign=Paint.Align.CENTER; paint.textSize=66f; c.drawText(if(game.winner<0) "EMPATE" else "${game.fighters[game.winner].definition.name.uppercase()} GANA",960f,460f,paint); paint.textSize=28f; c.drawText(if(game.matchOver) "FIN DEL COMBATE" else "TOCA PARA LA SIGUIENTE RONDA",960f,550f,paint); paint.textAlign=Paint.Align.LEFT }
  c.restore()
 }
 private fun dpUnit()=resources.displayMetrics.density/uiScale
 private fun joystickCenter()=144*dpUnit() to 1080-144*dpUnit()
 private fun buttonCenters(): List<Pair<Float,Float>> {val d=dpUnit(); val offset=76*d/sqrt(2f); val bx=1920-58*d; val cy=1080-58*d-offset; val cx=bx-offset; return listOf(cx to cy+offset,bx to cy,cx-offset to cy,cx to cy-offset)}
 internal fun sampledInput()=input
 override fun onTouchEvent(event: MotionEvent): Boolean {
  val offsetX=(width-1920*uiScale)/2; val offsetY=(height-1080*uiScale)/2
  fun position(i: Int)=((event.getX(i)-offsetX)/uiScale) to ((event.getY(i)-offsetY)/uiScale)
  when(event.actionMasked) {
   MotionEvent.ACTION_DOWN,MotionEvent.ACTION_POINTER_DOWN -> {
    val id=event.getPointerId(event.actionIndex); val p=position(event.actionIndex)
    if(abs(p.first-960)<24*dpUnit() && abs(p.second-124*dpUnit())<24*dpUnit()) { paused(); return true }
    if(game.roundOver) { if(!game.matchOver) {if(network==null) game.nextRound() else network.nextRoundRequested=true}; return true }
    pointerPositions[id]=p
    if(p.first in 24*dpUnit()..264*dpUnit() && p.second in 1080-264*dpUnit()..1080-24*dpUnit() && joystickId<0) joystickId=id
   }
   MotionEvent.ACTION_MOVE -> for(i in 0 until event.pointerCount) { val id=event.getPointerId(i); if(id in pointerPositions) pointerPositions[id]=position(i) }
   MotionEvent.ACTION_UP,MotionEvent.ACTION_POINTER_UP -> { val id=event.getPointerId(event.actionIndex); pointerPositions.remove(id); if(id==joystickId) joystickId=-1 }
   MotionEvent.ACTION_CANCEL -> { pointerPositions.clear(); joystickId=-1 }
  }
  val center=joystickCenter(); val radius=60*dpUnit()
  val point=pointerPositions[joystickId]
  if(point!=null){val dx=point.first-center.first; val dy=point.second-center.second; val length=hypot(dx,dy); val scale=if(length>radius) radius/length else 1f; joystickX=dx*scale; joystickY=dy*scale}else{joystickX=0f; joystickY=0f}
  var next=point?.let {Joystick.absolute(it.first.toInt(),it.second.toInt(),center.first.toInt(),center.second.toInt(),radius.toInt(),input)} ?: Input()
  var mask=0
  for((id,p) in pointerPositions) if(id!=joystickId) {val centers=buttonCenters(); val i=centers.indices.minBy {hypot(p.first-centers[it].first,p.second-centers[it].second)}; if(hypot(p.first-centers[i].first,p.second-centers[i].second)<42*dpUnit()) mask=mask or (1 shl i)}
  next=next.copy(buttons=mask); if(settings.vibration && mask!=0 && mask!=input.buttons) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); input=next
  return true
 }
}
