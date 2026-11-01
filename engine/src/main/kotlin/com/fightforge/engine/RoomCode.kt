package com.fightforge.engine
object RoomCode {
 fun encode(ip: String,port: Int): String {
  val parts=ip.split('.').map {it.toInt()}; require(parts.size==4 && parts.all {it in 0..255} && port in 1..65535)
  val address=parts.fold(0L){n,v -> (n shl 8) or v.toLong()}
  return ((address shl 16) or port.toLong()).toString(36).uppercase()
 }
 fun decode(code: String): Pair<String,Int> {
  val value=code.trim().lowercase().toLong(36); require(value in 1 until (1L shl 48))
  val port=(value and 65535).toInt(); require(port>0)
  val address=value ushr 16
  return (3 downTo 0).joinToString("."){((address ushr (it*8)) and 255).toString()} to port
 }
}
