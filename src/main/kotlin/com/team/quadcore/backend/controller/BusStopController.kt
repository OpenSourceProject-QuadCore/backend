package com.team.quadcore.backend.controller

import com.team.quadcore.backend.model.BusStop
import com.team.quadcore.backend.service.BusStopService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bus_stops")
class BusStopController(private val service: BusStopService) {

    @PostMapping
    suspend fun upsert(@Valid @RequestBody stop: BusStop): ResponseEntity<Void> {
        service.upsert(stop)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): ResponseEntity<BusStop> {
        return service.get(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping("/search")
    suspend fun search(@RequestParam q: String): ResponseEntity<List<BusStop>> {
        return ResponseEntity.ok(service.search(q))
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}