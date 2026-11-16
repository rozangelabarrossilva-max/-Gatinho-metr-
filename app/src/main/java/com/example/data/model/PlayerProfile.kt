package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class KittySkin(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val priceCoins: Long,
    val furRgb: Triple<Float, Float, Float>,
    val bellyRgb: Triple<Float, Float, Float>,
    val accentRgb: Triple<Float, Float, Float>,
    val previewHex: Long
) {
    GINGER(
        id = "ginger",
        displayName = "Miau Laranja",
        subtitle = "Listrado clássico do metrô",
        priceCoins = 0L,
        furRgb = Triple(0.98f, 0.56f, 0.16f),
        bellyRgb = Triple(1.0f, 0.92f, 0.78f),
        accentRgb = Triple(0.0f, 0.90f, 1.0f),
        previewHex = 0xFFFA8F29
    ),
    SIAMESE(
        id = "siamese",
        displayName = "Luna Siberiana",
        subtitle = "Branco lunar com cachecol azul",
        priceCoins = 120L,
        furRgb = Triple(0.92f, 0.94f, 0.98f),
        bellyRgb = Triple(0.78f, 0.85f, 0.96f),
        accentRgb = Triple(0.25f, 0.55f, 1.0f),
        previewHex = 0xFFEAF0FA
    ),
    NEON(
        id = "neon",
        displayName = "Neko Neon",
        subtitle = "Roxo elétrico com fone ciano",
        priceCoins = 260L,
        furRgb = Triple(0.56f, 0.24f, 0.92f),
        bellyRgb = Triple(0.84f, 0.72f, 1.0f),
        accentRgb = Triple(0.17f, 0.92f, 0.64f),
        previewHex = 0xFF8F3DEA
    ),
    GOLD(
        id = "gold",
        displayName = "Tigrinho Dourado",
        subtitle = "Pelagem ouro real dos trilhos",
        priceCoins = 500L,
        furRgb = Triple(1.0f, 0.80f, 0.18f),
        bellyRgb = Triple(1.0f, 0.96f, 0.75f),
        accentRgb = Triple(1.0f, 0.24f, 0.41f),
        previewHex = 0xFFFFCC2E
    );

    companion object {
        fun fromId(id: String): KittySkin = entries.firstOrNull { it.id == id } ?: GINGER
    }
}

enum class PowerUpType(val title: String, val description: String) {
    MAGNET("Ímã de Moedas", "Aumenta a duração do ímã para puxar moedas"),
    JETPACK("Mochila a Jato", "Aumenta o tempo de voo sobre os trens")
}

data class MissionProgressItem(
    val id: String,
    val title: String,
    val current: Long,
    val target: Long,
    val iconType: MissionIconType
) {
    val isCompleted: Boolean get() = current >= target
    val fraction: Float get() = if (target <= 0L) 1f else (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

enum class MissionIconType { MAGNET, JETPACK, DISTANCE, TRAINS }

@IgnoreExtraProperties
data class PlayerProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Gatinho",
    val highScore: Long = 0L,
    val totalCoins: Long = 0L,
    val totalRuns: Long = 0L,
    val scoreMultiplier: Long = 1L,
    val selectedSkin: String = KittySkin.GINGER.id,
    val unlockedSkins: List<String> = listOf(KittySkin.GINGER.id),
    val magnetLevel: Long = 1L,
    val jetpackLevel: Long = 1L,
    val missionTier: Long = 1L,
    val missionCoinsWithMagnet: Long = 0L,
    val missionJetpackFlights: Long = 0L,
    val missionTotalMeters: Long = 0L,
    val missionTrainsPassed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun magnetDurationSeconds(): Float = 7.5f + (magnetLevel.coerceIn(1L, 5L) - 1L) * 2.0f
    fun jetpackDurationSeconds(): Float = 6.5f + (jetpackLevel.coerceIn(1L, 5L) - 1L) * 1.8f

    fun activeMissions(): List<MissionProgressItem> {
        val tier = missionTier.coerceAtLeast(1L)
        val magnetTarget = 20L + (tier - 1L) * 15L
        val jetpackTarget = 1L + ((tier - 1L) / 2L)
        val trainsOrMetersUseTrains = tier % 2L == 0L
        val thirdMission = if (trainsOrMetersUseTrains) {
            MissionProgressItem(
                id = "trains",
                title = "Desvie ou surfe ${8L + (tier - 1L) * 5L} trens",
                current = missionTrainsPassed,
                target = 8L + (tier - 1L) * 5L,
                iconType = MissionIconType.TRAINS
            )
        } else {
            MissionProgressItem(
                id = "distance",
                title = "Corra ${350L + (tier - 1L) * 250L}m no metrô",
                current = missionTotalMeters,
                target = 350L + (tier - 1L) * 250L,
                iconType = MissionIconType.DISTANCE
            )
        }
        return listOf(
            MissionProgressItem(
                id = "magnet",
                title = "Colete $magnetTarget moedas com o Ímã",
                current = missionCoinsWithMagnet,
                target = magnetTarget,
                iconType = MissionIconType.MAGNET
            ),
            MissionProgressItem(
                id = "jetpack",
                title = "Voe com a Mochila a Jato ${jetpackTarget}x",
                current = missionJetpackFlights,
                target = jetpackTarget,
                iconType = MissionIconType.JETPACK
            ),
            thirdMission
        )
    }
}
