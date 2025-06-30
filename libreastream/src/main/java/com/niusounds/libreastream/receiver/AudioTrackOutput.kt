package com.niusounds.libreastream.receiver

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@RequiresApi(Build.VERSION_CODES.Q)
private fun defaultAudioTrackFactory(
    sampleRate: Int,
    channelMask: Int,
    bufferSize: Int
): AudioTrack {
    //return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        return AudioTrack.Builder()
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            //.setOffloadedPlayback(true)
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setChannelMask(channelMask)
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .build()
            )
            .build()


//    else {
//        AudioTrack(
//            AudioManager.STREAM_MUSIC,
//            sampleRate,
//            channelMask,
//            AudioFormat.ENCODING_PCM_FLOAT,
//            bufferSize,
//            AudioTrack.MODE_STREAM,
//        )
//    }
}

/**
 * Plays received [ReaStreamPacket] with system's default audio output device.
 * [sampleRate] and [channels] must be equal to received packets.
 *
 * [bufferScaleFactor] is used to enlarge [AudioTrack]'s buffer size.
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun Flow<ReaStreamPacket>.play(
    minbuf: Int,
    context: Context,
    sampleRate: Int,
    channels: Int = 1,
    bufferScaleFactor: Int = 1,
    audioTrackFactory: (sampleRate: Int, channelMask: Int, bufferSize: Int) -> AudioTrack = ::defaultAudioTrackFactory,
) {
//    val channelMask = when (channels) {
//        1 -> AudioFormat.CHANNEL_OUT_MONO
//        //2 -> AudioFormat.CHANNEL_OUT_STEREO
//        else -> error("unsupported channels")
//    }
    val bufferSize = bufferScaleFactor * AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_FLOAT
    ).coerceAtLeast(ReaStreamPacket.MAX_BLOCK_LENGTH * Float.SIZE_BYTES)
//    if(64<bufferSize) {
//        bufferSize=64
//    }
    val track = audioTrackFactory(sampleRate, AudioFormat.CHANNEL_OUT_MONO, bufferSize)
    //if(minbuf<=192)
        //track.setBufferSizeInFrames(minbuf+16)
    if(minbuf<=200)
        track.setBufferSizeInFrames(208)
    else track.setBufferSizeInFrames(minbuf)
    //track.setBufferSizeInFrames(minbuf)
    track.play()

    withContext(Dispatchers.Main){
        //Toast.makeText(context, "max buffer capacity "+track.bufferCapacityInFrames+" samples",Toast.LENGTH_LONG).show()
        Toast.makeText(context, "The min buffer size set is $bufferSize bytes",Toast.LENGTH_SHORT).show()
        Toast.makeText(context, "Buffer size set as "+track.bufferSizeInFrames.toString()+" samples", Toast.LENGTH_SHORT)
            .show()}
    //Toast.makeText(coroutineContext,track.playbackParams.toString(),Toast.LENGTH_LONG).show( )
    val audioData = FloatArray(ReaStreamPacket.MAX_BLOCK_LENGTH * 2)
    //val convertedSamples = FloatArray(ReaStreamPacket.MAX_BLOCK_LENGTH * 2)

    filter { it.isAudio && it.sampleRate == sampleRate }
        .onCompletion { track.release() }
        .collect { packet ->
            val packetChannels = packet.channels.toInt()
            val audioDataLength = packet.readAudio(audioData)
            track.write(
                audioData,
                0,
                audioDataLength,
                AudioTrack.WRITE_BLOCKING
            )
        }
}
