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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseJournalRepository : JournalRepository {
    // Reference to Firebase database
    private val database = FirebaseDatabase.getInstance()
    private val entriesRef = database.getReference("journal_entries")

    // Implementation will go here
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
    override suspend fun getJournalEntryById(entryId: String): JournalEntry? = suspendCancellableCoroutine { continuation ->
        // We need to find which user owns this entry
        // In a more optimized app, you'd store the userId with the entry when you create it
        // or maintain a separate index

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

}
