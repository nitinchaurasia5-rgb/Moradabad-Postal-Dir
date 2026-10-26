package com.example.model

/**
 * Data model representing a Postal Delivery Beat record.
 */
data class PostalBeat(
    val id: String,
    val pincode: String,
    val subOffice: String,
    val beatName: String,
    val coveredVillagesRaw: String,
    val villagesList: List<String> = emptyList(),
    val isBookmarked: Boolean = false
) {
    val totalVillagesCount: Int get() = villagesList.size

    val isBranchOffice: Boolean
        get() = beatName.contains("BO", ignoreCase = true) || beatName.contains("B.O", ignoreCase = true)

    val isNumberedBeat: Boolean
        get() = beatName.contains("Beat", ignoreCase = true) || beatName.contains("Postman", ignoreCase = true)

    val isGovtOrSpecial: Boolean
        get() = beatName.contains("Govt", ignoreCase = true) || 
                beatName.contains("Office", ignoreCase = true) ||
                coveredVillagesRaw.contains("कार्यालय", ignoreCase = true) ||
                coveredVillagesRaw.contains("Mill", ignoreCase = true)
}

/**
 * Real-time statistics computed over filtered and total beats.
 */
data class DashboardStatistics(
    val totalSubOffices: Int = 0,
    val totalBeats: Int = 0,
    val filteredMatches: Int = 0,
    val totalPincodes: Int = 0,
    val totalVillages: Int = 0,
    val topSubOffices: List<Pair<String, Int>> = emptyList(),
    val topPincodes: List<Pair<String, Int>> = emptyList(),
    val branchOfficeCount: Int = 0,
    val numberedBeatCount: Int = 0,
    val specialOfficeCount: Int = 0
)

/**
 * Sync status for Google Sheet data integration.
 */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Success(val message: String, val recordCount: Int, val source: String, val timestamp: Long) : SyncStatus
    data class Error(val errorMessage: String) : SyncStatus
}

enum class SortOption(val label: String) {
    BEAT_NAME("Beat Name (A-Z)"),
    VILLAGE_COUNT_DESC("Most Localities"),
    SUB_OFFICE("Sub-Office (A-Z)"),
    PINCODE("PIN Code")
}
