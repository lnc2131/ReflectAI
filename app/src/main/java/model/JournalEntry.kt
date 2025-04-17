// JournalEntry.kt
package model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class JournalEntry(
    val id: String = "",
    val userId: String = "testUser", // Will be replaced with authentication user ID
    val content: String = "",
    val aiAnalysis: String = "",
    val mood: String = "neutral", // Can be "sad", "neutral", or "happy"
    val date: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDate(date: LocalDate): String {
            val formatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            println("Converting LocalDate $date to string: $formatted")
            return formatted
        }
        
        fun toDate(dateString: String): LocalDate {
            try {
                val parsed = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                println("Successfully parsed date string: $dateString to $parsed")
                return parsed
            } catch (e: Exception) {
                println("Error parsing date string: $dateString - ${e.message}")
                // Default to today's date if parsing fails
                throw e
            }
        }
    }
}