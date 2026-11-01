package com.fightforge.game

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportRequest(val uris: List<Uri>,val folder: String,val width: Int,val height: Int,val ready: (List<String>)->Unit)
@Composable fun ImageImportDialog(store: Storage,request: ImportRequest?,dismiss: ()->Unit,message: (String)->Unit) {
 val scope=rememberCoroutineScope()
 var working by remember(request) {mutableStateOf(false)}
 var checked by remember(request) {mutableStateOf(false)}
 var mismatch by remember(request) {mutableStateOf(false)}
 suspend fun perform(r: ImportRequest) {
  working=true
  runCatching {withContext(Dispatchers.IO) {r.uris.map {store.importImage(it,r.folder,r.width,r.height)}}}
   .onSuccess {r.ready(it); dismiss()}.onFailure {message(it.message ?: "Error al importar"); dismiss()}
  working=false
 }
 LaunchedEffect(request) {if(request!=null) {
  runCatching {withContext(Dispatchers.IO) {request.uris.any {val d=store.dimensions(it); d.first!=request.width || d.second!=request.height}}}
   .onSuccess {mismatch=it; checked=true; if(!it) perform(request)}.onFailure {message(it.message ?: "Imagen no válida"); dismiss()}
 }}
 if(request!=null && checked && mismatch) AlertDialog(onDismissRequest={if(!working) dismiss()},title={Text("AJUSTAR IMÁGENES")},text={Text(if(working) "Importando…" else "Las imágenes no coinciden con ${request.width} × ${request.height} px. Se ajustarán sin deformar, centradas y con transparencia.")},confirmButton={TextButton(enabled=!working,onClick={scope.launch {perform(request)}}){Text("AJUSTAR")}},dismissButton={TextButton(enabled=!working,onClick=dismiss){Text("CANCELAR")}})
}
