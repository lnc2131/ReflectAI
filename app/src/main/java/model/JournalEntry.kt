// JournalEntry.kt
package model

data class JournalEntry(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val aiAnalysis: String = "",
    val timestamp: Long = System.currentTimeMillis()
)