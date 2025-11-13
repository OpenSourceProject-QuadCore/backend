package com.team.quadcore.backend.util

import com.google.api.core.ApiFuture
import java.util.concurrent.ExecutionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> ApiFuture<T>.await(): T {
    return suspendCoroutine { continuation ->
        addListener(
            {
                try {
                    continuation.resume(get())
                } catch (e: ExecutionException) {
                    continuation.resumeWithException(e.cause ?: e)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            },
            { it.run() }
        )
    }
}
