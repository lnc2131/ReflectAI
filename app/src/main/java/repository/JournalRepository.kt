package repository

import model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    // Get all journal entries for a specific user
    suspend fun getJournalEntries(userId: String): Flow<List<JournalEntry>>

    // Get a single journal entry by its ID
    suspend fun getJournalEntryById(entryId: String): JournalEntry?

    // Add a new journal entry and return its ID
    suspend fun addJournalEntry(entry: JournalEntry): String

    // Update an existing journal entry
    suspend fun updateJournalEntry(entry: JournalEntry): Boolean

    // Delete a journal entry
    suspend fun deleteJournalEntry(entryId: String): Boolean
}