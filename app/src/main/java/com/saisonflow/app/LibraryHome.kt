package com.saisonflow.app

import android.app.DownloadManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import java.util.zip.GZIPInputStream

private val navy = Color(0xff0b101d)
private val panel = Color(0xff1b2538)
private val orange = Color(0xffff851c)
private const val collection = "Cinéma Blender"
private data class Title(val name: String, val catalog: String, val demo: Boolean = false)

class LibraryHome : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = orange, background = navy, surface = panel)) {
                var query by rememberSaveable { mutableStateOf("") }
                var tab by rememberSaveable { mutableStateOf("Accueil") }
                var revision by remember { mutableIntStateOf(0) }
                val scope = rememberCoroutineScope()
                var importStatus by remember { mutableStateOf("") }
                val importCatalog = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
                    if (uri != null) scope.launch {
                        importStatus = runCatching { withContext(Dispatchers.IO) { MediaCatalogs(this@LibraryHome).importAuthorized(uri) }; revision++; "Catalogue ajouté" }
                            .getOrElse { "Import impossible : " + it.localizedMessage }
                    }
                }
                val catalog by produceState<List<Title>>(emptyList(),revision) {
                    value = withContext(Dispatchers.IO) {
                        val json = assets.open("animes.json").bufferedReader().use { JSONArray(it.readText()) }
                        buildList {
                            add(Title(collection, "Collection libre · Démonstration", true))
                            MediaCatalogs(this@LibraryHome).providers().flatMap { it.series().keys }.distinct().filter { it != collection }.forEach { add(Title(it,"Catalogue autorisé",true)) }
                            repeat(json.length()) { add(Title(json.getJSONObject(it).getString("title"), "Index animé")) }
                            GZIPInputStream(assets.open("french-stream.tsv.gz")).bufferedReader().useLines { lines ->
                                lines.forEach { add(Title(it.substringAfter('-').removeSuffix(".html").replace('-', ' '), "Index French Stream")) }
                            }
                        }
                    }
                }
                Scaffold(containerColor = navy, bottomBar = {
                    NavigationBar(containerColor = navy) {
                        listOf("Accueil" to "⌂", "Recherche" to "⌕", "Bibliothèque" to "▤").forEach { (label, icon) ->
                            NavigationBarItem(selected = tab == label, onClick = { tab = label }, icon = { Text(icon) }, label = { Text(label) })
                        }
                    }
                }) { padding ->
                    Column(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(painterResource(R.drawable.flow_mark), null, Modifier.size(42.dp))
                            Text("Saison", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Text("Flow", style = MaterialTheme.typography.headlineMedium, color = orange, fontWeight = FontWeight.Bold)
                        }
                        if (tab == "Bibliothèque") {
                            TextButton(onClick = { importCatalog.launch(arrayOf("application/json", "text/plain")) }) { Text("Importer mon catalogue autorisé") }
                            if (importStatus.isNotBlank()) Text(importStatus)
                            Library()
                        } else {
                            OutlinedTextField(query, { query = it; tab = "Recherche" }, singleLine = true,
                                placeholder = { Text("Rechercher un animé") }, modifier = Modifier.fillMaxWidth())
                            val results by produceState<List<Title>>(emptyList(), query, catalog) {
                                value = withContext(Dispatchers.Default) {
                                    val term = query.trim()
                                    if (term.isEmpty()) catalog.filter { it.demo || MainActivity.cover(it.name) != 0 }
                                    else catalog.asSequence().filter { it.name.contains(term, true) || (it.name == collection && ("Big Buck Bunny Sintel").contains(term,true)) }
                                        .distinctBy { it.name.lowercase() }.take(100).toList()
                                }
                            }
                            Text(if (query.isBlank()) "À découvrir" else "Résultats", style = MaterialTheme.typography.titleLarge)
                            if (catalog.isEmpty()) CircularProgressIndicator()
                            else if (results.isEmpty()) Text("Aucun résultat pour cette recherche.")
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(results) { item ->
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                                        startActivity(Intent(this@LibraryHome, OfferActivity::class.java)
                                            .putExtra("title",item.name).putExtra("provider",item.catalog)
                                            .putExtra("demo",item.demo).putExtra("poster",MainActivity.cover(item.name)))
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                        Surface(color = panel, modifier = Modifier.fillMaxWidth()) {
                                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                val poster = MainActivity.cover(item.name)
                                                if (poster != 0) Image(painterResource(poster), null, Modifier.size(85.dp,115.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                                Column(Modifier.padding(start = 12.dp)) {
                                                    Text(item.name, style = MaterialTheme.typography.titleLarge)
                                                    Text(item.catalog, color = orange)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun Library() {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val ready by produceState<List<DownloadStore.Record>>(emptyList()) {
            while (true) {
                value = withContext(Dispatchers.IO) {
                    DownloadStore(this@LibraryHome).use { store ->
                        store.page(0,store.count()).filter { manager.getUriForDownloadedFile(it.id) != null }
                    }
                }
                delay(3000)
            }
        }
        Text("Bibliothèque hors connexion", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = { startActivity(Intent(this,MainActivity::class.java).putExtra("section","Téléchargés")) }) { Text("Gérer les transferts") }
        if (ready.isEmpty()) Text("Aucune vidéo téléchargée.")
        LazyColumn {
            items(ready) { record ->
                ListItem(headlineContent = { Text(record.title) }, supportingContent = { Text("Lire hors connexion") },
                    modifier = Modifier.clickable {
                        manager.getUriForDownloadedFile(record.id)?.let { uri ->
                            startActivity(Intent(this@LibraryHome, LocalPlayer::class.java).setData(uri))
                        }
                    })
            }
        }
    }
}
