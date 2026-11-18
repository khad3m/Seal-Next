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
    private val client by lazy {
        OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).build()
    }

    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()
        val builder =
            okhttp3.Request.Builder()
                .method(request.httpMethod(), body)
                .url(request.url())
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0")
        request.headers().forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { builder.addHeader(name, it) }
        }
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
