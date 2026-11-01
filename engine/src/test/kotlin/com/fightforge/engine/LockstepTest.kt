package com.fightforge.engine
import org.junit.Assert.*
import org.junit.Test
class LockstepTest {
 @Test fun threeFramesDelay() {val queue=InputDelay(); val attack=Input(buttons=1); assertEquals(Input(),queue.advance(attack)); assertEquals(Input(),queue.advance(Input())); assertEquals(Input(),queue.advance(Input())); assertEquals(attack,queue.advance(Input()))}
 @Test(expected=IllegalArgumentException::class) fun detectsDesync() {NetworkFrame(60,Input(),7).validate(60,8)}
 @Test fun twoPeers() {val roster=Defaults.roster(); val a=Combat(roster[0],roster[1],practice=true); val b=Combat(roster[0],roster[1],practice=true); val q1=InputDelay(); val q2=InputDelay(); repeat(3600) {val left=q1.advance(Input(if(it%120<60) 1 else -1,buttons=if(it%24==0) 1 else 0)); val right=q2.advance(Input(if(it%120<60) -1 else 1,buttons=if(it%29==0) 2 else 0)); a.step(left,right); b.step(left,right); NetworkFrame(a.tick,left,if(a.tick%60==0) a.checksum() else null).validate(b.tick,b.checksum())}; assertEquals(a.checksum(),b.checksum())}
}
