package com.junkfood.seal.util

import android.content.Context
import com.junkfood.seal.App
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

// Lazily initializes yt-dlp, FFmpeg and aria2c on first real use instead of at app start.
object Engines {
    @Volatile private var ready = false

    fun yt(): YoutubeDL { ensure(); return YoutubeDL.getInstance() }

    fun ensure(context: Context = App.context) {
        if (ready) return
        synchronized(this) {
            if (ready) return
            val app = context.applicationContext
            YoutubeDL.init(app)
            FFmpeg.init(app)
            Aria2c.init(app)
            ready = true
        }
    }
}
