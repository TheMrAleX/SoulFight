package com.fightforge.game
import android.graphics.*
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.fightforge.engine.*
@Composable fun Portrait(f: FighterDef,modifier: Modifier){AndroidView(factory={object: View(it){private val image=f.animations["foto"]?.firstOrNull()?.let {path -> BitmapFactory.decodeFile(path)}; override fun onDraw(c: Canvas){if(image!=null)c.drawBitmap(image,null,RectF(0f,0f,width.toFloat(),height.toFloat()),Paint(Paint.FILTER_BITMAP_FLAG)) else ProceduralSprites.fighter(c,width/2f,height*.98f,height*.4f,height*.95f,f.color,State.IDLE,0,1)}; override fun onDetachedFromWindow(){super.onDetachedFromWindow(); image?.recycle()}}},modifier=modifier)}
