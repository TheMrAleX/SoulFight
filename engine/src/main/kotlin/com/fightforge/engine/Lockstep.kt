package com.fightforge.engine
/** A retained, fixed input delay. Each peer must advance exactly once per tick. */
class InputDelay(val frames: Int=3) {
 private val queue=ArrayDeque<Input>()
 init { require(frames in 0..12); repeat(frames){queue.addLast(Input())} }
 fun advance(input: Input): Input {queue.addLast(input); return queue.removeFirst()}
}
data class NetworkFrame(val tick: Int,val input: Input,val checksum: Long?=null) {
 fun validate(expectedTick: Int,expectedChecksum: Long) {
  require(tick==expectedTick){"Desincronización de frames"}
  require(input.horizontal in -1..1 && input.vertical in -1..1 && input.buttons in 0..15){"Entrada inválida"}
  if(tick%60==0) require(checksum==expectedChecksum){"Desincronización del estado"}
 }
}
