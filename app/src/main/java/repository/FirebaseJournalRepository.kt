package repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import model.JournalEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseJournalRepository : JournalRepository {
    // Reference to Firebase database
    private val database = FirebaseDatabase.getInstance()
    private val entriesRef = database.getReference("journal_entries")

    // Get all entries for a user
    override suspend fun getJournalEntries(userId: String): Flow<List<JournalEntry>> = callbackFlow {
        val entriesQuery = entriesRef.child(userId)

        val listener = entriesQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<JournalEntry>()
                for (childSnapshot in snapshot.children) {
                    childSnapshot.getValue(JournalEntry::class.java)?.let {
                        entries.add(it)
                    }
                }
                trySend(entries)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        })

        // Remove the listener when the Flow is cancelled
        awaitClose { entriesQuery.removeEventListener(listener) }
    }
    
    // Get entries for a specific date
    override suspend fun getJournalEntriesByDate(userId: String, date: LocalDate): List<JournalEntry> = 
        suspendCancellableCoroutine { continuation ->
            val dateString = JournalEntry.fromDate(date)
            
            // Query entries by date
            entriesRef.child(userId)
                .orderByChild("date")
                .equalTo(dateString)
                .get()
                .addOnSuccessListener { snapshot ->
                    val entries = mutableListOf<JournalEntry>()
                    for (entrySnapshot in snapshot.children) {
                        entrySnapshot.getValue(JournalEntry::class.java)?.let { entry ->
                            entries.add(entry)
                        }
                    }
                    continuation.resume(entries)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }
    
    // Check if an entry exists for a specific date
    override suspend fun hasEntryForDate(userId: String, date: LocalDate): Boolean =
        suspendCancellableCoroutine { continuation ->
            val dateString = JournalEntry.fromDate(date)
            
            entriesRef.child(userId)
                .orderByChild("date")
                .equalTo(dateString)
                .limitToFirst(1)
                .get()
                .addOnSuccessListener { snapshot ->
                    continuation.resume(snapshot.exists() && snapshot.childrenCount > 0)
                }
                .addOnFailureListener {
                    continuation.resume(false)
                }
        }
        
    // Get entry by ID
    override suspend fun getJournalEntryById(entryId: String): JournalEntry? = suspendCancellableCoroutine { continuation ->
        // We need to find which user owns this entry
        entriesRef.get().addOnSuccessListener { usersSnapshot ->
            var foundEntry: JournalEntry? = null

            // Loop through all users
            for (userSnapshot in usersSnapshot.children) {
                val userId = userSnapshot.key
                if (userId != null) {
                    // For each user, check if they have the entry we're looking for
                    entriesRef.child(userId).child(entryId).get().addOnSuccessListener { entrySnapshot ->
                        if (entrySnapshot.exists()) {
                            foundEntry = entrySnapshot.getValue(JournalEntry::class.java)
                        }

                        // Whether we found it or not, resolve the coroutine
                        continuation.resume(foundEntry)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }

                    // We've started the search, so return from the loop
                    return@addOnSuccessListener
                }
            }

            // If we get here, there were no users to search
            continuation.resume(null)
        }.addOnFailureListener {
            continuation.resume(null)
        }
    }
    
    // Add new entry
    override suspend fun addJournalEntry(entry: JournalEntry): String = suspendCancellableCoroutine { continuation ->
        // Generate a new ID if one isn't provided
        val entryId = if (entry.id.isNotEmpty()) entry.id else entriesRef.child(entry.userId).push().key ?: ""

        if (entryId.isEmpty()) {
            continuation.resumeWithException(Exception("Failed to generate entry ID"))
            return@suspendCancellableCoroutine
        }

        // Create an entry with the generated ID
        val entryWithId = entry.copy(id = entryId)

        // Save the entry to Firebase
        entriesRef.child(entry.userId).child(entryId).setValue(entryWithId)
            .addOnSuccessListener {
                continuation.resume(entryId)
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
            }
    }

    // Update existing entry
    override suspend fun updateJournalEntry(entry: JournalEntry): Boolean = suspendCancellableCoroutine { continuation ->
        // Ensure we have a valid ID and userId
        if (entry.id.isEmpty() || entry.userId.isEmpty()) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        // Update the entry in Firebase
        entriesRef.child(entry.userId).child(entry.id).setValue(entry)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    // Delete an entry
    override suspend fun deleteJournalEntry(entryId: String): Boolean = suspendCancellableCoroutine { continuation ->
        // We need to find which user this entry belongs to
        entriesRef.get().addOnSuccessListener { usersSnapshot ->
            var deleted = false

            for (userSnapshot in usersSnapshot.children) {
                val userId = userSnapshot.key
                if (userId != null) {
                    entriesRef.child(userId).child(entryId).get().addOnSuccessListener { entrySnapshot ->
                        if (entrySnapshot.exists()) {
                            // Found the entry, now delete it
                            entriesRef.child(userId).child(entryId).removeValue()
                                .addOnSuccessListener {
                                    // Also delete any associated analysis
                                    val aiAnalysisRef = database.getReference("ai_analysis")
                                    aiAnalysisRef.child(entryId).removeValue()

                                    deleted = true
                                    continuation.resume(true)
                                }
                                .addOnFailureListener {
                                    continuation.resume(false)
                                }
                            return@addOnSuccessListener
                        }
                    }
                }
            }

            // If we get here, entry wasn't found
            if (!deleted) {
                continuation.resume(false)
            }
        }.addOnFailureListener {
            continuation.resume(false)
        }
    }
}