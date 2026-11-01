package com.fightforge.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name="640 × 360",widthDp=640,heightDp=360)
@Preview(name="800 × 360",widthDp=800,heightDp=360)
@Preview(name="915 × 412",widthDp=915,heightDp=412)
@Composable private fun MenuLayoutPreview() {
 ForgeTheme {
  Row(Modifier.fillMaxSize().background(ForgeColors.bg).padding(24.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)) {
   Column(Modifier.weight(1f)) {Text("FIGHT FORGE",style=MaterialTheme.typography.titleLarge); Text("CREA. COMBATE. EVOLUCIONA.",color=ForgeColors.secondary)}
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
    listOf("UN JUGADOR","MULTIJUGADOR","PRÁCTICA","CREACIÓN","TIENDA","OPCIONES").forEach {label -> ForgeButton(label,Modifier.fillMaxWidth(),secondary=label!="UN JUGADOR",onClick={})}
   }
  }
 }
}
