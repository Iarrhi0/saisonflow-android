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
        assertEquals(3, records.size)
        val expected = mapOf(
            "Big Buck Bunny" to "b2ded9ae5a20fa36ca8cec49ef923bffb5e1a51d9e8c1f8336273d2fa9d35ff0",
            "Big Buck Bunny · film complet" to "4deeba5c85bffc43af78857bb7bbfeb8dd09d34d11759e644a0f63815013fac1",
            "Sintel" to "b670602fa00934ca27c4351bb0efe7ea7a07fae57284e44226025eeed7c51254")
        val deadline = SystemClock.elapsedRealtime() + 360000
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
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
        shell("cmd connectivity airplane-mode enable")
        try {
            val network = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val until = SystemClock.elapsedRealtime() + 15000
            while (network.activeNetwork != null && SystemClock.elapsedRealtime() < until) SystemClock.sleep(200)
            assertNull("Connexion encore active", network.activeNetwork)
            val intent = android.content.Intent(context, LocalPlayer::class.java).setData(manager.getUriForDownloadedFile(records.first().id))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            androidx.test.core.app.ActivityScenario.launch<LocalPlayer>(intent).use { scenario ->
                var played = false
                val timeout = SystemClock.elapsedRealtime() + 20000
                while (!played && SystemClock.elapsedRealtime() < timeout) {
                    scenario.onActivity { activity ->
                        val root = activity.findViewById<android.view.ViewGroup>(android.R.id.content)
                        val view = root.getChildAt(0) as androidx.media3.ui.PlayerView
                        val p = view.player
                        played = p != null && p.isPlaying && p.currentPosition > 750 && p.videoSize.height > 0
                    }
                    SystemClock.sleep(200)
                }
                assertTrue("Lecture vidéo locale en mode avion", played)
            }
        } finally { shell("cmd connectivity airplane-mode disable") }
    }
}
