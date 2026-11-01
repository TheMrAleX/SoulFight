package com.fightforge.game
import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.fightforge.engine.State
class MenuBackdrop(context: Context): View(context) {
 private var tick=0
 override fun onDraw(c: Canvas){tick++; ProceduralSprites.stage(c,width.toFloat(),height.toFloat(),tick,0); val s=height/1080f; c.saveLayerAlpha(0f,0f,width.toFloat(),height.toFloat(),70); ProceduralSprites.fighter(c,width*.22f,height*.88f,200*s,400*s,0,State.IDLE,tick,1); ProceduralSprites.fighter(c,width*.43f,height*.88f,200*s,400*s,1,State.IDLE,tick,-1); c.restore(); if(isShown) postInvalidateOnAnimation()}
}
