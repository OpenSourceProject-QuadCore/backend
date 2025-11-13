package com.team.quadcore.backend.repository

import com.google.cloud.firestore.Firestore
import com.google.firebase.cloud.FirestoreClient
import org.springframework.stereotype.Component

@Component
class FirestoreRepository(val db: Firestore)
