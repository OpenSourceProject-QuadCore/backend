package com.team.quadcore.backend.controller

import com.team.quadcore.backend.service.BusDataService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/sync")
class DataSyncController(private val busDataService: BusDataService) {
    // 정적 데이터 호출, firebase db 동기화
    @PostMapping("/static-bus-data")
    fun synchronizeStaticBusData(): Mono<ResponseEntity<String>> {
        return busDataService.synchronizeStaticBusData()
            .then(Mono.just(ResponseEntity.ok("Static bus data synchronization started successfully.")))
            .onErrorResume { e ->
                Mono.just(ResponseEntity.status(500).body("Error during data synchronization: ${e.message}"))
            }
    }
}
