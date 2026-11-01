package com.fightforge.game

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fightforge.engine.*

fun BoxKind.title()=when(this){BoxKind.DAMAGE -> "DAÑO"; BoxKind.MOVEMENT -> "MOVIMIENTO"; BoxKind.TELEPORT -> "TELETRANSPORTE"; BoxKind.PUNISH -> "CASTIGO"; BoxKind.PROJECTILE -> "PROYECTIL"; BoxKind.SOLID -> "SÓLIDO"; BoxKind.GHOST -> "TRANSLÚCIDO"; BoxKind.GRAB -> "AGARRE"}
fun points(value: Long)=String.format(java.util.Locale.getDefault(),"%.2f",value/1000.0)
@Composable fun HitboxPalette(selected: BoxKind,stats: Stats,change: (BoxKind)->Unit) {
 val multiplier=stats.reachCost/100.0
 fun cost(value: Double)=String.format(java.util.Locale.getDefault(),"%.2f",value*multiplier)
 Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  BoxKind.entries.forEach {kind ->
   val cost=when(kind){BoxKind.DAMAGE -> "${cost(1.0)}p/1000u²"; BoxKind.MOVEMENT -> "${cost(1.0)}p/60u"; BoxKind.TELEPORT -> "${cost(24.0)}p"; BoxKind.PUNISH -> "+${cost(.5)}p/1000u²"; BoxKind.GRAB -> "${cost(30.0)}p"; else -> "ÁREA + EXTRAS"}
   Column(Modifier.width(112.dp),horizontalAlignment=Alignment.CenterHorizontally) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(HitboxView.colors[kind.ordinal])).border(if(selected==kind) 3.dp else 1.dp,if(selected==kind) ForgeColors.gold else ForgeColors.border,CircleShape).semantics {contentDescription="CREAR HITBOX ${kind.title()}"}.clickable {change(kind)},contentAlignment=Alignment.Center){Text(if(selected==kind) "✓" else "",color=Color.Black)}
    Text(kind.title(),style=MaterialTheme.typography.labelSmall)
    Text(cost,color=ForgeColors.muted,style=MaterialTheme.typography.labelSmall)
   }
  }
 }
}
