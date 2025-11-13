package com.team.quadcore.backend.controller

import com.team.quadcore.backend.model.Bus
import com.team.quadcore.backend.service.BusService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/buses")
class BusController(private val service: BusService) {

    @PostMapping
    suspend fun upsert(@Valid @RequestBody bus: Bus): ResponseEntity<Void> {
        service.upsert(bus)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{id}")
    suspend fun get(@PathVariable id: String): ResponseEntity<Bus> {
        return service.get(id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }

    @GetMapping
    suspend fun listActive(): ResponseEntity<List<Bus>> {
        return ResponseEntity.ok(service.listActive())
    }

    @DeleteMapping("/{id}")
    suspend fun delete(@PathVariable id: String): ResponseEntity<Void> {
        service.delete(id)
        return ResponseEntity.noContent().build()
    }
}