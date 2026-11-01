package com.fightforge.engine

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

const val SCALE = 16
const val FLOOR = 920 * SCALE
@Serializable data class Stats(val damage: Int = 5, val life: Int = 5, val speed: Int = 5, val reach: Int = 5) {
    fun valid() = listOf(damage, life, speed, reach).all { it in 0..10 } && damage + life + speed + reach == 20
    val maxHp get() = 5000 * (60 + 8 * life) / 100
    val walk get() = 504 * SCALE * (70 + 6 * speed) / 6000
    val run get() = walk * 2
    val reachCost get() = 130 - 6 * reach
}
@Serializable data class Rect(val x: Int, val y: Int, val w: Int, val h: Int) {
    fun intersects(b: Rect) = x < b.x + b.w && x + w > b.x && y < b.y + b.h && y + h > b.y
    fun touches(b: Rect) = x <= b.x + b.w && x + w >= b.x && y <= b.y + b.h && y + h >= b.y
    fun union(b: Rect): Rect { val l = minOf(x,b.x); val t = minOf(y,b.y); return Rect(l,t,maxOf(x+w,b.x+b.w)-l,maxOf(y+h,b.y+b.h)-t) }
}
enum class State { IDLE,WALK,RUN,CROUCH_DOWN,CROUCH,CROUCH_UP,JUMP,FALL,AIR_RUN,BLOCK_STAND,BLOCK_CROUCH,BLOCK_AIR,HURT,STUN,DIZZY,KO,DOWN,GETUP,ATTACK,GRABBED,TECH }
@Serializable enum class Kind { BASIC, COMMAND, ULTIMATE }
@Serializable enum class BoxKind { DAMAGE, MOVEMENT, TELEPORT, PUNISH, PROJECTILE, SOLID, GHOST, GRAB }
@Serializable data class Box(val kind: BoxKind, val rect: Rect, val rival: Boolean = false, val returnTrip: Boolean = false, val speed: Int = 0, val gravity: Boolean = false, val freeDirection: Boolean = false, val hp: Int = 50, val behavior: Int = 0, val effect: Int = 6, val harmfulToOwner: Boolean = false, val slots: Int = 1, val entity: String = "player", val both: Boolean = false, val photo: String = "")
@Serializable data class Move(val name: String, val command: List<String>, val kind: Kind = Kind.BASIC, val startup: Int = 5, val recovery: Int = 5, val extra: Int = 0, val boxes: List<Box> = emptyList(), val powerPercent: Int = 50, val invulnerable: Boolean = false, val antiAir: Boolean = false, val antiGround: Boolean = false, val animation: String = "quieto", val introAnimation: String = "") {
    val active get() = when(kind) { Kind.BASIC -> 4; Kind.COMMAND -> 6; Kind.ULTIMATE -> 8 }
    val duration get() = startup + active + recovery
    fun damage(s: Stats, combo: Int = 1): Int {
        val buttons = command.count { it in listOf("A","B","C","D") }.coerceAtLeast(1)
        val base = when(kind) { Kind.BASIC -> 120; Kind.COMMAND -> 220; Kind.ULTIMATE -> 700 }
        return (base.toLong() * (60 + 8*s.damage) * (100+extra) * (100+20*(buttons-1)) * max(40,100-10*(combo-1)) / 100000000).toInt().coerceAtLeast(1)
    }
}
@Serializable data class AnimationSettings(val fps: Int=12,val loop: Boolean=true,val enabled: Boolean=true)
@Serializable data class FighterDef(val id: String, val name: String, val stats: Stats = Stats(), val width: Int = 200, val height: Int = 400, val description: String = "", val moves: List<Move> = emptyList(), val color: Int = 0, val custom: Boolean = false, val animations: Map<String,List<String>> = emptyMap(),val animationSettings: Map<String,AnimationSettings> = emptyMap())
object Commands {
    val tokens = setOf("N","NE","E","SE","S","SW","W","NW","A","B","C","D")
    fun normalized(c: List<String>) = c.map { it.trim().uppercase() }
    fun valid(c: List<String>, kind: Kind): Boolean = c.size in (if(kind == Kind.ULTIMATE) 3 else 1)..8 && c.all { it in tokens } && c.any { it in setOf("A","B","C","D") }
    fun duplicate(c: List<String>, moves: List<Move>) = moves.any { normalized(it.command) == normalized(c) }
    fun match(history: List<Pair<Int,String>>, moves: List<Move>, tick: Int): Move? = moves.filter { m ->
        var i = history.lastIndex; var next = tick
        m.command.asReversed().all { token ->
            while (i >= 0 && history[i].second != token && next-history[i].first <= 15) i--
            if(i < 0 || next-history[i].first > 15) false else { next=history[i].first; i--; true }
        }
    }.maxByOrNull { it.command.size }
}
object Economy {
    // Milli-points preserve half points and stat multipliers without rounding exploits.
    fun boxCost(b: Box, stats: Stats): Long {
        val area = b.rect.w.toLong()*b.rect.h
        val raw = when(b.kind) {
            BoxKind.DAMAGE -> area
            BoxKind.MOVEMENT -> (abs(b.rect.w).toLong()+abs(b.rect.h))*1000/60 * (if(b.rival) 2 else 1) * (if(b.returnTrip) 2 else 1)
            BoxKind.TELEPORT -> if(b.rival) 72000L else 24000L
            BoxKind.PUNISH -> -minOf(area,15000)/2
            BoxKind.GRAB -> 30000L
            BoxKind.PROJECTILE -> area + b.speed*10L + (if(b.freeDirection) 2000 else 0) + (if(b.gravity) 6000 else 0) + (b.slots-1)*15000L
            BoxKind.SOLID, BoxKind.GHOST -> area + (if(b.kind == BoxKind.SOLID) b.hp*20L else 0L) + (if(b.behavior==15) 14 else b.behavior)*1000L + b.effect*1000L*(if(b.harmfulToOwner) -1 else 1) + (if(b.gravity) 6000 else 0) + (b.slots-1)*15000L
        }
        return raw*stats.reachCost/100
    }
    fun frameGain(frames: Int, s: Stats): Long = if(frames >= 5) (frames-5)*500L*(50+10*s.speed)/100 else (frames-5)*1000L
    fun balance(m: Move,s: Stats): Long = frameGain(m.startup,s)+frameGain(m.recovery,s) + (if(m.extra < 0) -m.extra*100L else -m.extra*200L) + (if(m.kind==Kind.ULTIMATE) 20000L+(m.powerPercent-50)*800L else 0) - m.boxes.sumOf { boxCost(it,s) } - (if(m.invulnerable) 40000 else 0) - (if(m.antiAir) 12000 else 0) - (if(m.antiGround) 25000 else 0)
    fun errors(m: Move,s: Stats,body: Rect): List<String> = buildList {
        if(m.name.isBlank()) add("Falta el nombre")
        if(!Commands.valid(m.command,m.kind)) add("Comando inválido")
        if(m.startup !in 1..600 || m.recovery !in 1..600) add("Frames fuera de rango")
        if(m.extra !in -50..100 || m.extra%5 != 0) add("Daño extra inválido")
        if(balance(m,s)<0) add("Saldo negativo")
        if(m.boxes.none {it.kind in listOf(BoxKind.DAMAGE,BoxKind.PROJECTILE,BoxKind.GRAB,BoxKind.MOVEMENT,BoxKind.TELEPORT) || (it.kind in listOf(BoxKind.SOLID,BoxKind.GHOST) && it.effect==6)}) add("Falta una fuente de daño o movimiento")
        if(m.kind==Kind.BASIC && (m.antiAir || m.antiGround)) add("Esta opción requiere Comando o Ulti")
        if(m.kind!=Kind.ULTIMATE && m.invulnerable) add("La invulnerabilidad requiere Ulti")
        if(m.powerPercent !in 50..100 || m.powerPercent%5!=0) add("Consumo de poder inválido")
        if(m.boxes.count { it.kind==BoxKind.TELEPORT }>1) add("Solo un teletransporte")
        if(m.boxes.filter { it.kind==BoxKind.MOVEMENT }.groupBy { it.entity }.any {it.value.size>1}) add("Solo un movimiento por entidad")
        if(m.boxes.any { it.kind==BoxKind.GRAB } && m.boxes.size>1) add("El agarre es exclusivo")
        if(m.boxes.any {it.kind==BoxKind.GRAB} && m.recovery>60) add("Recuperación máxima del agarre: 60")
        for(b in m.boxes) {
            if(b.slots !in 1..4) add("Cantidad inválida")
            if(b.rect.w !in 20..800 || b.rect.h !in 20..800) add("Tamaño de hitbox inválido")
            if(b.kind==BoxKind.DAMAGE && (b.rect.w<40 || b.rect.h<40)) add("Daño: mínimo 40×40")
            if(b.kind !in listOf(BoxKind.DAMAGE,BoxKind.MOVEMENT,BoxKind.PUNISH,BoxKind.GRAB) && (abs(b.rect.x)>800 || abs(b.rect.y)>800)) add("Distancia máxima: 800 u")
            if(b.speed !in 0..1800) add("Velocidad de proyectil inválida")
            if(b.kind in listOf(BoxKind.DAMAGE,BoxKind.MOVEMENT,BoxKind.PUNISH) && !b.rect.touches(body)) add("La hitbox debe tocar el personaje")
        }
    }
}
data class TemplateGeometry(val width: Int,val height: Int) {
    init { require(width in 100..400 && height in 200..800) }
    val pixelsWide get() = width*23/10
    val pixelsHigh get() = height*23/10
    val body get() = Rect((pixelsWide-width*2)/2,(pixelsHigh-height*2)/2,width*2,height*2)
    companion object {
        private val cache = mutableMapOf<Pair<Int,Int>,TemplateGeometry>()
        @Synchronized fun cached(w: Int,h: Int) = cache.getOrPut(w to h) { TemplateGeometry(w,h) }
        fun movement(body: Rect,rival: Rect,boxes: List<Box>): Rect {
            var bounds=body.union(rival)
            for(b in boxes) { bounds=bounds.union(b.rect); if(b.kind in listOf(BoxKind.MOVEMENT,BoxKind.TELEPORT)) bounds=bounds.union(Rect(b.rect.x,b.rect.y,body.w,body.h)) }
            val mx=bounds.w*15/200; val my=bounds.h*15/200
            return Rect((bounds.x-mx)*2,(bounds.y-my)*2,(bounds.w+mx*2)*2,(bounds.h+my*2)*2)
        }
    }
}
object AnimationFallback {
    fun resolve(name: String, available: Set<String>): String {
        val fallbacks=mapOf("correr atrás" to "caminar atrás","caminar atrás" to "caminar adelante","caminar adelante" to "quieto","correr adelante" to "caminar adelante","mareado" to "daño","noqueado" to "daño","tumbado" to "K.O.","levantarse" to "K.O.","agacharse→levantarse" to "agacharse")
        var current=name
        repeat(8) { if(current in available) return current; current=fallbacks[current] ?: "quieto" }
        return "quieto"
    }
}
data class Input(val horizontal: Int=0,val vertical: Int=0,val buttons: Int=0)
object Joystick {
    fun absolute(x: Int,y: Int,cx: Int,cy: Int,radius: Int): Input {
        val dx=x-cx; val dy=y-cy
        if(dx.toLong()*dx+dy.toLong()*dy < radius.toLong()*radius*18*18/10000) return Input()
        val h=if(abs(dx)*100>abs(dy)*41) dx.compareTo(0) else 0
        val v=if(abs(dy)*100>abs(dx)*41) dy.compareTo(0) else 0
        return Input(h,v)
    }
}
class DoubleTap {
    private var previous=0; private val released=mutableMapOf<Int,Int>(); private var running=0
    fun reset() {previous=0; released.clear(); running=0}
    fun checksum() = previous*31+running*17+(released[-1] ?: 0)*13+(released[1] ?: 0)
    fun update(direction: Int,tick: Int): Boolean {
        if(direction!=previous) {
            if(previous!=0) released[previous]=tick
            running=if(direction!=0 && tick-(released[direction] ?: -1000)<=12) direction else 0
            previous=direction
        }
        return direction!=0 && running==direction
    }
}
class SeededRandom(private var state: Int) {
    fun next(bound: Int): Int { state=state xor (state shl 13); state=state xor (state ushr 17); state=state xor (state shl 5); return (state and Int.MAX_VALUE)%bound }
    fun checksum()=state
}
