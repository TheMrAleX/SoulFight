package com.fightforge.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ForgeIcons {
 val Check=ImageVector.Builder("Completo",24.dp,24.dp,24f,24f).apply {path(fill=SolidColor(Color.White)){moveTo(3f,12f); lineTo(6f,9f); lineTo(10f,13f); lineTo(19f,4f); lineTo(22f,7f); lineTo(10f,19f); close()}}.build()
 val Pending=ImageVector.Builder("Pendiente",24.dp,24.dp,24f,24f).apply {path(fill=null,stroke=SolidColor(Color.White),strokeLineWidth=2f){moveTo(12f,2f); lineTo(22f,12f); lineTo(12f,22f); lineTo(2f,12f); close()}}.build()
 val Trash=ImageVector.Builder("Eliminar",24.dp,24.dp,24f,24f).apply {
  path(fill=SolidColor(Color.White),pathFillType=PathFillType.EvenOdd){
   moveTo(3f,5f); lineTo(21f,5f); lineTo(21f,7f); lineTo(3f,7f); close()
   moveTo(9f,2f); lineTo(15f,2f); lineTo(15f,4f); lineTo(9f,4f); close()
   moveTo(5f,9f); lineTo(19f,9f); lineTo(18f,22f); lineTo(6f,22f); close()
   moveTo(9f,12f); lineTo(10f,12f); lineTo(10f,19f); lineTo(9f,19f); close()
   moveTo(14f,12f); lineTo(15f,12f); lineTo(15f,19f); lineTo(14f,19f); close()
  }
 }.build()
 val Lock=ImageVector.Builder("Bloqueado",24.dp,24.dp,24f,24f).apply {
  path(fill=SolidColor(Color.White)){
   moveTo(5f,10f); lineTo(19f,10f); lineTo(19f,22f); lineTo(5f,22f); close()
   moveTo(7f,10f); lineTo(7f,7f); curveTo(7f,0f,17f,0f,17f,7f); lineTo(17f,10f); lineTo(15f,10f); lineTo(15f,7f); curveTo(15f,3f,9f,3f,9f,7f); lineTo(9f,10f); close()
  }
 }.build()
}
