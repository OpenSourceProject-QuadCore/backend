package com.team.quadcore.backend.repository

import com.google.cloud.firestore.SetOptions
import com.team.quadcore.backend.model.Bus
import com.team.quadcore.backend.util.await
import org.springframework.stereotype.Repository

@Repository
class BusRepository(
    private val fs: FirestoreRepository
) {

    private val col get() = fs.db.collection("buses")

    suspend fun upsert(bus: Bus) {
        col.document(bus.id).set(bus, SetOptions.merge()).await()
    }

    suspend fun find(id: String): Bus? {
        val snap = col.document(id).get().await()
        return if (snap.exists()) snap.toObject(Bus::class.java) else null
    }

    suspend fun listActive(): List<Bus> {
        val q = col.whereEqualTo("active", true).get().await()
        return q.documents.mapNotNull { it.toObject(Bus::class.java) }
    }

    suspend fun delete(id: String) {
        col.document(id).delete().await()
    }
}