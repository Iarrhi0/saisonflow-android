package com.saisonflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class LocalPlayer : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var position = 0L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        position = savedInstanceState?.getLong("position") ?: 0L
    }
    override fun onStart() {
        super.onStart()
        val uri = intent.data
        if (uri?.scheme != "content") { finish(); return }
        val view = PlayerView(this)
        setContentView(view)
        player = ExoPlayer.Builder(this).build().also {
            view.player = it
            it.setMediaItem(MediaItem.fromUri(uri)); it.seekTo(position); it.prepare(); it.play()
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong("position",player?.currentPosition ?: position)
        super.onSaveInstanceState(outState)
    }
    override fun onStop() {
        player?.let { position = it.currentPosition; it.release() }
        player = null
        super.onStop()
    }
}
