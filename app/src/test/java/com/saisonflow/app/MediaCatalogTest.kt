package com.saisonflow.app

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MediaCatalogTest {
    @Test fun choosesWorking480RegardlessOfProviderOrder() = runBlocking {
        val failed = Episode("Collection",1,1,"Film",mapOf("480p" to "https://failed.example/a.mp4"))
        val works = Episode("Collection",1,1,"Film",mapOf("360p" to "https://working.example/b.mp4","480p" to "https://working.example/a.mp4"))
        for (providers in listOf(listOf(failed,works),listOf(works,failed))) {
            val offers = resolveEpisodes(providers) { "working" in it }
            assertEquals(listOf("480p","360p"),offers.map { it.quality })
        }
        assertTrue(resolveEpisodes(listOf(failed)) { false }.isEmpty())
    }
}
