package com.fightforge.engine

import kotlin.math.abs

object MotionGeometry {
    fun body(width: Int, height: Int) = Rect(-width / 2, -height, width, height)
    fun displacement(area: Rect, body: Rect): Pair<Int, Int> {
        val dx = when { area.x < body.x -> area.x-body.x; area.x+area.w > body.x+body.w -> area.x+area.w-body.x-body.w; else -> 0 }
        val dy = when { area.y < body.y -> area.y-body.y; area.y+area.h > body.y+body.h -> area.y+area.h-body.y-body.h; else -> 0 }
        return dx to dy
    }
    fun snap(rect: Rect, body: Rect): Rect {
        val edge = listOf(abs(rect.x-body.x-body.w),abs(rect.x+rect.w-body.x),abs(rect.y+rect.h-body.y),abs(rect.y-body.y-body.h)).withIndex().minBy { it.value }.index
        return when(edge) {
            0 -> rect.copy(x=body.x+body.w,y=rect.y.coerceIn(body.y-rect.h+1,body.y+body.h-1))
            1 -> rect.copy(x=body.x-rect.w,y=rect.y.coerceIn(body.y-rect.h+1,body.y+body.h-1))
            2 -> rect.copy(y=body.y-rect.h,x=rect.x.coerceIn(body.x-rect.w+1,body.x+body.w-1))
            else -> rect.copy(y=body.y+body.h,x=rect.x.coerceIn(body.x-rect.w+1,body.x+body.w-1))
        }
    }
    fun grab(width: Int,height: Int,reach: Int): Rect = Rect(width/2,-height*9/10,width*(75+5*reach)/200,height*8/10)
    fun animation(state: State, relative: Int): String = when(state) {
        State.IDLE -> "quieto"
        State.WALK -> if(relative<0) "caminar atrás" else "caminar adelante"
        State.RUN -> if(relative<0) "correr atrás" else "correr adelante"
        State.AIR_RUN -> if(relative<0) "correr aire atrás" else "correr aire adelante"
        State.CROUCH_DOWN -> "agacharse"
        State.CROUCH -> "agachado quieto"
        State.CROUCH_UP -> "agacharse→levantarse"
        State.JUMP -> "salto"
        State.FALL -> "caída"
        State.BLOCK_STAND -> "bloqueo parado"
        State.BLOCK_CROUCH -> "bloqueo agachado"
        State.BLOCK_AIR -> "bloqueo aire"
        State.HURT,State.GRABBED,State.TECH -> "daño"
        State.STUN -> "noqueado"
        State.DIZZY -> "mareado"
        State.KO -> "K.O."
        State.DOWN -> "tumbado"
        State.GETUP -> "levantarse"
        State.ATTACK -> "quieto"
    }
}
