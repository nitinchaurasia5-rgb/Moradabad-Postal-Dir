package com.example

import com.example.data.CsvParser
import com.example.model.PostalBeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test CsvParser with sample rows`() {
        val sampleCsv = """
            ,PINCODE,POST OFFICE / SO,BRANCH OFFICE / BEAT,COVERED VILLAGES & LOCALITIES
            ,244303,Sarai Tareen,Saraitarin,"Mangalpura, Nawab Khel, Shahjipura"
            ,244225,Bachhraon,Beat no. 1,"Peerzadgan, Hafiz Road, Chaudhriyan"
        """.trimIndent()

        val beats = CsvParser.parseCsv(sampleCsv)
        assertEquals(2, beats.size)

        val beat1 = beats[0]
        assertEquals("244303", beat1.pincode)
        assertEquals("Sarai Tareen", beat1.subOffice)
        assertEquals("Saraitarin", beat1.beatName)
        assertEquals(3, beat1.villagesList.size)
        assertTrue(beat1.villagesList.contains("Mangalpura"))

        val beat2 = beats[1]
        assertEquals("244225", beat2.pincode)
        assertTrue(beat2.isNumberedBeat)
    }

    @Test
    fun `test CsvParser handles multiline quoted fields`() {
        val multilineCsv = """
            ,PINCODE,POST OFFICE / SO,BRANCH OFFICE / BEAT,COVERED VILLAGES & LOCALITIES
            ,244701,Milak ,MILAK SO (Beat 2 & 3),"RAURA KALAN, VASHEERNAGAR
            NASIRABAD ( NEAR KALAWATI BAL VIDYA MANDIR)"
        """.trimIndent()

        val beats = CsvParser.parseCsv(multilineCsv)
        assertEquals(1, beats.size)
        assertTrue(beats[0].villagesList.size >= 2)
    }
}
