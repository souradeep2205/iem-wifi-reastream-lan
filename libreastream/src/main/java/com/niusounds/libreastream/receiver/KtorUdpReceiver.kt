package com.niusounds.libreastream.receiver

import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readByteBuffer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/**
 * Receive UDP data with Ktor.
 */
class KtorUdpReceiver(
    private val port: Int,
    private val ioContext: CoroutineContext,
) : PacketReceiver {
    override fun receive(): Flow<ByteBuffer> {
        val server = aSocket(ActorSelectorManager(ioContext))
            .udp()
            .bind(InetSocketAddress(port)){
                receiveBufferSize=(4800+47)*2
            }

        return server.incoming.consumeAsFlow()
            .map {
                it.packet.readByteBuffer().order(ByteOrder.LITTLE_ENDIAN)
            }.onCompletion {
                server.close()
            }
    }
}
