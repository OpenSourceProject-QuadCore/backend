package com.team.quadcore.backend.service

import com.team.quadcore.backend.model.Bus
import com.team.quadcore.backend.repository.BusRepository
import org.springframework.stereotype.Service

@Service
class BusService(private val repo: BusRepository) {
    suspend fun upsert(bus: Bus) = repo.upsert(bus)
    suspend fun get(id: String) = repo.find(id)
    suspend fun listActive() = repo.listActive()
    suspend fun delete(id: String) = repo.delete(id)
}