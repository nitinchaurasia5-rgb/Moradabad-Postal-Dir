package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey val key: String = DEFAULT_KEY,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val sourceName: String = "Bundled Master Data",
    val recordCount: Int = 455,
    val sheetUrl: String = "",
    val isOfflineAvailable: Boolean = true
) {
    val formattedLastSync: String
        get() {
            if (lastSyncTimestamp <= 0) return "Never synced"
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            return sdf.format(Date(lastSyncTimestamp))
        }

    companion object {
        const val DEFAULT_KEY = "last_sync_metadata"
    }
}
