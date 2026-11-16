package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RunRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val score: Long = 0L,
    val coinsCollected: Long = 0L,
    val distanceMeters: Long = 0L,
    val magnetCoins: Long = 0L,
    val jetpackMeters: Long = 0L,
    val skinId: String = KittySkin.GINGER.id,
    @ServerTimestamp val createdAt: Timestamp? = null
)
