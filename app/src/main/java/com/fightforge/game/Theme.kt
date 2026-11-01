package com.fightforge.game
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
object ForgeColors {
 val bg=Color(0xFF0B0F1A); val surface=Color(0xFF141B2D); val high=Color(0xFF1E2742); val border=Color(0xFF2A3556)
 val primary=Color(0xFFFF4D5E); val secondary=Color(0xFF3DD9EB); val gold=Color(0xFFFFC83D); val text=Color(0xFFF2F5FF)
 val muted=Color(0xFF8B95B8); val ok=Color(0xFF3DDC84); val danger=Color(0xFFFF3B4E)
 val palette=listOf(primary,secondary,gold,ok,muted,danger)
}
@Composable fun ForgeTheme(content: @Composable ()->Unit) {
 val type=TextStyle(fontFamily=FontFamily(android.graphics.Typeface.create("sans-serif-condensed",android.graphics.Typeface.BOLD)),fontWeight=FontWeight.Bold,letterSpacing=1.sp)
 MaterialTheme(colorScheme=darkColorScheme(primary=ForgeColors.primary,secondary=ForgeColors.secondary,onPrimary=ForgeColors.text,onSecondary=ForgeColors.text,background=ForgeColors.bg,surface=ForgeColors.surface,onSurface=ForgeColors.text,onBackground=ForgeColors.text),shapes=Shapes(extraSmall=androidx.compose.foundation.shape.CutCornerShape(4.dp),small=androidx.compose.foundation.shape.CutCornerShape(8.dp),medium=androidx.compose.foundation.shape.CutCornerShape(10.dp),large=androidx.compose.foundation.shape.CutCornerShape(12.dp),extraLarge=androidx.compose.foundation.shape.CutCornerShape(16.dp)),typography=Typography(titleLarge=type.copy(fontSize=28.sp),titleMedium=type.copy(fontSize=20.sp),bodyMedium=type.copy(fontSize=14.sp),labelLarge=type.copy(fontSize=14.sp),bodyLarge=type.copy(fontSize=14.sp,lineHeight=18.sp),labelSmall=type.copy(fontSize=12.sp)),content=content)
}
