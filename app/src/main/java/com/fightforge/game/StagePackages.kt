package com.fightforge.game
import com.fightforge.engine.AssetArchive
import kotlinx.serialization.encodeToString
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream
object StagePackages {
 fun encode(store: Storage,s: StageDef): ByteArray {
  val files=linkedMapOf<String,ByteArray>(); val frames=s.frames.mapIndexed {i,path -> val name="frames/$i.png"; files[name]=File(path).readBytes(); name}
  val music=if(s.music.isBlank()) "" else "music.audio".also {files[it]=File(s.music).readBytes()}
  files["stage.json"]=store.json.encodeToString(s.copy(frames=frames,music=music)).toByteArray()
  return AssetArchive.encode(files)
 }
 fun decode(store: Storage,bytes: ByteArray): StageDef {
  val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
  val dir=File(store.context.cacheDir,"stages/$hash").apply {mkdirs()}; var count=0L
  ZipInputStream(ByteArrayInputStream(bytes)).use {zip -> var entry=zip.nextEntry; while(entry!=null) {val file=File(dir,entry.name); require(!entry.isDirectory && file.canonicalPath.startsWith(dir.canonicalPath+File.separator)); file.parentFile!!.mkdirs(); file.outputStream().use {out -> val buf=ByteArray(8192); var n=zip.read(buf); while(n>=0) {count+=n; require(count<=32*1024*1024); out.write(buf,0,n); n=zip.read(buf)}}; zip.closeEntry(); entry=zip.nextEntry}}
  fun resolve(path: String): String {val f=File(dir,path); require(f.canonicalPath.startsWith(dir.canonicalPath+File.separator) && f.isFile); return f.absolutePath}
  val raw=store.json.decodeFromString<StageDef>(File(dir,"stage.json").readText()); require(raw.name.isNotBlank() && raw.frames.size in 1..12 && raw.fps in 1..30)
  return raw.copy(frames=raw.frames.map(::resolve),music=if(raw.music.isBlank()) "" else resolve(raw.music))
 }
}
