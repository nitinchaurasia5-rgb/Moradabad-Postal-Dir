package com.example.data

import com.example.model.PostalBeat
import java.io.BufferedReader
import java.io.StringReader

object CsvParser {

    /**
     * Parse standard RFC-4180 CSV text (with quoted multiline fields) into a list of PostalBeat.
     */
    fun parseCsv(csvText: String): List<PostalBeat> {
        val rows = parseCsvRows(csvText)
        val beats = mutableListOf<PostalBeat>()

        for ((index, row) in rows.withIndex()) {
            if (row.size < 4) continue

            // The sheet format has either:
            // Col 0: empty, Col 1: Pincode, Col 2: Sub-Office, Col 3: Beat, Col 4: Covered Localities
            // OR if Col 0 is Pincode:
            val (pincode, subOffice, beatName, coveredVillages) = when {
                row.size >= 5 && row[1].trim().matches(Regex("^\\d{6}$")) -> {
                    Quadruple(row[1].trim(), row[2].trim(), row[3].trim(), row[4].trim())
                }
                row[0].trim().matches(Regex("^\\d{6}$")) -> {
                    Quadruple(
                        row[0].trim(),
                        row.getOrElse(1) { "" }.trim(),
                        row.getOrElse(2) { "" }.trim(),
                        row.getOrElse(3) { "" }.trim()
                    )
                }
                else -> null
            } ?: continue

            if (pincode.isEmpty() || beatName.isEmpty()) continue

            // Split covered villages into individual localities list
            val villagesList = parseVillages(coveredVillages)

            beats.add(
                PostalBeat(
                    id = "beat_${pincode}_${subOffice}_${beatName}_$index".replace(Regex("[^a-zA-Z0-9_]"), "_"),
                    pincode = pincode,
                    subOffice = subOffice,
                    beatName = beatName,
                    coveredVillagesRaw = coveredVillages,
                    villagesList = villagesList
                )
            )
        }

        return beats
    }

    private data class Quadruple(val first: String, val second: String, val third: String, val fourth: String)

    /**
     * Splits comma-separated villages while also cleaning up formatting.
     */
    private fun parseVillages(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.split(Regex("[,;/\\n\\r]+"))
            .map { it.trim().trim('"') }
            .filter { it.isNotBlank() && it != "-" }
    }

    /**
     * Low-level state machine parser for RFC-4180 CSV.
     */
    fun parseCsvRows(csvText: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val reader = BufferedReader(StringReader(csvText))
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var chInt: Int

        while (reader.read().also { chInt = it } != -1) {
            val c = chInt.toChar()

            if (c == '"') {
                if (inQuotes) {
                    reader.mark(1)
                    val nextInt = reader.read()
                    if (nextInt != -1 && nextInt.toChar() == '"') {
                        // Escaped quote
                        currentField.append('"')
                    } else {
                        reader.reset()
                        inQuotes = false
                    }
                } else {
                    inQuotes = true
                }
            } else if (c == ',' && !inQuotes) {
                currentRow.add(currentField.toString())
                currentField.setLength(0)
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r') {
                    reader.mark(1)
                    val nextInt = reader.read()
                    if (nextInt != -1 && nextInt.toChar() != '\n') {
                        reader.reset()
                    }
                }
                currentRow.add(currentField.toString())
                currentField.setLength(0)
                if (currentRow.any { it.isNotBlank() }) {
                    result.add(ArrayList(currentRow))
                }
                currentRow.clear()
            } else {
                currentField.append(c)
            }
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                result.add(currentRow)
            }
        }

        return result
    }
}
