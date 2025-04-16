package repository

import com.google.firebase.database.FirebaseDatabase
import model.JournalEntry
import java.util.UUID

class SimpleJournalRepository {
    // Reference to Firebase database
    private val database = FirebaseDatabase.getInstance()
    private val entriesRef = database.getReference("journal_entries")

    // Save a journal entry directly
    fun saveEntry(entry: JournalEntry, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val entryRef = if (entry.id.isEmpty()) {
            // Generate a new ID if one isn't provided
            entriesRef.push()
        } else {
            entriesRef.child(entry.id)
        }

        // If we generated a new ID, use it
        val finalEntry = if (entry.id.isEmpty()) {
            entry.copy(id = entryRef.key ?: UUID.randomUUID().toString())
        } else {
            entry
        }

        // Save the entry to Firebase
        entryRef.setValue(finalEntry)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    // Get all journal entries
    fun getEntries(onSuccess: (List<JournalEntry>) -> Unit, onError: (Exception) -> Unit) {
        entriesRef.get()
            .addOnSuccessListener { snapshot ->
                val entries = mutableListOf<JournalEntry>()
                for (entrySnapshot in snapshot.children) {
                    entrySnapshot.getValue(JournalEntry::class.java)?.let {
                        entries.add(it)
                    }
                }
                onSuccess(entries)
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    // Get a specific journal entry
    fun getEntry(entryId: String, onSuccess: (JournalEntry?) -> Unit, onError: (Exception) -> Unit) {
        entriesRef.child(entryId).get()
            .addOnSuccessListener { snapshot ->
                val entry = if (snapshot.exists()) {
                    snapshot.getValue(JournalEntry::class.java)
                } else {
                    null
                }
                onSuccess(entry)
            }
            .addOnFailureListener {
                onError(it)
            }
    }
}