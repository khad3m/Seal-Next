package com.junkfood.seal.ui.page.stream

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

// OkHttp backed Downloader required by NewPipe Extractor. Used for playback only.
object NewPipeDownloader : Downloader() {
    // Modern Android Chrome UA: YouTube serves cleaner responses (fewer bot checks and
    // consent redirects) to mobile Chrome than to the previous desktop Firefox UA.
    private const val UA =
        "Mozilla/5.0 (Linux; Android 15; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            // bug #5: a hard per-call deadline. readTimeout alone is per-read — a server
            // that dribbles bytes slowly could stall extraction indefinitely, leaving the
            // stream player loading forever.
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()
        val builder =
            okhttp3.Request.Builder()
                .method(request.httpMethod(), body)
                .url(request.url())
                .addHeader("User-Agent", UA)
                .addHeader("Accept-Language", "en-US,en;q=0.9")
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
        // YouTube EU consent wall: without a consent cookie, search/trending extraction
        // can bounce off consent.google.com and come back empty. SOCS=CAI is the same
        // "essential cookies only" consent the NewPipe app itself sends.
        val hasCookie = request.headers().any { it.key.equals("Cookie", ignoreCase = true) }
        val ytUrl = request.url().contains("youtube.com") || request.url().contains("youtu.be")
        if (ytUrl && !hasCookie) builder.addHeader("Cookie", "SOCS=CAI")
        client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 429) throw ReCaptchaException("reCaptcha challenge", request.url())
            return Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                resp.body?.string(),
                resp.request.url.toString(),
            )
        }
    }
}
