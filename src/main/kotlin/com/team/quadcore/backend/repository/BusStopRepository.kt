package com.team.quadcore.backend.repository

import com.google.cloud.firestore.SetOptions
import com.team.quadcore.backend.model.BusStop
import com.team.quadcore.backend.util.await
import org.springframework.stereotype.Repository

@Repository
class BusStopRepository(
    private val fs: FirestoreRepository
) {
    private val col get() = fs.db.collection("bus_stops")

    suspend fun upsert(stop: BusStop) {
        col.document(stop.id).set(stop, SetOptions.merge()).await()
    }

    suspend fun find(id: String): BusStop? {
        val snap = col.document(id).get().await()
        return if (snap.exists()) snap.toObject(BusStop::class.java)?.copy(id = snap.id) else null
    }

    suspend fun searchByNamePrefix(prefix: String, limit: Int = 20): List<BusStop> {
        val end = prefix + "\uf8ff"
        val q = col.orderBy("name").startAt(prefix).endAt(end).limit(limit).get().await()
        return q.documents.mapNotNull { it.toObject(BusStop::class.java)?.copy(id = it.id) }
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}