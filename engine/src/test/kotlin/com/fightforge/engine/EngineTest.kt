package com.fightforge.engine
import org.junit.Assert.*
import org.junit.Test
class EngineTest {
 @Test fun stats() { assertEquals(5000,Stats().maxHp); assertEquals(3000,Stats(0,0,10,10).maxHp); assertTrue(Move("a",listOf("A")).damage(Stats(0,10,5,5))>0) }
 @Test fun geometry() { val t=TemplateGeometry.cached(200,400); assertEquals(460,t.pixelsWide); assertEquals(920,t.pixelsHigh); assertSame(t,TemplateGeometry.cached(200,400)) }
 @Test fun absoluteJoystick() { assertEquals(Input(1,-1),Joystick.absolute(240,0,120,120,120)); assertEquals(Input(),Joystick.absolute(120,120,120,120,120)) }
 @Test fun doubleTap() { val t=DoubleTap(); assertFalse(t.update(1,0)); t.update(0,4); assertTrue(t.update(1,10)); assertFalse(t.update(0,11)) }
 @Test fun budget() { val s=Stats(); assertEquals(0L,Economy.balance(Move("a",listOf("A")),s)); assertEquals(24000L,Economy.boxCost(Box(BoxKind.TELEPORT,Rect(0,0,20,20)),s)); assertEquals(72000L,Economy.boxCost(Box(BoxKind.TELEPORT,Rect(0,0,20,20),rival=true),s)); assertEquals(6000L,Economy.boxCost(Box(BoxKind.PROJECTILE,Rect(0,0,20,20),gravity=true),s)-Economy.boxCost(Box(BoxKind.PROJECTILE,Rect(0,0,20,20)),s)) }
 @Test fun movementMultipliers() { val b=Box(BoxKind.MOVEMENT,Rect(100,0,60,60)); assertEquals(Economy.boxCost(b,Stats())*4,Economy.boxCost(b.copy(rival=true,returnTrip=true),Stats())) }
 @Test fun commands() { assertTrue(Commands.valid(listOf("S","E","A"),Kind.COMMAND)); val a=Move("a",listOf("A")); val b=Move("b",listOf("S","E","A")); assertEquals(b,Commands.match(listOf(1 to "S",4 to "E",7 to "A"),listOf(a,b),7)); assertTrue(Commands.duplicate(listOf("a"),listOf(a))) }
 @Test fun damageButtons() { val a=Move("a",listOf("A")); assertEquals(a.damage(Stats())*12/10,a.copy(command=listOf("A","B")).damage(Stats())) }
 @Test fun deterministic() { val r=Defaults.roster(); val a=Combat(r[0],r[1],practice=true); val b=Combat(r[0],r[1],practice=true); repeat(3600) { val input=Input(if(it%120<60) 1 else -1,buttons=if(it%30==0) 1 else 0); a.step(input,Input()); b.step(input,Input()) }; assertEquals(3600,a.tick); assertEquals(a.checksum(),b.checksum()) }
 @Test fun defaultsAreLegal() { Defaults.roster().forEach { f -> assertTrue(f.stats.valid()); f.moves.forEach { assertEquals(emptyList<String>(),Economy.errors(it,f.stats,Rect(-100,-400,200,400))) } } }
 @Test fun attachedValidation() { val m=Move("a",listOf("A"),recovery=100,boxes=listOf(Box(BoxKind.DAMAGE,Rect(900,0,40,40)))); assertTrue(Economy.errors(m,Stats(),Rect(-100,-400,200,400)).contains("La hitbox debe tocar el personaje")) }
 @Test fun fallback() { assertEquals("quieto",AnimationFallback.resolve("correr atrás",setOf("quieto"))); assertEquals("daño",AnimationFallback.resolve("mareado",setOf("daño","quieto"))) }
}
