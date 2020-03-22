package com.github.mikibemiki.mppktx.service

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import com.github.mikibemiki.mppktx.coroutines.awaitNonNull
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapMerge

open class BoundService<B : IBinder>(
    context: Context,
    scope: CoroutineScope,
    private val boundTimeout: Long = 1_000L,
    binding: (serviceConnection: ServiceConnection) -> Unit
) {

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            binderChannel.offer(null)
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            @Suppress("UNCHECKED_CAST")
            binderChannel.offer(service as B)
        }
    }

    private val binderChannel = ConflatedBroadcastChannel<B?>()

    init {
        //Launch Job which waits until scope is canceled and then unbinds service
        scope.launch {
            try {
                delay(Long.MAX_VALUE)
            } catch (c: CancellationException) {
                context.unbindService(serviceConnection)
            }
        }
        binding(serviceConnection)
    }

    /**
     * Waits [boundTimeout] for service to bind. If it fails throws
     * [TimeoutCancellationException]
     */
    suspend operator fun <T> invoke(block: suspend B.() -> T): T {
        return withTimeout(boundTimeout) {
            block(binderChannel.awaitNonNull())
        }
    }

    /**
     * Maps flows from service after connects or reconnects to resulting flow.
     */
    fun <T> mapFlow(block: suspend B.() -> Flow<T>): Flow<T> {
        return binderChannel
            .asFlow()
            .flatMapMerge {
                block(it ?: return@flatMapMerge emptyFlow<T>())
            }
    }
}

