package com.saisonflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

class OfferActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("title").orEmpty()
        val demo = intent.getBooleanExtra("demo", false)
        val store = DownloadStore(this)
        val engine = DownloadEngine(this, store)
        setContent {
            val colors = darkColorScheme(primary = Color(0xffff881e), background = Color(0xff0b101d))
            MaterialTheme(colorScheme = colors) {
                var busy by remember { mutableStateOf(demo) }
                var verified by remember { mutableStateOf(emptyList<Episode>()) }
                var sheet by rememberSaveable { mutableStateOf(false) }
                var message by rememberSaveable { mutableStateOf("") }
                var queued by rememberSaveable { mutableStateOf(intArrayOf()) }
                LaunchedEffect(demo) {
                    if (demo) {
                        verified = withContext(Dispatchers.IO) {
                            val catalog = assets.open("catalogue-exemple.json").bufferedReader().use { Catalogue.parse(it.readText()) }
                            catalog.bySeries[title]?.get(1).orEmpty().map { episode ->
                                async { episode.takeIf { runCatching { Mp4Probe.available(it.qualities.getValue("original")) }.getOrDefault(false) } }
                            }.awaitAll().filterNotNull()
                        }
                    }
                    busy = false
                }
                Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
                    Column(Modifier.systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        TextButton(onClick = { finish() }) { Text("Retour à la recherche") }
                        val poster = intent.getIntExtra("poster", 0)
                        if (poster != 0) Image(painterResource(poster), title, Modifier.fillMaxWidth().height(190.dp))
                        Text(title, style = MaterialTheme.typography.headlineLarge)
                        Text(intent.getStringExtra("provider").orEmpty(), color = colors.primary)
                        if (busy) Text("Vérification des fichiers…")
                        else if (verified.isEmpty()) Text("Aucune source téléchargeable autorisée disponible actuellement.")
                        else {
                            Text("Saison 1 · collection de démonstration", style = MaterialTheme.typography.titleLarge)
                            verified.forEach { Text(it.title) }
                            Button(onClick = { sheet = true }, enabled = verified.any { it.number !in queued }) { Text("Télécharger") }
                        }
                        if (message.isNotEmpty()) Text(message)
                    }
                }
                if (sheet && verified.isNotEmpty()) ModalBottomSheet(onDismissRequest = { sheet = false }) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Télécharger la sélection", style = MaterialTheme.typography.headlineSmall)
                        Text("MP4 original · résolution et langue non renseignées")
                        var selected by remember { mutableStateOf(verified.map { it.number }.filter { it !in queued }.toSet()) }
                        TextButton(onClick = { selected = verified.filter { it.number !in queued }.map { it.number }.toSet() }) { Text("Toute la saison") }
                        verified.forEach { episode ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(episode.number in selected, enabled = episode.number !in queued,
                                    onCheckedChange = { checked -> selected = if (checked) selected + episode.number else selected - episode.number })
                                Text(episode.title)
                            }
                        }
                        Button(enabled = selected.isNotEmpty(), onClick = {
                            var added = 0
                            verified.filter { it.number in selected && it.number !in queued }.forEach { episode ->
                                runCatching { engine.enqueue(episode.qualities.getValue("original"), episode.title, "original", intent.getBooleanExtra("wifi",false)) }
                                    .onSuccess { queued += episode.number; added++ }
                            }
                            message = "$added sur ${selected.size} ajouté(s) à la file"
                            sheet = false
                        }) { Text("Ajouter à la file") }
                    }
                }
            }
        }
    }
}
