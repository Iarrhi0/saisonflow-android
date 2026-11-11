package com.saisonflow.app

import android.app.DownloadManager
import android.content.Context
import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class CompletedMediaTest {
    @Test fun queuedFilesMatchPublishedMedia() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val records = DownloadStore(context).use { it.page(0, 3) }
        assertEquals(2, records.size)
        val expected = mapOf(
            "Big Buck Bunny" to "b2ded9ae5a20fa36ca8cec49ef923bffb5e1a51d9e8c1f8336273d2fa9d35ff0",
            "Sintel" to "b670602fa00934ca27c4351bb0efe7ea7a07fae57284e44226025eeed7c51254")
        val deadline = SystemClock.elapsedRealtime() + 180000
        for (record in records) {
            var state = -1
            var reason = -1
            while (SystemClock.elapsedRealtime() < deadline) {
                manager.query(DownloadManager.Query().setFilterById(record.id)).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    state = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                }
                if (state == DownloadManager.STATUS_SUCCESSFUL || state == DownloadManager.STATUS_FAILED) break
                SystemClock.sleep(1000)
            }
            assertEquals(record.title + " raison Android " + reason, DownloadManager.STATUS_SUCCESSFUL, state)
            val uri = requireNotNull(manager.getUriForDownloadedFile(record.id))
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)!!.use { input ->
                val buf = ByteArray(65536)
                while (true) {
                    val size = input.read(buf)
                    if (size < 0) break
                    digest.update(buf, 0, size)
                }
            }
            assertEquals(expected.getValue(record.title), digest.digest().joinToString("") { "%02x".format(it) })
        }
    }
}
