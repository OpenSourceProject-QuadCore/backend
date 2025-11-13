package com.team.quadcore.backend.repository

import com.google.api.gax.rpc.ResourceExhaustedException
import com.google.cloud.firestore.Firestore
import com.team.quadcore.backend.model.Bus
import com.team.quadcore.backend.model.BusRouteStop
import com.team.quadcore.backend.model.BusStop
import io.grpc.StatusRuntimeException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import kotlin.math.min
import kotlin.random.Random

private const val BUSES = "buses"
private const val BUS_STOPS = "bus_stops"
private const val BUS_ROUTE_STOPS = "bus_route_stops"

@Repository
class BusDataRepository(private val firestore: Firestore) {

    private val logger = LoggerFactory.getLogger(BusDataRepository::class.java)


    fun saveStaticData(
        buses: List<Bus>,
        busStops: List<BusStop>,
        busRouteStops: List<BusRouteStop>
    ): Mono<Void> {
        logger.info("Preparing to save data counts: ${BUSES}=${buses.size}, ${BUS_STOPS}=${busStops.size}, ${BUS_ROUTE_STOPS}=${busRouteStops.size}")

        return Mono.fromCallable {
            commitInChunks(buses, 250) { batch, bus ->
                val ref = firestore.collection(BUSES).document(bus.id.safeId())
                batch.set(ref, bus)
            }
            commitInChunks(busStops, 250) { batch, stop ->
                val ref = firestore.collection(BUS_STOPS).document(stop.id.safeId())
                batch.set(ref, stop)
            }
            commitInChunks(busRouteStops, 250) { batch, rs ->
                val docId = "${rs.routeId}_${rs.stopId}_${rs.sequence}".safeId()
                val ref = firestore.collection(BUS_ROUTE_STOPS).document(docId)
                batch.set(ref, rs)
            }
            null
        }.subscribeOn(Schedulers.boundedElastic()).then()
    }


    private fun <T> commitInChunks(
        items: List<T>,
        chunkSize: Int = 250,
        writer: (com.google.cloud.firestore.WriteBatch, T) -> Unit
    ) {
        if (items.isEmpty()) return
        val chunks = items.chunked(chunkSize)
        chunks.forEachIndexed { idx, chunk ->
            val batch = firestore.batch()
            chunk.forEach { writer(batch, it) }

            var attempt = 0
            val maxAttempts = 6
            var backoffMs = 200L

            while (true) {
                try {
                    logger.info(
                        "Committing chunk {}/{} (size={}) attempt={}",
                        idx + 1,
                        chunks.size,
                        chunk.size,
                        attempt + 1
                    )
                    batch.commit().get()
                    break
                } catch (e: java.util.concurrent.ExecutionException) {
                    val cause = e.cause
                    val isQuota =
                        cause is ResourceExhaustedException ||
                                (cause is StatusRuntimeException && cause.status.code.name == "RESOURCE_EXHAUSTED")

                    if (!isQuota || attempt + 1 >= maxAttempts) {
                        throw e
                    }

                    val jitter = Random.nextLong(0, 150)
                    val sleep = min(5_000L, backoffMs) + jitter
                    logger.warn("Quota hit (RESOURCE_EXHAUSTED). Backing off {} ms then retry…", sleep)
                    Thread.sleep(sleep)
                    backoffMs *= 2
                    attempt++
                }
            }

            Thread.sleep(200) // 필요에 따라 100~400ms로 조절
        }
    }
}

private fun String.safeId(): String = replace(Regex("[^A-Za-z0-9_\\-]"), "_")