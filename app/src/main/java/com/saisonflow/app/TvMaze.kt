package com.saisonflow.app

import android.net.Uri
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

internal object TvMaze {
    private fun fetch(path: String): JSONArray {
        val connection = URL("https://api.tvmaze.com/$path").openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 6000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent","SaisonFlow/0.2")
            connection.inputStream.bufferedReader().use { JSONArray(it.readText()) }
        } finally { connection.disconnect() }
    }
    fun search(query: String): List<Title> {
        val rows = fetch("search/shows?q=" + Uri.encode(query))
        return (0 until rows.length()).map {
            val show = rows.getJSONObject(it).getJSONObject("show")
            val genres = show.getJSONArray("genres")
            val info = (listOf(show.optString("premiered").take(4)) +
                (0 until genres.length()).map(genres::getString)).filter { it.isNotBlank() && it != "null" }
            Title(show.getString("name"), "TVmaze", id = show.getInt("id"), info = info.joinToString(" · "))
        }
    }
    fun seasons(id: Int): Map<Int,List<Episode>> {
        require(id > 0)
        val rows = fetch("shows/$id/episodes")
        return (0 until rows.length()).mapNotNull {
            val ep = rows.getJSONObject(it)
            val number = ep.optInt("number")
            val season = ep.optInt("season")
            if (season > 0 && number > 0) Episode("",season,number,ep.getString("name"),emptyMap()) else null
        }.groupBy { it.season }
    }
}
