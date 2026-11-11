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
                var seasons by remember { mutableStateOf<Map<Int,List<Episode>>>(emptyMap()) }
                var season by rememberSaveable { mutableIntStateOf(0) }
                var offers by remember { mutableStateOf(emptyList<MediaCandidate>()) }
                var busy by remember { mutableStateOf(demo) }
                var verified by remember { mutableStateOf(emptyList<Episode>()) }
                var sheet by rememberSaveable { mutableStateOf(false) }
                var message by rememberSaveable { mutableStateOf("") }
                var queued by rememberSaveable(season) { mutableStateOf(intArrayOf()) }
                LaunchedEffect(title) {
                    seasons = withContext(Dispatchers.IO) { MediaCatalogs(this@OfferActivity).seasons(title) }
                    if (season !in seasons) season = seasons.keys.firstOrNull() ?: 0
                }
                LaunchedEffect(seasons,season) {
                    busy = true
                    offers = resolveEpisodes(seasons[season].orEmpty())
                    verified = offers.distinctBy { it.episode.number }.map { it.episode }
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
                        if (seasons.size > 1) seasons.keys.forEach { n ->
                            TextButton(onClick = { season = n; verified = emptyList(); offers = emptyList() }) { Text("Saison $n") }
                        }
                        if (busy) Text("Vérification des fichiers…")
                        else if (verified.isEmpty()) Text("Aucune source téléchargeable autorisée disponible actuellement.")
                        else {
                            Text("Saison $season · ${verified.size} disponible(s)", style = MaterialTheme.typography.titleLarge)
                            verified.forEach { Text(it.title) }
                            Button(onClick = { sheet = true }, enabled = verified.any { it.number !in queued }) { Text("Télécharger") }
                        }
                        if (message.isNotEmpty()) Text(message)
                    }
                }
                if (sheet && verified.isNotEmpty()) ModalBottomSheet(onDismissRequest = { sheet = false }) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Télécharger la sélection", style = MaterialTheme.typography.headlineSmall)
                        var requested by remember { mutableStateOf("Auto") }
                        val eligible = offers.filter { requested == "Auto" || it.quality == requested }.distinctBy { it.episode.number }
                        Text("Piste audio non renseignée")
                        Row { (listOf("Auto") + offers.map { it.quality }.distinct()).forEach { key ->
                            TextButton(onClick = { requested = key }) { Text(if (requested == key) "✓ $key" else key) }
                        } }
                        var selected by remember(requested) { mutableStateOf(eligible.map { it.episode.number }.filter { it !in queued }.toSet()) }
                        TextButton(onClick = { selected = eligible.map { it.episode.number }.filter { it !in queued }.toSet() }) { Text("Toute la saison") }
                        eligible.map { it.episode }.forEach { episode ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(episode.number in selected, enabled = episode.number !in queued,
                                    onCheckedChange = { checked -> selected = if (checked) selected + episode.number else selected - episode.number })
                                Text(episode.title)
                            }
                        }
                        Button(enabled = selected.isNotEmpty(), onClick = {
                            var added = 0
                            eligible.filter { it.episode.number in selected && it.episode.number !in queued }.forEach { offer ->
                                val episode = offer.episode
                                runCatching { engine.enqueue(offer.url, episode.title, offer.quality, intent.getBooleanExtra("wifi",false)) }
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
