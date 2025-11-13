package com.team.quadcore.backend.service

import com.team.quadcore.backend.dto.*
import com.team.quadcore.backend.model.*
import com.team.quadcore.backend.repository.BusDataRepository
import com.team.quadcore.backend.util.GeoUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
class BusDataService(
    private val busDataRepository: BusDataRepository,
    webClientBuilder: WebClient.Builder,
    @Value("\${bus.api.key}") private val busServiceKey: String,
    @Value("\${stop.api.key}") private val stopServiceKey: String
) {

    private val webClient: WebClient = webClientBuilder.build()
    private val logger = LoggerFactory.getLogger(BusDataService::class.java)
    private val cityCode = "37050"

    private val busApiUrl = "https://apis.data.go.kr/1613000/BusRouteInfoInqireService/getRouteNoList"
    private val stopPerRouteApiUrl = "https://apis.data.go.kr/1613000/BusRouteInfoInqireService/getRouteAcctoThrghSttnList"
    private val stationApiUrl = "https://apis.data.go.kr/1613000/BusSttnInfoInqireService/getSttnNoList"

    fun synchronizeStaticBusData(): Mono<Void> {
        logger.info("Starting static bus data synchronization for cityCode: $cityCode")
        val now = Date()

        val allApiRoutesFlux = fetchAllRoutesFromApi()
        val allApiStopsFlux = fetchAllStopsFromApi()

        val collectedApiDataMono: Mono<List<Triple<BusItem, List<StopItem>, List<StationItem>>>> =
            Mono.zip(allApiRoutesFlux.collectList(), allApiStopsFlux.collectList())
                .flatMap { tuple ->
                    val apiRoutes = tuple.t1
                    val apiStops = tuple.t2
                    logger.info("Fetched ${apiRoutes.size} routes and ${apiStops.size} stops from API.")

                    Flux.fromIterable(apiRoutes)
                        .concatMap { apiRoute ->
                            fetchStopsForRouteFromApi(apiRoute.routeid!!)
                                .map { apiRouteStops -> Triple(apiRoute, apiRouteStops, apiStops) }
                                .onErrorResume { e ->
                                    logger.warn("Route-stops fetch failed (routeId=${apiRoute.routeid}): ${e.message}")
                                    Mono.just(Triple(apiRoute, emptyList(), apiStops))
                                }
                        }

                        .delayElements(java.time.Duration.ofMillis(80))
                        .collectList()
                }

        return collectedApiDataMono
            .flatMap { processedApiData ->
                logger.info("Processing and structuring all fetched data.")
                val (finalBuses, finalBusStops, finalBusRouteStops) = processAndStructureData(processedApiData, now)

                logger.info("Saving processed data to Firestore via Repository.")
                busDataRepository.saveStaticData(finalBuses, finalBusStops, finalBusRouteStops)
            }
            .doOnSuccess { logger.info("Successfully synchronized static bus data.") }
            .doOnError { error -> logger.error("Error during static bus data synchronization", error) }
            .then()
    }


    private fun fetchAllRoutesFromApi(): Flux<BusItem> {
        val uri = UriComponentsBuilder.fromHttpUrl(busApiUrl)
            .queryParam("serviceKey", busServiceKey)
            .queryParam("pageNo", "1")
            .queryParam("numOfRows", "1000")
            .queryParam("_type", "json")
            .queryParam("cityCode", cityCode)
            .build().encode().toUri()
        return webClient.get().uri(uri).retrieve().bodyToMono(BusApiResponse::class.java)
            .flatMapIterable { it.response?.body?.items?.item ?: emptyList() }
            .filter { it.routeid != null && it.routeno != null }
    }

    private fun fetchAllStopsFromApi(): Flux<StationItem> {
        val uri = UriComponentsBuilder.fromHttpUrl(stationApiUrl)
            .queryParam("serviceKey", stopServiceKey)
            .queryParam("pageNo", "1")
            .queryParam("numOfRows", "3000")
            .queryParam("_type", "json")
            .queryParam("cityCode", cityCode)
            .build().encode().toUri()
        return webClient.get().uri(uri).retrieve().bodyToMono(StationListApiResponse::class.java)
            .flatMapIterable { it.response?.body?.items?.item ?: emptyList() }
            .filter { it.nodeid != null && it.nodenm != null && it.gpslati != null && it.gpslong != null }
    }

    private fun fetchStopsForRouteFromApi(routeId: String): Mono<List<StopItem>> {
        val uri = UriComponentsBuilder.fromHttpUrl(stopPerRouteApiUrl)
            .queryParam("serviceKey", busServiceKey)
            .queryParam("pageNo", "1")
            .queryParam("numOfRows", "1000")
            .queryParam("_type", "json")
            .queryParam("cityCode", cityCode)
            .queryParam("routeId", routeId)
            .build().encode().toUri()

        return webClient.get().uri(uri)
            .retrieve()
            .bodyToMono(StopApiResponse::class.java)
            .map { it.response?.body?.items?.item ?: emptyList() }
            .retryWhen(
                reactor.util.retry.Retry.backoff(3, java.time.Duration.ofMillis(300))
                    .maxBackoff(java.time.Duration.ofSeconds(5))
                    .jitter(0.2)
                    .filter { t ->
                        t is org.springframework.web.reactive.function.client.WebClientRequestException ||
                                t.cause is reactor.netty.http.client.PrematureCloseException
                    }
            )
    }



    private fun processAndStructureData(
        processedApiData: List<Triple<BusItem, List<StopItem>, List<StationItem>>>,
        now: Date
    ): Triple<List<Bus>, List<BusStop>, List<BusRouteStop>> {

        val finalBuses = mutableListOf<Bus>()
        val finalBusRouteStops = mutableListOf<BusRouteStop>()
        val stopsToBusesMap = mutableMapOf<String, MutableList<BusLite>>()

        processedApiData.forEach { (apiRoute, apiRouteStops, _) ->
            val stopsLite = apiRouteStops
                .filter { it.nodeid != null && it.nodeord != null }
                .sortedBy { it.nodeord }
                .map { BusStopLite(it.nodeid!!, it.nodeord!!) }

            apiRouteStops.forEach { rs ->
                if (rs.nodeid != null && rs.nodeord != null) {
                    finalBusRouteStops.add(BusRouteStop(
                        routeId = apiRoute.routeid!!,
                        stopId = rs.nodeid!!,
                        sequence = rs.nodeord!!,
                        direction = rs.updowncd?.toString(),
                        cityCode = cityCode,
                        updatedAt = now
                    ))
                    val busLite = BusLite(apiRoute.routeid!!, apiRoute.routeno!!, apiRoute.routetp)
                    stopsToBusesMap.computeIfAbsent(rs.nodeid!!) { mutableListOf() }.add(busLite)
                }
            }

            finalBuses.add(Bus(
                id = apiRoute.routeid!!,
                shortName = apiRoute.routeno!!,
                routeType = apiRoute.routetp,
                startStopName = apiRoute.startnodenm,
                endStopName = apiRoute.endnodenm,
                firstBusTime = apiRoute.startvehicletime,
                lastBusTime = apiRoute.endvehicletime,
                cityCode = cityCode,
                stopsLite = stopsLite,
                updatedAt = now,
                active = true
            ))
        }

        val allApiStops = processedApiData.firstOrNull()?.third ?: emptyList()
        val finalBusStops = allApiStops.map { apiStop ->
            val lat = apiStop.gpslati!!
            val lng = apiStop.gpslong!!
            BusStop(
                id = apiStop.nodeid!!,
                name = apiStop.nodenm!!,
                code = apiStop.nodenno,
                lat = lat,
                lng = lng,
                geohash = GeoUtil.calculateGeohash(lat, lng),
                cityCode = cityCode,
                busesLite = stopsToBusesMap[apiStop.nodeid!!]?.distinct() ?: emptyList(),
                updatedAt = now
            )
        }

        return Triple(finalBuses, finalBusStops, finalBusRouteStops.distinct())
    }
}