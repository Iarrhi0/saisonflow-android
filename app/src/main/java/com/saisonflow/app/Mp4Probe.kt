package com.saisonflow.app

import java.io.DataInputStream
import java.net.HttpURLConnection
import java.net.URL

/** Read the ISO BMFF signature before offering a file from the licensed demo catalogue. */
internal object Mp4Probe {
    fun matches(prefix: ByteArray) = prefix.size >= 8 &&
        String(prefix, 4, 4, Charsets.US_ASCII) == "ftyp"

    fun available(address: String): Boolean {
        val url = URL(address)
        require(url.protocol == "https")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Range", "bytes=0-31")
            if (connection.responseCode !in setOf(200, 206)) return false
            val prefix = ByteArray(8)
            DataInputStream(connection.inputStream).use { it.readFully(prefix) }
            matches(prefix)
        } finally {
            connection.disconnect()
        }
    }
}
