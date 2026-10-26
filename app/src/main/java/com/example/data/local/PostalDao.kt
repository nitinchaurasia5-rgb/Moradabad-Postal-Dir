package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PostalDao {

    @Query("SELECT * FROM postal_beats ORDER BY beatName ASC")
    fun getAllBeats(): Flow<List<PostalBeatEntity>>

    @Query("SELECT COUNT(*) FROM postal_beats")
    suspend fun getBeatsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(beats: List<PostalBeatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeat(beat: PostalBeatEntity)

    @Update
    suspend fun updateBeat(beat: PostalBeatEntity)

    @Query("DELETE FROM postal_beats WHERE id = :id")
    suspend fun deleteBeatById(id: String)

    @Query("DELETE FROM postal_beats")
    suspend fun clearAllBeats()

    @Query("UPDATE postal_beats SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmark(id: String, isBookmarked: Boolean)

    @Query("SELECT * FROM sync_metadata WHERE `key` = :key LIMIT 1")
    fun getSyncMetadata(key: String = SyncMetadataEntity.DEFAULT_KEY): Flow<SyncMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSyncMetadata(metadata: SyncMetadataEntity)

    /**
     * Atomically clears old data and inserts fresh sheet data along with sync metadata.
     */
    @Transaction
    suspend fun replaceAllBeats(beats: List<PostalBeatEntity>, metadata: SyncMetadataEntity) {
        clearAllBeats()
        insertAll(beats)
        setSyncMetadata(metadata)
    }
}
