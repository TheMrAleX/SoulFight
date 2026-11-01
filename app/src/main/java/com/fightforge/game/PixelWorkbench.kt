package com.fightforge.game

import android.graphics.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay

@Composable fun PixelWorkbench(store: Storage,f: FighterDef,slot: String,save: (List<String>,Int)->Unit,back: ()->Unit) {
 val geometry=TemplateGeometry.cached(f.width,f.height)
 val rawWidth=if(slot=="foto") 256 else f.animationSettings[slot]?.canvasWidth?.takeIf {it>0} ?: geometry.pixelsWide
 val rawHeight=if(slot=="foto") 256 else f.animationSettings[slot]?.canvasHeight?.takeIf {it>0} ?: geometry.pixelsHigh
 val fit=minOf(1f,4096f/rawWidth,4096f/rawHeight); val width=(rawWidth*fit).toInt().coerceAtLeast(1); val height=(rawHeight*fit).toInt().coerceAtLeast(1)
 val config=f.animationSettings[slot]
 val bodyGuide=if(config!=null && config.canvasWidth>0){com.fightforge.engine.Rect(((-f.width-config.offsetX)*fit).toInt(),((-2*f.height-config.offsetY)*fit).toInt(),(2*f.width*fit).toInt(),(2*f.height*fit).toInt())} else geometry.body
 val directory=remember {File(store.context.filesDir,"characters/${f.id}/animations/${slot.hashCode()}").apply {mkdirs()}}
 fun blank(): Bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
 fun read(path: String): Bitmap=BitmapFactory.decodeFile(path)?.let {original -> if(original.width==width && original.height==height) original.copy(Bitmap.Config.ARGB_8888,true).also {original.recycle()} else Bitmap.createScaledBitmap(original,width,height,true).also {if(it!==original) original.recycle()}} ?: blank()
 fun persist(bitmap: Bitmap): String {val file=File(directory,"${UUID.randomUUID()}.png"); file.outputStream().use {check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}; return file.absolutePath}
 var paths by remember {mutableStateOf(f.animations[slot].orEmpty().take(if(slot=="foto") 1 else 24).ifEmpty {listOf(blank().let {b -> persist(b).also {b.recycle()}})})}
 var index by remember {mutableIntStateOf(0)}
 var bitmap by remember {mutableStateOf(read(paths.first()))}
 var tool by remember {mutableStateOf("LÁPIZ")}; var color by remember {mutableIntStateOf(Color.WHITE)}; var brush by remember {mutableIntStateOf(4)}
 var hue by remember {mutableFloatStateOf(0f)}; var saturation by remember {mutableFloatStateOf(1f)}; var brightness by remember {mutableFloatStateOf(1f)}
 var onion by remember {mutableStateOf(true)}; var guide by remember {mutableStateOf(true)}; var playing by remember {mutableStateOf(false)}; var fps by remember {mutableIntStateOf(f.animationSettings[slot]?.fps ?: 12)}
 var view by remember {mutableStateOf<PixelView?>(null)}
 fun flush(): List<String> {val next=paths.toMutableList(); next[index]=persist(bitmap); paths=next; return next}
 fun switch(target: Int) {if(target==index) return; val next=if(playing) paths else flush(); index=target.coerceIn(next.indices); bitmap=read(next[index])}
 BackHandler {back()}
 val previous=remember(index,paths) {if(index>0) read(paths[index-1]) else null}
 DisposableEffect(previous){onDispose {previous?.recycle()}}
 LaunchedEffect(playing,fps,paths.size) {while(playing){delay(1000L/fps); switch((index+1)%paths.size)}}
 Frame("FABRICAR · $slot",back,action={ForgeButton("GUARDAR ANIMACIÓN"){playing=false; save(flush(),fps)}}) {
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
   listOf("LÁPIZ","BORRADOR","RELLENO","LÍNEA","RECTÁNGULO","CUENTAGOTAS").forEach {t -> TextButton({playing=false; tool=t}){Text(t,color=if(t==tool) ForgeColors.gold else ForgeColors.secondary)}}
   TextButton({view?.undo()}){Text("↶")}; TextButton({view?.redo()}){Text("↷")}; CheckRow("GUÍA",guide){guide=it}; CheckRow("ONION 30%",onion){onion=it}
  }
  Row(Modifier.weight(1f)) {
   key(bitmap) {AndroidView(factory={PixelView(it,bitmap).also {v -> view=v}},update={it.tool=tool; it.color=color; it.brush=brush; it.picked={c -> color=c}; it.onion=if(onion) previous else null; it.guide=if(guide && slot!="foto") bodyGuide else null; it.editable=!playing; it.invalidate()},modifier=Modifier.weight(1f).fillMaxHeight())}
   Column(Modifier.width(150.dp).verticalScroll(rememberScrollState())) {
    Counter("PX",brush,1,16){brush=it}; Text("TONO"); Slider(hue,{hue=it; color=Color.HSVToColor(floatArrayOf(hue,saturation,brightness))},valueRange=0f..360f)
    Text("SATURACIÓN"); Slider(saturation,{saturation=it; color=Color.HSVToColor(floatArrayOf(hue,saturation,brightness))}); Text("BRILLO"); Slider(brightness,{brightness=it; color=Color.HSVToColor(floatArrayOf(hue,saturation,brightness))})
    (0 until 8).forEach {row -> Row {repeat(4){col -> val n=row*4+col; val c=if(n==0) Color.BLACK else if(n==31) Color.WHITE else Color.HSVToColor(floatArrayOf((n%16)*22.5f,if(n<16) 1f else .4f,1f)); Box(Modifier.size(32.dp).background(androidx.compose.ui.graphics.Color(c)).border(1.dp,ForgeColors.border).clickable {color=c})}}}
   }
  }
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically) {
   TextButton({if(playing) playing=false else {flush(); playing=true}}){Text(if(playing) "PARAR" else "▶")}
   paths.indices.forEach {i -> var drag by remember(i) {mutableFloatStateOf(0f)}; TextButton({playing=false; switch(i)},Modifier.width(64.dp).border(if(index==i) 2.dp else 1.dp,if(index==i) ForgeColors.secondary else ForgeColors.border).pointerInput(i,paths.size) {detectDragGesturesAfterLongPress(onDragStart={drag=0f},onDrag={change,amount -> change.consume(); drag+=amount.x},onDragEnd={val target=(i+(drag/(64*store.context.resources.displayMetrics.density)).toInt()).coerceIn(paths.indices); if(target!=i){val next=flush().toMutableList(); val item=next.removeAt(i); next.add(target,item); paths=next; index=target; bitmap=read(next[target])}})}){Text("${i+1}")} }
   if(slot!="foto") {TextButton({playing=false; if(paths.size<24){val next=flush()+blank().let {b -> persist(b).also {b.recycle()}}; paths=next; index=next.lastIndex; bitmap=read(next.last())}}){Text("+ FRAME")}; TextButton({playing=false; if(paths.size<24){val next=flush().toMutableList(); next.add(index+1,persist(bitmap)); paths=next; index++; bitmap=read(next[index])}}){Text("DUPLICAR")}; TextButton({playing=false; if(paths.size>1){val next=paths.toMutableList(); next.removeAt(index); paths=next; index=index.coerceAtMost(next.lastIndex); bitmap=read(next[index])}}){Text("BORRAR")}}
   Counter("FPS",fps,1,30){fps=it}
  }
 }
}
