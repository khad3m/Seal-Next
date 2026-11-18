package com.junkfood.seal.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/**
 * Lightweight, zero-bloat native Android MediaExtractor + MediaMuxer processor for
 * lossless video/audio trimming and instant audio track extraction.
 */
object MediaProcessor {

    private const val MIN_BUFFER_SIZE = 1024 * 1024 // 1 MB floor for high-bitrate 1080p/4K frames

    fun getDurationMs(file: File): Long {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(file.absolutePath)
            var maxUs = 0L
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                    maxUs = maxOf(maxUs, fmt.getLong(MediaFormat.KEY_DURATION))
                }
            }
            maxUs / 1000L
        } catch (_: Throwable) {
            0L
        } finally {
            runCatching { extractor.release() }
        }
    }

    /**
     * Losslessly trims [input] between [startMs] and [endMs] into [output] using MediaMuxer.
     * Starts at the sync/keyframe at or before [startMs] so the trimmed clip never begins with
     * corrupted P/B frames.
     */
    fun trimMedia(input: File, output: File, startMs: Long, endMs: Long): Result<File> =
        runCatching {
            require(endMs > startMs) { "End time must be after start time" }
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            try {
                extractor.setDataSource(input.absolutePath)
                val trackCount = extractor.trackCount
                muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

                val trackMap = HashMap<Int, Int>(trackCount)
                var maxBufSize = MIN_BUFFER_SIZE
                for (i in 0 until trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                        extractor.selectTrack(i)
                        val dstTrack = muxer.addTrack(format)
                        trackMap[i] = dstTrack
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            maxBufSize = maxOf(maxBufSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                    }
                }
                require(trackMap.isNotEmpty()) { "No supported audio/video tracks found" }

                muxer.start()
                val startUs = startMs * 1000L
                val endUs = endMs * 1000L
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

                val buffer = ByteBuffer.allocate(maxBufSize)
                val bufferInfo = MediaCodec.BufferInfo()
                var basePtsUs = -1L

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val sampleTime = extractor.sampleTime
                    if (sampleTime > endUs) break
                    val srcTrack = extractor.sampleTrackIndex
                    val dstTrack = trackMap[srcTrack]
                    if (dstTrack != null) {
                        if (basePtsUs < 0L) basePtsUs = sampleTime
                        bufferInfo.offset = 0
                        bufferInfo.size = sampleSize
                        bufferInfo.presentationTimeUs = (sampleTime - basePtsUs).coerceAtLeast(0L)
                        @Suppress("DEPRECATION")
                        bufferInfo.flags = extractor.sampleFlags
                        muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                    }
                    extractor.advance()
                }
                muxer.stop()
                output
            } finally {
                runCatching { muxer?.release() }
                runCatching { extractor.release() }
            }
        }

    /**
     * Losslessly extracts the audio track from [input] into an `.m4a` [output] file.
     */
    fun extractAudioTrack(input: File, output: File): Result<File> =
        runCatching {
            val extractor = MediaExtractor()
            var muxer: MediaMuxer? = null
            try {
                extractor.setDataSource(input.absolutePath)
                var audioTrack = -1
                var maxBufSize = MIN_BUFFER_SIZE
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                    if (mime.startsWith("audio/")) {
                        audioTrack = i
                        if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                            maxBufSize = maxOf(maxBufSize, format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                        }
                        break
                    }
                }
                require(audioTrack >= 0) { "No audio track found in file" }

                extractor.selectTrack(audioTrack)
                muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val dstTrack = muxer.addTrack(extractor.getTrackFormat(audioTrack))
                muxer.start()

                val buffer = ByteBuffer.allocate(maxBufSize)
                val bufferInfo = MediaCodec.BufferInfo()
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    @Suppress("DEPRECATION")
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrack, buffer, bufferInfo)
                    extractor.advance()
                }
                muxer.stop()
                output
            } finally {
                runCatching { muxer?.release() }
                runCatching { extractor.release() }
            }
        }
}
