package com.fightforge.game
import com.fightforge.engine.*
import kotlinx.serialization.encodeToString
import java.io.*
import java.security.MessageDigest
import java.util.zip.*
object CharacterPackages {
 fun encode(store: Storage,f: FighterDef): ByteArray {
  val paths=(f.animations.values.flatten()+f.moves.flatMap {it.boxes}.map {it.photo}.filter {it.isNotEmpty()}).distinct(); val mapping=paths.mapIndexed { i,path -> path to "assets/$i.png" }.toMap()
  val safe=f.copy(moves=f.moves.map {m -> m.copy(boxes=m.boxes.map {b -> b.copy(photo=mapping[b.photo] ?: "")})},animations=f.animations.mapValues {(_,frames) -> frames.map {mapping.getValue(it)}})
  val files=linkedMapOf("character.json" to store.json.encodeToString(safe).toByteArray())
  paths.forEach {path -> val file=File(path); require(file.canonicalPath.startsWith(store.context.filesDir.canonicalPath+File.separator)); require(file.length()<=8*1024*1024); files[mapping.getValue(path)]=file.readBytes()}
  return AssetArchive.encode(files)
 }
 fun decode(store: Storage,bytes: ByteArray): FighterDef {
  require(bytes.size<=8*1024*1024); val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}; val dir=File(store.context.cacheDir,"packages/$hash"); dir.mkdirs()
  var total=0L
  ZipInputStream(ByteArrayInputStream(bytes)).use {zip -> var entry=zip.nextEntry; while(entry!=null) {require(!entry.isDirectory); val file=File(dir,entry.name); require(file.canonicalPath.startsWith(dir.canonicalPath+File.separator)); file.parentFile!!.mkdirs(); file.outputStream().use {out -> val buffer=ByteArray(8192); var n=zip.read(buffer); while(n>=0) {total+=n; require(total<=32*1024*1024){"Contenido descomprimido demasiado grande"}; out.write(buffer,0,n); n=zip.read(buffer)}}; zip.closeEntry(); entry=zip.nextEntry} }
  val raw=store.json.decodeFromString<FighterDef>(File(dir,"character.json").readText()); require(raw.stats.valid() && raw.width in 100..400 && raw.height in 200..800 && raw.moves.size<=128)
  return raw.copy(moves=raw.moves.map {m -> m.copy(boxes=m.boxes.map {b -> if(b.photo.isEmpty()) b else {val image=File(dir,b.photo); require(image.canonicalPath.startsWith(dir.canonicalPath+File.separator) && image.isFile); b.copy(photo=image.absolutePath)}})},animations=raw.animations.mapValues {(_,frames) -> require(frames.size<=24); frames.map {path -> val file=File(dir,path); require(file.canonicalPath.startsWith(dir.canonicalPath+File.separator) && file.isFile); file.absolutePath}})
 }
 fun import(store: Storage,bytes: ByteArray): FighterDef {
  val source=decode(store,bytes); val id=java.util.UUID.randomUUID().toString(); val paths=(source.animations.values.flatten()+source.moves.flatMap {it.boxes}.map {it.photo}.filter {it.isNotEmpty()}).distinct()
  val map=paths.mapIndexed {i,path -> val target=File(store.context.filesDir,"characters/$id/assets/$i.png"); target.parentFile!!.mkdirs(); File(path).copyTo(target); path to target.absolutePath}.toMap()
  val f=source.copy(id=id,name=source.name.take(16),custom=true,animations=source.animations.mapValues {(_,frames) -> frames.map {map.getValue(it)}},moves=source.moves.map {m -> m.copy(boxes=m.boxes.map {b -> b.copy(photo=map[b.photo] ?: "")})})
  store.character(f); return f
 }
}
