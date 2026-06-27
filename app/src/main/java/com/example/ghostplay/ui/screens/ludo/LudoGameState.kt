package com.example.ghostplay.ui.screens.ludo

import kotlinx.serialization.Serializable

@Serializable
enum class LudoColor {
    RED,    // Bottom-Left
    GREEN,  // Top-Left
    YELLOW, // Top-Right
    BLUE    // Bottom-Right
}

@Serializable
enum class TokenPositionType {
    BASE,
    TRACK,
    HOME_STRETCH,
    FINISHED
}

@Serializable
data class LudoCell(val col: Int, val row: Int)

@Serializable
data class LudoToken(
    val color: LudoColor,
    val id: Int, // 0..3
    val positionType: TokenPositionType = TokenPositionType.BASE,
    val positionIndex: Int = 0 // Track index or home stretch index
)

@Serializable
data class LudoPlayer(
    val id: String = "",
    val name: String = "",
    val color: LudoColor,
    val isBot: Boolean = false,
    val isHost: Boolean = false
)

@Serializable
data class LudoBoardState(
    val tokens: List<LudoToken> = initialTokens(),
    val currentPlayer: LudoColor = LudoColor.RED,
    val diceValue: Int? = null,
    val diceRolled: Boolean = false,
    val winningPlayers: List<LudoColor> = emptyList(),
    val logs: List<String> = listOf("INITIALIZING_CYBERPULSE_LUDO")
) {
    companion object {
        fun initialTokens(): List<LudoToken> {
            val list = mutableListOf<LudoToken>()
            LudoColor.entries.forEach { color ->
                for (id in 0..3) {
                    list.add(LudoToken(color, id, TokenPositionType.BASE, 0))
                }
            }
            return list
        }
    }
}

@Serializable
data class LudoLobby(
    val lobbyId: String = "",
    val status: String = "LOBBY", // LOBBY, PLAYING, FINISHED
    val hostId: String = "",
    val players: List<LudoPlayer> = emptyList(),
    val boardState: LudoBoardState = LudoBoardState(),
    val lastUpdateTime: Long = System.currentTimeMillis()
)

object LudoCoordinates {
    // 52 track coordinate cells clockwise starting from bottom-left (6, 13)
    val TRACK = listOf(
        LudoCell(6, 13), LudoCell(6, 12), LudoCell(6, 11), LudoCell(6, 10), LudoCell(6, 9), // South arm left (up)
        LudoCell(5, 8), LudoCell(4, 8), LudoCell(3, 8), LudoCell(2, 8), LudoCell(1, 8), LudoCell(0, 8), // West arm bottom (left)
        LudoCell(0, 7), // West arm tip (up)
        LudoCell(0, 6), LudoCell(1, 6), LudoCell(2, 6), LudoCell(3, 6), LudoCell(4, 6), LudoCell(5, 6), // West arm top (right)
        LudoCell(6, 5), LudoCell(6, 4), LudoCell(6, 3), LudoCell(6, 2), LudoCell(6, 1), LudoCell(6, 0), // North arm left (up)
        LudoCell(7, 0), // North arm tip (right)
        LudoCell(8, 0), LudoCell(8, 1), LudoCell(8, 2), LudoCell(8, 3), LudoCell(8, 4), LudoCell(8, 5), // North arm right (down)
        LudoCell(9, 6), LudoCell(10, 6), LudoCell(11, 6), LudoCell(12, 6), LudoCell(13, 6), LudoCell(14, 6), // East arm top (right)
        LudoCell(14, 7), // East arm tip (down)
        LudoCell(14, 8), LudoCell(13, 8), LudoCell(12, 8), LudoCell(11, 8), LudoCell(10, 8), LudoCell(9, 8), // East arm bottom (left)
        LudoCell(8, 9), LudoCell(8, 10), LudoCell(8, 11), LudoCell(8, 12), LudoCell(8, 13), LudoCell(8, 14), // South arm right (down)
        LudoCell(7, 14), // South arm tip (left)
        LudoCell(6, 14)  // South arm bottom-left corner
    )

    // Base coordinates for tokens inside starting quadrants
    val BASES = mapOf(
        LudoColor.RED to listOf(LudoCell(1, 10), LudoCell(3, 10), LudoCell(1, 12), LudoCell(3, 12)),
        LudoColor.GREEN to listOf(LudoCell(1, 1), LudoCell(3, 1), LudoCell(1, 3), LudoCell(3, 3)),
        LudoColor.YELLOW to listOf(LudoCell(10, 1), LudoCell(12, 1), LudoCell(10, 3), LudoCell(12, 3)),
        LudoColor.BLUE to listOf(LudoCell(10, 10), LudoCell(12, 10), LudoCell(10, 12), LudoCell(12, 12))
    )

    // Home stretch coordinates
    val HOME_STRETCHES = mapOf(
        LudoColor.RED to listOf(LudoCell(7, 13), LudoCell(7, 12), LudoCell(7, 11), LudoCell(7, 10), LudoCell(7, 9)),
        LudoColor.GREEN to listOf(LudoCell(1, 7), LudoCell(2, 7), LudoCell(3, 7), LudoCell(4, 7), LudoCell(5, 7)),
        LudoColor.YELLOW to listOf(LudoCell(7, 1), LudoCell(7, 2), LudoCell(7, 3), LudoCell(7, 4), LudoCell(7, 5)),
        LudoColor.BLUE to listOf(LudoCell(13, 7), LudoCell(12, 7), LudoCell(11, 7), LudoCell(10, 7), LudoCell(9, 7))
    )

    // Finished/Target coordinates in center home triangle
    val FINISHED = mapOf(
        LudoColor.RED to LudoCell(7, 8),
        LudoColor.GREEN to LudoCell(6, 7),
        LudoColor.YELLOW to LudoCell(7, 6),
        LudoColor.BLUE to LudoCell(8, 7)
    )

    // Safe zone indexes on the TRACK path
    val SAFE_ZONE_INDEXES = setOf(0, 8, 13, 20, 26, 33, 39, 46)

    // Starting indices on the TRACK path for each player
    val START_INDEXES = mapOf(
        LudoColor.RED to 0,
        LudoColor.GREEN to 13,
        LudoColor.YELLOW to 26,
        LudoColor.BLUE to 39
    )

    // Last index on the TRACK path before entering home stretch
    val EXIT_INDEXES = mapOf(
        LudoColor.RED to 51,
        LudoColor.GREEN to 11,
        LudoColor.YELLOW to 24,
        LudoColor.BLUE to 37
    )

    fun getCell(token: LudoToken): LudoCell {
        return when (token.positionType) {
            TokenPositionType.BASE -> BASES[token.color]!![token.id]
            TokenPositionType.TRACK -> TRACK[token.positionIndex]
            TokenPositionType.HOME_STRETCH -> HOME_STRETCHES[token.color]!![token.positionIndex]
            TokenPositionType.FINISHED -> FINISHED[token.color]!!
        }
    }
}
