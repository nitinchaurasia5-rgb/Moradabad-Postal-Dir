package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CsvParser
import com.example.data.local.AppDatabase
import com.example.data.local.PostalBeatEntity
import com.example.data.local.SyncMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Postal Beats", appName)
    }

    @Test
    fun `verify bundled asset parsing from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val content = context.assets.open("moradabad_beats.csv").bufferedReader().use { it.readText() }
        val beats = CsvParser.parseCsv(content)
        assertTrue("Beats count should be around 455", beats.size >= 400)
        val first = beats.first()
        assertEquals("244303", first.pincode)
        assertEquals("Sarai Tareen", first.subOffice)
        assertTrue(first.villagesList.isNotEmpty())
    }

    @Test
    fun `verify Room database caching and offline retrieval`() = runBlocking {
        val dao = db.postalDao()

        val sampleBeat = PostalBeatEntity(
            id = "test_1",
            pincode = "244303",
            subOffice = "Sarai Tareen",
            beatName = "Saraitarin",
            coveredVillagesRaw = "Mangalpura, Nawab Khel, Shahjipura",
            isBookmarked = true
        )

        val metadata = SyncMetadataEntity(
            lastSyncTimestamp = 1700000000000L,
            sourceName = "Google Sheets Live",
            recordCount = 1,
            sheetUrl = "https://docs.google.com/spreadsheets/d/test",
            isOfflineAvailable = true
        )

        dao.replaceAllBeats(listOf(sampleBeat), metadata)

        val cachedBeats = dao.getAllBeats().first()
        assertEquals(1, cachedBeats.size)
        assertEquals("244303", cachedBeats[0].pincode)
        assertEquals(3, cachedBeats[0].toDomainModel().villagesList.size)
        assertTrue(cachedBeats[0].isBookmarked)

        val cachedMeta = dao.getSyncMetadata().first()
        assertNotNull(cachedMeta)
        assertEquals("Google Sheets Live", cachedMeta?.sourceName)
        assertTrue(cachedMeta?.isOfflineAvailable == true)
    }
}
