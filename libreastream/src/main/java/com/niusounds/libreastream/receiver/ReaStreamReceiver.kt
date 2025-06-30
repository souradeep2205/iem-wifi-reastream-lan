package com.niusounds.libreastream.receiver

import com.niusounds.libreastream.ReaStream
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Receive [ReaStreamPacket]s as [Flow].
 */
fun receiveReaStream(
    dispatcher: ExecutorCoroutineDispatcher,
    receiver: PacketReceiver = KtorUdpReceiver(
        port = ReaStream.DEFAULT_PORT,
        ioContext = dispatcher
    ),
): Flow<ReaStreamPacket> {
    val flow = receiver.receive().map { ByteBufferReaStreamPacket(it) }
    return flow
}
