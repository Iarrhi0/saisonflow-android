package com.saisonflow.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

internal data class MediaCandidate(val episode: Episode, val quality: String, val url: String)
internal interface MediaOfferProvider {
    fun series(): Map<String, Map<Int, List<Episode>>>
}
private class JsonProvider(private val catalog: Catalogue) : MediaOfferProvider {
    override fun series(): Map<String, Map<Int, List<Episode>>> = catalog.bySeries
}

internal class MediaCatalogs(private val context: Context) {
    private val directory get() = File(context.filesDir, "media-catalogs")
    fun providers(): List<MediaOfferProvider> = buildList {
        add(JsonProvider(context.assets.open("catalogue-exemple.json").bufferedReader().use { Catalogue.parse(it.readText()) }))
        directory.listFiles()?.filter { it.extension == "json" }?.sortedBy { it.name }?.forEach {
            add(JsonProvider(Catalogue.parse(it.readText())))
        }
    }
    fun importAuthorized(uri: Uri) {
        val bytes = context.contentResolver.openInputStream(uri)!!.use { input ->
            val buffer = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(16384)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                require(buffer.size() + count <= 5 * 1024 * 1024) { "Catalogue trop volumineux" }
                buffer.write(chunk,0,count)
            }
            buffer.toByteArray()
        }
        require(Catalogue.parse(bytes.toString(Charsets.UTF_8)).bySeries.isNotEmpty()) { "Catalogue vide" }
        val name = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        directory.mkdirs()
        val dest = File(directory, "$name.json")
        if (!dest.exists()) {
            val tmp = File.createTempFile("import", ".tmp", directory)
            try { tmp.writeBytes(bytes); check(tmp.renameTo(dest)) } finally { tmp.delete() }
        }
    }
    fun seasons(title: String) = providers().flatMap { it.series()[title].orEmpty().entries }
        .groupBy({ it.key }, { it.value }).mapValues { it.value.flatten() }.toSortedMap()
}

internal suspend fun resolveEpisodes(episodes: List<Episode>,
    probe: (String) -> Boolean = Mp4Probe::available): List<MediaCandidate> = coroutineScope {
    val slots = Semaphore(4)
    episodes.flatMap { ep -> ep.qualities.map { MediaCandidate(ep,it.key,it.value) } }
        .filter { QualitySelector.isSupportedDirectVideo(it.url) }
        .distinctBy { Triple(it.episode.number,it.quality,it.url) }
        .map { candidate -> async(Dispatchers.IO) {
            slots.withPermit { candidate.takeIf { runCatching { probe(it.url) }.getOrDefault(false) } }
        } }.awaitAll().filterNotNull().sortedWith(compareBy({ it.episode.number },
            { when(it.quality) { "480p" -> 0; "360p" -> 1; else -> 2 } }, { it.url }))
}
