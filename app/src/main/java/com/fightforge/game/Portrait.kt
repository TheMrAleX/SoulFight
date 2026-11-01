package com.fightforge.game
import android.graphics.*
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
@Composable fun Portrait(f: FighterDef,modifier: Modifier){key(f.id,f.color,f.animations["foto"]){AndroidView(factory={object: View(it){private val image=f.animations["foto"]?.firstOrNull()?.let {path -> val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}; BitmapFactory.decodeFile(path,bounds); var sample=1; while(bounds.outWidth/sample>512 || bounds.outHeight/sample>512) sample*=2; BitmapFactory.decodeFile(path,BitmapFactory.Options().apply {inSampleSize=sample})}; override fun onDraw(c: Canvas){if(image!=null)c.drawBitmap(image,null,RectF(0f,0f,width.toFloat(),height.toFloat()),Paint(Paint.FILTER_BITMAP_FLAG)) else ProceduralSprites.fighter(c,width/2f,height*.98f,height*.4f,height*.95f,f.color,State.IDLE,0,1)}}},modifier=modifier)}}
