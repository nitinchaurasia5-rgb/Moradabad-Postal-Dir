package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PostalBeatEntity
import com.example.data.local.PostalDao
import com.example.data.local.SyncMetadataEntity
import com.example.model.PostalBeat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class PostalRepository(
    private val context: Context,
    private val postalDao: PostalDao = AppDatabase.getInstance(context).postalDao()
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("postal_prefs", Context.MODE_PRIVATE)

    // Flow of domain PostalBeats streamed directly from Room DB
    val beatsState: Flow<List<PostalBeat>> = postalDao.getAllBeats().map { entities ->
        entities.map { it.toDomainModel() }
    }

    // Flow of Sync metadata streamed directly from Room DB
    val syncMetadata: Flow<SyncMetadataEntity?> = postalDao.getSyncMetadata()

    private val _activeSheetUrl = MutableStateFlow<String>(
        prefs.getString(KEY_SHEET_URL, DEFAULT_SAMPLE_SHEET_URL) ?: DEFAULT_SAMPLE_SHEET_URL
    )
    val activeSheetUrl: StateFlow<String> = _activeSheetUrl.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        val existingCount = postalDao.getBeatsCount()
        if (existingCount == 0) {
            // Seed database from bundled master CSV
            val bundledBeats = loadFromAssets()
            val entities = bundledBeats.map { PostalBeatEntity.fromDomainModel(it) }
            val metadata = SyncMetadataEntity(
                lastSyncTimestamp = System.currentTimeMillis(),
                sourceName = "Bundled Master Data (Cached in Room)",
                recordCount = entities.size,
                sheetUrl = _activeSheetUrl.value,
                isOfflineAvailable = true
            )
            postalDao.replaceAllBeats(entities, metadata)
        }
    }

    private fun loadFromAssets(): List<PostalBeat> {
        val assetManager = context.assets
        val content = assetManager.open("moradabad_beats.csv").bufferedReader().use { it.readText() }
        return CsvParser.parseCsv(content)
    }

    /**
     * Serializes beats back to CSV format compatible with Google Sheets & the parser.
     */
    fun generateCsv(beats: List<PostalBeat>): String {
        val sb = StringBuilder()
        sb.append(",PINCODE,POST OFFICE / SO,BRANCH OFFICE / BEAT,COVERED VILLAGES & LOCALITIES\n")
        for (beat in beats) {
            val escapedVillages = beat.villagesList.joinToString(", ").replace("\"", "\"\"")
            val escapedSO = beat.subOffice.replace("\"", "\"\"")
            val escapedBeat = beat.beatName.replace("\"", "\"\"")
            sb.append(",\"${beat.pincode}\",\"$escapedSO\",\"$escapedBeat\",\"$escapedVillages\"\n")
        }
        return sb.toString()
    }

    /**
     * Updates an existing beat or inserts it into Room.
     */
    suspend fun saveBeat(beat: PostalBeat) = withContext(Dispatchers.IO) {
        val entity = PostalBeatEntity.fromDomainModel(beat)
        postalDao.insertBeat(entity)
    }

    /**
     * Adds a brand new beat record into Room.
     */
    suspend fun addBeat(
        pincode: String,
        subOffice: String,
        beatName: String,
        villages: List<String>
    ): PostalBeat = withContext(Dispatchers.IO) {
        val newBeat = PostalBeat(
            id = "beat_custom_" + UUID.randomUUID().toString().take(8),
            pincode = pincode.trim(),
            subOffice = subOffice.trim(),
            beatName = beatName.trim(),
            coveredVillagesRaw = villages.joinToString(", "),
            villagesList = villages.map { it.trim() }.filter { it.isNotBlank() },
            isBookmarked = false
        )
        postalDao.insertBeat(PostalBeatEntity.fromDomainModel(newBeat))
        newBeat
    }

    /**
     * Deletes a beat from Room by ID.
     */
    suspend fun deleteBeat(beatId: String) = withContext(Dispatchers.IO) {
        postalDao.deleteBeatById(beatId)
    }

    /**
     * Appends a new village to a beat in Room.
     */
    suspend fun addVillageToBeat(beatId: String, newVillage: String, currentBeat: PostalBeat) = withContext(Dispatchers.IO) {
        val updatedVillages = (currentBeat.villagesList + newVillage.trim()).distinct()
        val updatedBeat = currentBeat.copy(
            villagesList = updatedVillages,
            coveredVillagesRaw = updatedVillages.joinToString(", ")
        )
        postalDao.insertBeat(PostalBeatEntity.fromDomainModel(updatedBeat))
    }

    /**
     * Removes a village from a beat in Room.
     */
    suspend fun removeVillageFromBeat(beatId: String, villageToRemove: String, currentBeat: PostalBeat) = withContext(Dispatchers.IO) {
        val updatedVillages = currentBeat.villagesList.filter { it != villageToRemove }
        val updatedBeat = currentBeat.copy(
            villagesList = updatedVillages,
            coveredVillagesRaw = updatedVillages.joinToString(", ")
        )
        postalDao.insertBeat(PostalBeatEntity.fromDomainModel(updatedBeat))
    }

    /**
     * Toggles bookmark in Room.
     */
    suspend fun toggleBookmark(beatId: String, currentStatus: Boolean) = withContext(Dispatchers.IO) {
        postalDao.updateBookmark(beatId, !currentStatus)
    }

    /**
     * Imports CSV directly and caches into Room.
     */
    suspend fun replaceWithCsv(csvContent: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parsed = CsvParser.parseCsv(csvContent)
            if (parsed.isEmpty()) {
                return@withContext Result.failure(Exception("No valid records found in CSV data."))
            }
            val entities = parsed.map { PostalBeatEntity.fromDomainModel(it) }
            val metadata = SyncMetadataEntity(
                lastSyncTimestamp = System.currentTimeMillis(),
                sourceName = "Direct CSV Import (Room Database)",
                recordCount = entities.size,
                sheetUrl = _activeSheetUrl.value,
                isOfflineAvailable = true
            )
            postalDao.replaceAllBeats(entities, metadata)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync with Google Sheets online via published CSV URL and cache into Room.
     */
    suspend fun syncFromGoogleSheet(url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val normalized = normalizeGoogleSheetUrl(url)
            val request = Request.Builder()
                .url(normalized)
                .header("User-Agent", "PostalBeatsDashboard/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Google Sheets returned HTTP ${response.code}: ${response.message}"))
            }

            val body = response.body?.string() ?: ""
            if (body.isBlank()) {
                return@withContext Result.failure(Exception("Received empty response from Google Sheet"))
            }

            val parsedBeats = CsvParser.parseCsv(body)
            if (parsedBeats.isEmpty()) {
                return@withContext Result.failure(Exception("Could not parse rows. Check that sheet has PINCODE, Sub-Office, Beat, and Villages columns."))
            }

            val entities = parsedBeats.map { PostalBeatEntity.fromDomainModel(it) }
            val metadata = SyncMetadataEntity(
                lastSyncTimestamp = System.currentTimeMillis(),
                sourceName = "Google Sheets Live Sync",
                recordCount = entities.size,
                sheetUrl = url,
                isOfflineAvailable = true
            )

            // Atomically update Room database
            postalDao.replaceAllBeats(entities, metadata)

            _activeSheetUrl.value = url
            prefs.edit().putString(KEY_SHEET_URL, url).apply()

            Result.success(entities.size)
        } catch (e: IOException) {
            // Network failure / Offline
            Result.failure(Exception("Network offline or unreachable. Dashboard is continuing to serve all cached records from local Room database."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun normalizeGoogleSheetUrl(inputUrl: String): String {
        val trimmed = inputUrl.trim()
        if (trimmed.contains("pub?output=csv") || trimmed.contains("format=csv")) {
            return trimmed
        }
        val sheetIdRegex = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)")
        val match = sheetIdRegex.find(trimmed)
        if (match != null) {
            val sheetId = match.groupValues[1]
            val gidRegex = Regex("[#&?]gid=([0-9]+)")
            val gidMatch = gidRegex.find(trimmed)
            val gid = gidMatch?.groupValues?.get(1) ?: "0"
            return "https://docs.google.com/spreadsheets/d/$sheetId/export?format=csv&gid=$gid"
        }
        return trimmed
    }

    /**
     * Resets Room database to default bundled master dataset.
     */
    suspend fun resetToDefault() = withContext(Dispatchers.IO) {
        val defaultBeats = loadFromAssets()
        val entities = defaultBeats.map { PostalBeatEntity.fromDomainModel(it) }
        val metadata = SyncMetadataEntity(
            lastSyncTimestamp = System.currentTimeMillis(),
            sourceName = "Bundled Master Data (Cached in Room)",
            recordCount = entities.size,
            sheetUrl = DEFAULT_SAMPLE_SHEET_URL,
            isOfflineAvailable = true
        )
        postalDao.replaceAllBeats(entities, metadata)
    }

    companion object {
        private const val KEY_SHEET_URL = "key_sheet_url"

        const val DEFAULT_SAMPLE_SHEET_URL =
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vSampleMoradabadPostalDirectory/pub?output=csv"
    }
}
