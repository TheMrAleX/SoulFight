package com.fightforge.engine

import org.junit.Assert.*
import org.junit.Test

class AirCombatTest {
 private fun fighter()=Defaults.roster()[0].copy(moves=listOf(Move("AÉREO",listOf("A"),startup=12,recovery=80,boxes=listOf(Box(BoxKind.DAMAGE,Rect(100,-200,40,40))))))
 @Test fun airAttackFollowsTheSameJumpArcAsAnUninterruptedJump() {
  val g=Combat(fighter(),fighter(),practice=true)
  g.step(Input(vertical=-1),Input(vertical=-1))
  repeat(85){frame ->
   g.step(if(frame==4) Input(buttons=1) else Input(),Input())
   assertEquals("Height diverged on frame $frame",g.fighters[1].y,g.fighters[0].y)
   assertEquals("Velocity diverged on frame $frame",g.fighters[1].vy,g.fighters[0].vy)
  }
  assertEquals(FLOOR,g.fighters[0].y)
  assertNotNull("Landing must not require finishing the attack",g.fighters[0].move)
 }
 @Test fun damageStunDoesNotSuspendAnAirborneFighter() {
  val g=Combat(fighter(),fighter(),practice=true)
  g.step(Input(vertical=-1),Input(vertical=-1))
  g.fighters[0].state=State.HURT; g.fighters[0].timer=14
  repeat(30){g.step(Input(),Input()); assertEquals(g.fighters[1].y,g.fighters[0].y)}
 }
 @Test fun airAttackKeepsHorizontalSteering() {
  val g=Combat(fighter(),fighter(),practice=true)
  g.step(Input(vertical=-1),Input())
  val x=g.fighters[0].x
  g.step(Input(horizontal=-1,buttons=1),Input())
  repeat(8){g.step(Input(horizontal=-1),Input())}
  assertEquals(x-9*g.one.stats.walk,g.fighters[0].x)
 }
 @Test fun ultimateStillPausesBothFightersForExactlySixtyTicks() {
  val g=Combat(Defaults.roster()[0],Defaults.roster()[1],practice=true)
  g.step(Input(vertical=-1),Input(vertical=-1))
  val positions=g.fighters.map {it.y to it.vy}; g.superPause=60
  repeat(60){g.step(Input(buttons=1),Input()); assertEquals(positions,g.fighters.map {it.y to it.vy})}
  assertEquals(0,g.superPause)
  g.step(Input(),Input()); assertNotEquals(positions,g.fighters.map {it.y to it.vy})
 }
 @Test fun downedFighterMustLandBeforeGettingUp() {
  val g=Combat(fighter(),fighter(),practice=true); val f=g.fighters[0]
  f.y=FLOOR-600*SCALE; f.state=State.DOWN; f.timer=1
  repeat(120){if(f.y<FLOOR){g.step(Input(),Input()); if(f.y<FLOOR){assertEquals(State.DOWN,f.state); assertEquals(0,f.invulnerable)}}}
  assertEquals(FLOOR,f.y); assertEquals(State.GETUP,f.state)
  repeat(30){g.step(Input(),Input())}; assertEquals(State.IDLE,f.state)
 }
 @Test fun attackRecoveryRestoresTheActualAirborneState() {
  val f=fighter().copy(moves=listOf(Move("CORTO",listOf("A"),startup=5,recovery=9,boxes=listOf(Box(BoxKind.DAMAGE,Rect(100,-200,40,40))))))
  val g=Combat(f,f,practice=true)
  g.step(Input(vertical=-1),Input()); g.step(Input(buttons=1),Input())
  repeat(18){g.step(Input(),Input())}; assertNull(g.fighters[0].move); assertEquals(State.JUMP,g.fighters[0].state)
  repeat(12){g.step(Input(),Input())}; g.step(Input(buttons=1),Input())
  repeat(18){g.step(Input(),Input())}; assertNull(g.fighters[0].move); assertEquals(State.FALL,g.fighters[0].state)
 }
 @Test fun knockedOutFighterStillFallsInPractice() {
  val g=Combat(fighter(),fighter(),practice=true); g.infiniteLife=false
  g.step(Input(vertical=-1),Input(vertical=-1)); g.fighters[0].hp=0
  repeat(80){g.step(Input(),Input()); assertEquals(g.fighters[1].y,g.fighters[0].y)}
  assertEquals(FLOOR,g.fighters[0].y); assertEquals(State.KO,g.fighters[0].state)
 }

 @Test fun resettingPracticePositionsCancelsAirborneActionsWithoutHealing() {
  val g=Combat(fighter(),fighter(),practice=true); g.step(Input(vertical=-1),Input(vertical=-1)); g.step(Input(buttons=1),Input())
  g.fighters[0].hp=3210; g.fighters[0].power=720; val tick=g.tick
  g.resetPositions()
  assertEquals(tick,g.tick); assertEquals(3210,g.fighters[0].hp); assertEquals(720,g.fighters[0].power)
  g.fighters.forEach {assertEquals(FLOOR,it.y); assertEquals(0,it.vy); assertNull(it.move); assertEquals(State.IDLE,it.state); assertEquals(0,it.relativeMotion)}
  assertEquals(1000*SCALE,g.fighters[0].x); assertEquals(1600*SCALE,g.fighters[1].x)
 }

 @Test fun restartingMatchRestoresAFreshStateIncludingAttackHistory() {
  val f=fighter().copy(moves=listOf(Move("DISPARO",listOf("A"),startup=1,recovery=80,boxes=listOf(Box(BoxKind.PROJECTILE,Rect(100,-260,80,80),speed=900)))))
  val g=Combat(f,f,practice=true); val initial=g.checksum(); g.fighters[1].x=1200*SCALE
  g.step(Input(buttons=1),Input()); g.step(Input(),Input()); assertTrue(g.lastHitTick>0)
  g.reset(); assertEquals(initial,g.checksum()); assertTrue(g.lastHitTick<0); assertTrue(g.lastGrabClash<0)
 }

}
