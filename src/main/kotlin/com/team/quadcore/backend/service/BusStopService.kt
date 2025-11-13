package com.team.quadcore.backend.service

import com.team.quadcore.backend.model.BusStop
import com.team.quadcore.backend.repository.BusStopRepository
import org.springframework.stereotype.Service

@Service
class BusStopService(private val repo: BusStopRepository) {
    suspend fun upsert(stop: BusStop) = repo.upsert(stop)
    suspend fun get(id: String) = repo.find(id)
    suspend fun search(prefix: String) = repo.searchByNamePrefix(prefix)
    suspend fun delete(id: String) = repo.delete(id)
}