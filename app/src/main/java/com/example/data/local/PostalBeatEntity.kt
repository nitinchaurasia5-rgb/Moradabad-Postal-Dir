package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.PostalBeat

@Entity(tableName = "postal_beats")
data class PostalBeatEntity(
    @PrimaryKey val id: String,
    val pincode: String,
    val subOffice: String,
    val beatName: String,
    val coveredVillagesRaw: String,
    val isBookmarked: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): PostalBeat {
        val villagesList = if (coveredVillagesRaw.isBlank()) {
            emptyList()
        } else {
            coveredVillagesRaw.split(Regex("[,;/\\n\\r]+"))
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() && it != "-" }
        }

        return PostalBeat(
            id = id,
            pincode = pincode,
            subOffice = subOffice,
            beatName = beatName,
            coveredVillagesRaw = coveredVillagesRaw,
            villagesList = villagesList,
            isBookmarked = isBookmarked
        )
    }

    companion object {
        fun fromDomainModel(beat: PostalBeat): PostalBeatEntity {
            return PostalBeatEntity(
                id = beat.id,
                pincode = beat.pincode,
                subOffice = beat.subOffice,
                beatName = beat.beatName,
                coveredVillagesRaw = if (beat.coveredVillagesRaw.isNotBlank()) {
                    beat.coveredVillagesRaw
                } else {
                    beat.villagesList.joinToString(", ")
                },
                isBookmarked = beat.isBookmarked,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
}
