package com.fightforge.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.fightforge.engine.*

@Composable fun FighterDetails(f: FighterDef,modifier: Modifier=Modifier,continueFight: ()->Unit) {
 Column(modifier.fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
   Portrait(f,Modifier.size(88.dp).background(ForgeColors.high,CutCornerShape(10.dp)))
   Column(Modifier.weight(1f)) {Text(f.name.uppercase(),style=MaterialTheme.typography.titleMedium,maxLines=2,overflow=TextOverflow.Ellipsis); Text(f.description,color=ForgeColors.secondary,style=MaterialTheme.typography.labelSmall,maxLines=3,overflow=TextOverflow.Ellipsis)}
  }
  listOf("DAÑO" to f.stats.damage,"VIDA" to f.stats.life,"VELOCIDAD" to f.stats.speed,"ALCANCE" to f.stats.reach).forEach {(label,value) ->
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)) {
    Text(label,Modifier.width(76.dp),style=MaterialTheme.typography.labelSmall)
    LinearProgressIndicator(progress={value/10f},modifier=Modifier.weight(1f).height(8.dp),color=ForgeColors.secondary)
    Text(value.toString(),Modifier.width(20.dp),color=ForgeColors.gold)
   }
  }
  Spacer(Modifier.weight(1f))
  ForgeButton("CONTINUAR",Modifier.fillMaxWidth(),onClick=continueFight)
 }
}

@Composable fun ShopGrid(store: Storage,purchase: (FighterDef)->Unit) {
 LazyVerticalGrid(columns=GridCells.Adaptive(180.dp),modifier=Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
  items(Defaults.roster(),key={it.id}) {f ->
   Column(Modifier.background(ForgeColors.surface,CutCornerShape(10.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
     Portrait(f,Modifier.size(52.dp))
     Column(Modifier.weight(1f)){Text(f.name,style=MaterialTheme.typography.titleMedium); Text(f.description,color=ForgeColors.muted,style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)}
    }
    val owned=store.owned(f)
    ForgeButton(if(owned) "COMPRADO" else "${Defaults.prices[Defaults.names.indexOf(f.name)]} MONEDAS",Modifier.fillMaxWidth(),secondary=true,enabled=!owned){purchase(f)}
   }
  }
 }
}
