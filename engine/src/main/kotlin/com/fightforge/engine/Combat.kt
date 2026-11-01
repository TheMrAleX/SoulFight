package com.fightforge.engine

import kotlin.math.abs

class Fighter(val definition: FighterDef,var x: Int) {
    var y=FLOOR; var vy=0; var facing=1; var hp=definition.stats.maxHp; var power=0
    var state=State.IDLE; var timer=0; var move: Move?=null; var attackFrame=0; var struck=false
    var grabbedBy=-1; var grabTarget=-1; var combo=0; var dizzy=0; var previousButtons=0; var previousDirection=""; var invulnerable=0
    val hitObjects=mutableSetOf<Int>(); val history=mutableListOf<Pair<Int,String>>(); val tap=DoubleTap()
    val body get()=Rect(x-definition.width*SCALE/2,y-definition.height*SCALE,definition.width*SCALE,definition.height*SCALE)
    fun reset(position: Int) { x=position; y=FLOOR; vy=0; hp=definition.stats.maxHp; power=0; state=State.IDLE; timer=0; move=null; attackFrame=0; struck=false; grabbedBy=-1; grabTarget=-1; combo=0; dizzy=0; tap.reset(); history.clear(); previousButtons=0; previousDirection=""; invulnerable=0 }
}
data class Spawn(val owner: Int,val kind: BoxKind,var x: Int,var y: Int,val w: Int,val h: Int,var vx: Int,var vy: Int,val gravity: Boolean,var life: Int,val damage: Int,val serial: Int,var hp: Int=50,val behavior: Int=0,val effect: Int=6,val harmfulToOwner: Boolean=false,val both: Boolean=false,val photo: String="",val originX: Int=x,var cooldown: Int=0)
class Combat(val one: FighterDef,val two: FighterDef,val seed: Int=42,val practice: Boolean=false) {
    val fighters=arrayOf(Fighter(one,1000*SCALE),Fighter(two,1600*SCALE))
    var superPause=0; var cinematicOwner=0; var tick=0; var round=1; var seconds=99; val wins=intArrayOf(0,0); var roundOver=false; var winner=-1; var matchOver=false
    val spawns=mutableListOf<Spawn>(); private var serial=0
    var infiniteLife=practice; var dummy=0
    fun nextRound() { if(matchOver) return; round++; roundOver=false; winner=-1; seconds=99; roundStart=tick; fighters[0].reset(1000*SCALE); fighters[1].reset(1600*SCALE); spawns.clear() }
    private var roundStart=0
    fun reset() { superPause=0; tick=0; round=1; seconds=99; wins.fill(0); roundOver=false; matchOver=false; winner=-1; roundStart=0; fighters[0].reset(1000*SCALE); fighters[1].reset(1600*SCALE); spawns.clear() }
    fun step(a: Input,b: Input) {
        if(roundOver || matchOver) return
        tick++; if(superPause>0) {superPause--; roundStart++; return}; val inputs=arrayOf(a,b)
        fighters[0].facing=if(fighters[0].x<=fighters[1].x) 1 else -1; fighters[1].facing=-fighters[0].facing
        for(i in 0..1) update(i,inputs[i])
        val grabs=(0..1).map { i -> val f=fighters[i]; val m=f.move; m!=null && f.attackFrame in m.startup until m.startup+m.active && m.boxes.any { it.kind==BoxKind.GRAB } && abs(f.x-fighters[1-i].x)<f.definition.width*SCALE }
        if(grabs.all { it }) { for(i in 0..1) { val f=fighters[i]; f.move=null; f.state=State.TECH; f.timer=15; f.x-=f.facing*150*SCALE } }
        else for(i in 0..1) attack(i,inputs[1-i])
        val iterator=spawns.iterator()
        while(iterator.hasNext()) {
            val p=iterator.next(); p.x+=p.vx; p.y+=p.vy; if(p.gravity) p.vy+=13
            p.life--; if(p.cooldown>0) p.cooldown--
            if(p.y+p.h>FLOOR) {p.y=FLOOR-p.h; p.vy=0}
            if(p.kind!=BoxKind.PROJECTILE) {
                val targetIndex=if(p.behavior==15) p.owner else 1-p.owner
                if(p.behavior in listOf(14,15)) p.vx=(fighters[targetIndex].x-p.x).compareTo(0)*60
                if(p.behavior==6 && abs(p.x-p.originX)>200*SCALE) p.vx=-p.vx
            }
            for(targetIndex in 0..1) {
                val target=fighters[targetIndex]
                val allowed=if(p.kind==BoxKind.PROJECTILE) targetIndex!=p.owner else p.both || targetIndex==(if(p.effect==8) {if(p.harmfulToOwner) 1-p.owner else p.owner} else {if(p.harmfulToOwner) p.owner else 1-p.owner})
                if(allowed && p.cooldown==0 && Rect(p.x,p.y,p.w,p.h).intersects(target.body) && target.invulnerable==0) {
                    if(p.kind==BoxKind.PROJECTILE) {hit(p.owner,p.damage,inputs[targetIndex],false); p.life=0}
                    else {
                        when(p.effect) {
                            8 -> target.hp=(target.hp+p.damage/2).coerceAtMost(target.definition.stats.maxHp)
                            7 -> target.power=(target.power-120).coerceAtLeast(0)
                            10 -> {target.state=State.DOWN; target.timer=40; target.move=null}
                            else -> if(targetIndex!=p.owner) hit(p.owner,p.damage,inputs[targetIndex],false) else {target.hp=(target.hp-p.damage).coerceAtLeast(0); target.state=State.HURT; target.timer=14}
                        }
                        p.cooldown=30
                    }
                }
            }
            if(p.life<=0 || p.hp<=0 || p.x !in -800*SCALE..4000*SCALE) iterator.remove()
        }
        if(practice && infiniteLife) fighters.forEach { it.hp=it.definition.stats.maxHp }
        if(!practice) {
            seconds=(99-(tick-roundStart)/60).coerceAtLeast(0)
            if(fighters.any { it.hp<=0 } || seconds==0) {
                val left=fighters[0].hp.toLong()*fighters[1].definition.stats.maxHp
                val right=fighters[1].hp.toLong()*fighters[0].definition.stats.maxHp
                winner=left.compareTo(right).let { if(it>0) 0 else if(it<0) 1 else -1 }
                if(winner>=0) wins[winner]++
                roundOver=true; matchOver=wins.any { it>=2 }
            }
        }
    }
    private fun effectiveBody(f: Fighter): Rect {
        val body=f.body; val m=f.move ?: return body
        if(m.antiAir) return body.copy(y=body.y+body.h*4/10,h=body.h*6/10)
        if(m.antiGround) return body.copy(h=body.h*6/10)
        return body
    }
    private fun vulnerableBody(f: Fighter): Rect {
        var body=effectiveBody(f); val m=f.move ?: return body
        if(f.attackFrame>=m.startup+m.active) for(b in m.boxes.filter {it.kind in listOf(BoxKind.DAMAGE,BoxKind.PUNISH)}) {
            val r=b.rect; val x=f.x+(if(f.facing>0) r.x else -r.x-r.w)*SCALE
            body=body.union(Rect(x,f.y+r.y*SCALE,r.w*SCALE,r.h*SCALE))
        }
        return body
    }
    private fun update(i: Int,input: Input) {
        val f=fighters[i]; val rival=fighters[1-i]
        if(f.invulnerable>0) f.invulnerable--
        if(f.dizzy>0 && tick%4==0) f.dizzy--
        if(f.hp<=0) { f.state=State.KO; return }; if(f.grabbedBy>=0) return
        val relative=input.horizontal*f.facing
        val direction=when { input.vertical<0 -> if(relative>0) "NE" else if(relative<0) "NW" else "N"; input.vertical>0 -> if(relative>0) "SE" else if(relative<0) "SW" else "S"; relative>0 -> "E"; relative<0 -> "W"; else -> "" }
        val directionChanged=direction!=f.previousDirection && direction.isNotEmpty()
        if(directionChanged) f.history.add(tick to direction)
        for(button in 0..3) if(input.buttons and (1 shl button)!=0 && f.previousButtons and (1 shl button)==0) f.history.add(tick to ('A'+button).toString())
        val newButton=input.buttons and f.previousButtons.inv()!=0
        f.previousButtons=input.buttons; f.previousDirection=direction; f.history.removeAll { tick-it.first>120 }
        if(f.timer>0) { f.timer--; if(f.timer==0) when(f.state) { State.CROUCH_DOWN -> f.state=State.CROUCH; State.CROUCH_UP -> f.state=State.IDLE; State.DOWN -> { f.state=State.GETUP; f.timer=30; f.invulnerable=30 }; State.STUN -> { f.state=State.DOWN; f.timer=40 }; else -> { f.state=State.IDLE; rival.combo=0 } }; return }
        if(f.move!=null) { f.attackFrame++; if(f.attackFrame>=f.move!!.duration) {
            if(f.grabTarget>=0) { val victim=fighters[f.grabTarget]; victim.grabbedBy=-1; hit(i,f.move!!.damage(f.definition.stats,f.combo+1)*18/10,Input(),true); f.grabTarget=-1 }
            f.move=null; f.state=State.IDLE
        }; return }
        if(newButton || directionChanged) {
            val m=Commands.match(f.history,f.definition.moves,tick)
            if(m!=null && (m.kind!=Kind.ULTIMATE || f.power>=3000*m.powerPercent/100)) {
                f.move=m; f.attackFrame=0; f.struck=false; f.hitObjects.clear(); f.tap.reset(); f.state=State.ATTACK
                if(m.kind==Kind.ULTIMATE) {f.power-=3000*m.powerPercent/100; superPause=60; cinematicOwner=i}
                if(m.invulnerable) f.invulnerable=m.startup+m.active
                f.history.clear(); return
            }
        }
        if(f.y==FLOOR && input.vertical<=0 && f.state==State.CROUCH) {f.state=State.CROUCH_UP; f.timer=5; return}
        if(f.y<FLOOR || f.vy<0) { f.vy+=13; f.y+=f.vy; if(f.y>=FLOOR) { f.y=FLOOR; f.vy=0 }; f.state=if(f.vy<0) State.JUMP else State.FALL; f.x+=input.horizontal*f.definition.stats.walk }
        else if(input.vertical<0) { f.vy=-1500*SCALE/60; f.state=State.JUMP; f.y+=f.vy }
        else if(input.vertical>0) {if(relative<0) f.state=State.BLOCK_CROUCH else if(f.state==State.CROUCH) f.state=State.CROUCH else {f.state=State.CROUCH_DOWN; f.timer=5}}
        else if(input.horizontal!=0) {
            val running=f.tap.update(input.horizontal,tick) && (f.definition.animationSettings[if(relative>0) "correr adelante" else "correr atrás"]?.enabled != false); f.state=if(running) State.RUN else if(relative<0) State.BLOCK_STAND else State.WALK
            f.x+=input.horizontal*(if(running) f.definition.stats.run else f.definition.stats.walk)
            if(running && relative>0 && tick%3==0) f.power=(f.power+1).coerceAtMost(3000)
        } else { f.tap.update(0,tick); f.state=State.IDLE }
        f.x=f.x.coerceIn(100*SCALE,3100*SCALE)
        for(p in spawns.filter {it.kind==BoxKind.SOLID}) if(f.body.intersects(Rect(p.x,p.y,p.w,p.h))) {f.x=if(f.x<p.x+p.w/2) p.x-f.definition.width*SCALE/2 else p.x+p.w+f.definition.width*SCALE/2}
        if(f.body.intersects(rival.body)) f.x=rival.x-f.facing*(f.definition.width+rival.definition.width)*SCALE/2
    }
    private fun attack(i: Int,defense: Input) {
        val f=fighters[i]; val m=f.move ?: return; val other=fighters[1-i]
        if(f.attackFrame==m.startup) for(box in m.boxes) {
            if(box.kind==BoxKind.TELEPORT) { val target=if(box.rival) other else f; target.x=(f.x+box.rect.x*SCALE*f.facing).coerceIn(100*SCALE,3100*SCALE) }
            if(box.kind in listOf(BoxKind.PROJECTILE,BoxKind.SOLID,BoxKind.GHOST)) {
                val existing=spawns.filter { it.owner==i && it.kind==box.kind }
                if(existing.size>=box.slots) spawns.remove(existing.minBy { it.serial })
                spawns.add(Spawn(i,box.kind,f.x+box.rect.x*SCALE*f.facing,f.y+box.rect.y*SCALE,box.rect.w*SCALE,box.rect.h*SCALE,(if(box.behavior==6 && box.speed==0) 300 else box.speed)*SCALE/60*f.facing,0,box.gravity,if(box.kind==BoxKind.PROJECTILE) 120 else 240,m.damage(f.definition.stats),serial++,box.hp,box.behavior,box.effect,box.harmfulToOwner,box.both,box.photo))
            }
        }
        if(f.attackFrame !in m.startup until m.startup+m.active) return
        for(box in m.boxes) {
            if(box.kind==BoxKind.MOVEMENT) {
                val target=if(box.rival) other else f
                var shift=box.rect.w*SCALE/m.active
                if(box.rival && defense.horizontal* f.facing>0) shift=(shift-other.definition.stats.walk*8/10).coerceAtLeast(0)
                if(box.returnTrip && f.attackFrame-m.startup>=m.active/2) shift=-shift
                if(box.entity=="player") target.x=(target.x+shift*f.facing).coerceIn(100*SCALE,3100*SCALE)
                else {val kind=when(box.entity) {"projectile" -> BoxKind.PROJECTILE; "solid" -> BoxKind.SOLID; else -> BoxKind.GHOST}; spawns.filter {it.owner==i && it.kind==kind}.forEach {it.x+=shift*f.facing}}
            }
            if(!f.struck && box.kind in listOf(BoxKind.DAMAGE,BoxKind.GRAB)) {
                val r=if(box.kind==BoxKind.GRAB) Rect(f.x+if(f.facing>0) 0 else -f.definition.width*SCALE,f.y-f.definition.height*SCALE*9/10,f.definition.width*SCALE,f.definition.height*SCALE*8/10) else Rect(f.x+(if(f.facing>0) box.rect.x else -box.rect.x-box.rect.w)*SCALE,f.y+box.rect.y*SCALE,box.rect.w*SCALE,box.rect.h*SCALE)
                spawns.filter {it.owner!=i && it.kind==BoxKind.SOLID && it.serial !in f.hitObjects && r.intersects(Rect(it.x,it.y,it.w,it.h))}.forEach {it.hp-=m.damage(f.definition.stats); f.hitObjects.add(it.serial)}
                if(r.intersects(vulnerableBody(other)) && other.invulnerable==0) { f.struck=true
                    if(box.kind==BoxKind.GRAB) { f.grabTarget=1-i; other.grabbedBy=i; other.state=State.GRABBED; other.move=null; other.timer=0 }
                    else hit(i,m.damage(f.definition.stats,f.combo+1),defense,false) }
            }
        }
    }
    private fun hit(i: Int,damage: Int,input: Input,grab: Boolean) {
        val f=fighters[i]; val target=fighters[1-i]
        val block=!grab && input.horizontal*target.facing<0 && target.move==null && target.timer==0
        val actual=if(block) (damage/10).coerceAtLeast(1) else damage
        if(target.grabTarget>=0) { fighters[target.grabTarget].grabbedBy=-1; fighters[target.grabTarget].state=State.IDLE; target.grabTarget=-1 }
        target.hp=(target.hp-actual).coerceAtLeast(0); target.move=null
        f.power=(f.power+if(block) 30 else 60).coerceAtMost(3000); target.power=(target.power+45).coerceAtMost(3000)
        target.x=(target.x+f.facing*(if(grab) 1200 else if(block) 40 else 60)*SCALE).coerceIn(100*SCALE,3100*SCALE)
        if(!block) { f.combo++; target.dizzy+=damage*400/target.definition.stats.maxHp }
        target.state=when { target.hp==0 -> State.KO; target.dizzy>=100 -> State.DIZZY; grab || damage*100>=target.definition.stats.maxHp*20 -> State.DOWN; damage*100>=target.definition.stats.maxHp*8 -> State.STUN; block -> State.BLOCK_STAND; else -> State.HURT }
        target.timer=when(target.state) { State.DIZZY -> { target.dizzy=0; 90 }; State.DOWN -> 40; State.STUN -> 45; State.BLOCK_STAND -> 10; else -> 14 }
    }
    fun checksum(): Long {
        var hash=1469598103934665603L
        fun add(v: Int) { hash=(hash xor v.toLong())*1099511628211L }
        add(tick); add(superPause); add(cinematicOwner); add(round); add(seconds); add(winner); wins.forEach { add(it) }
        fighters.forEach { f -> listOf(f.x,f.y,f.vy,f.hp,f.power,f.state.ordinal,f.timer,f.attackFrame,f.combo,f.dizzy,f.invulnerable,f.grabbedBy,f.grabTarget,f.previousButtons,f.facing,if(f.struck) 1 else 0).forEach { add(it) } }
        fighters.forEach { f -> f.history.forEach { add(it.first); add(it.second.hashCode()) }; add(f.tap.checksum()) }; spawns.forEach { p -> listOf(p.owner,p.kind.ordinal,p.x,p.y,p.vx,p.vy,p.life,p.serial,p.hp,p.cooldown,p.behavior,p.effect).forEach { add(it) } }
        return hash
    }
}
class Ai(seed: Int,val difficulty: Int=1) {
    private val random=SeededRandom(seed); private var held=Input()
    fun input(game: Combat,index: Int): Input {
        val reaction=intArrayOf(20,12,6)[difficulty.coerceIn(0,2)]
        if(game.tick%reaction!=0) return held.copy(buttons=0)
        val f=game.fighters[index]; val distance=abs(f.x-game.fighters[1-index].x)/SCALE
        held=if(random.next(100)<intArrayOf(35,15,5)[difficulty.coerceIn(0,2)]) Input() else if(distance>320) Input(f.facing) else when(random.next(6)) { 0 -> Input(-f.facing); 1 -> Input(0,-1); else -> Input(buttons=1 shl random.next(4)) }
        return held
    }
}
object Defaults {
    val classes=listOf(Stats(),Stats(7,7,3,3),Stats(4,4,4,8),Stats(5,3,8,4),Stats(5,9,2,4),Stats(9,2,6,3))
    val names=listOf("Kenji","Brutus","Lyra","Raiden","Golem","Sombra")
    val prices=listOf(0,0,300,500,500,800)
    fun roster()=names.mapIndexed { i,name ->
        val stats=classes[i]
        val moves=(0..3).map { b ->
            var m=Move(listOf("PUÑO","PATADA","IMPACTO","ASCENSO")[b],listOf(('A'+b).toString()),boxes=listOf(Box(BoxKind.DAMAGE,Rect(90,-300,100+b*10,100))))
            while(Economy.balance(m,stats)<0) m=m.copy(recovery=m.recovery+1)
            m
        }.toMutableList()
        var command=Move("FORJA",listOf("S","E","A"),Kind.COMMAND,boxes=listOf(Box(BoxKind.PROJECTILE,Rect(100,-260,80,80),speed=900)))
        while(Economy.balance(command,stats)<0) command=command.copy(recovery=command.recovery+1)
        moves.add(command)
        var ultimate=Move("ALMA ARDIENTE",listOf("S","E","D"),Kind.ULTIMATE,powerPercent=100,boxes=listOf(Box(BoxKind.DAMAGE,Rect(90,-400,350,400))))
        while(Economy.balance(ultimate,stats)<0) ultimate=ultimate.copy(recovery=ultimate.recovery+1)
        moves.add(ultimate)
        FighterDef("default_$i",name,stats,description=listOf("Equilibrado","Grappler","Zoner","Rushdown","Tanque","Asesino")[i],moves=moves,color=i)
    }
}
