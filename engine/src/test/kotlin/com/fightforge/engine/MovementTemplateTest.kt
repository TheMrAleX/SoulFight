package com.fightforge.engine
import org.junit.Assert.*
import org.junit.Test
class MovementTemplateTest {
 private val body=MotionGeometry.body(200,400)
 private val rival=Rect(400,-400,200,400)
 @Test fun movingRivalDoesNotMoveTheCreatorsCanvasOrigin() {
  val boxes=listOf(Box(BoxKind.MOVEMENT,Rect(100,-400,120,400),rival=true))
  assertEquals(body,MotionGeometry.extreme(body,body,boxes,false))
  assertEquals(rival.copy(x=520),MotionGeometry.extreme(body,rival,boxes,true))
  val template=TemplateGeometry.movement(body,rival,boxes)
  assertTrue(template.x+template.w>=720*2)
 }
 @Test fun teleportOfRivalIsIncludedWithoutMovingCreator() {
  val boxes=listOf(Box(BoxKind.TELEPORT,Rect(700,-800,200,400),rival=true))
  assertEquals(body,MotionGeometry.extreme(body,body,boxes,false))
  assertEquals(Rect(700,-800,200,400),MotionGeometry.extreme(body,rival,boxes,true))
  val template=TemplateGeometry.movement(body,rival,boxes)
  assertTrue(template.y<=-800*2); assertTrue(template.x+template.w>=900*2)
 }
 @Test fun teleportAndTravelHaveCombinedCoverage() {
  val boxes=listOf(Box(BoxKind.TELEPORT,Rect(400,-600,200,400)),Box(BoxKind.MOVEMENT,Rect(100,-400,120,400)))
  assertEquals(Rect(520,-600,200,400),MotionGeometry.extreme(body,body,boxes,false))
  val template=TemplateGeometry.movement(body,rival,boxes)
  assertTrue(template.x+template.w>=720*2)
 }
}
