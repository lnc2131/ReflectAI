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
        // Ensure userId is always set
        val entryWithUserId = if (entry.userId.isEmpty()) {
            entry.copy(userId = "testUser")  // Use default user ID if none provided
        } else {
            entry
        }
        
        // Generate a reference under the user's ID
        val entryRef = if (entryWithUserId.id.isEmpty()) {
            // Generate a new ID if one isn't provided
            entriesRef.child(entryWithUserId.userId).push()
        } else {
            entriesRef.child(entryWithUserId.userId).child(entryWithUserId.id)
        }

        // If we generated a new ID, use it
        val finalEntry = if (entryWithUserId.id.isEmpty()) {
            entryWithUserId.copy(id = entryRef.key ?: UUID.randomUUID().toString())
        } else {
            entryWithUserId
        }

        // Log the save details for debugging
        println("Saving entry: ID=${finalEntry.id}, Date=${finalEntry.date}, User=${finalEntry.userId}")
        
        // Save the entry to Firebase
        entryRef.setValue(finalEntry)
            .addOnSuccessListener {
                println("Entry saved successfully: ID=${finalEntry.id}, Date=${finalEntry.date}, Path=${entryRef.path}")
                
                // Immediately verify the entry was saved correctly
                entryRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val savedEntry = snapshot.getValue(JournalEntry::class.java)
                        println("Verification: Entry exists in database with ID=${savedEntry?.id}, Date=${savedEntry?.date}")
                    } else {
                        println("WARNING: Entry verification failed - entry not found at path ${entryRef.path}")
                    }
                }
                
                // Check if we can query by date
                entriesRef.child(finalEntry.userId)
                    .orderByChild("date")
                    .equalTo(finalEntry.date)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        println("Date query verification: Found ${snapshot.childrenCount} entries with date ${finalEntry.date}")
                    }
                
                onSuccess()
            }
            .addOnFailureListener { error ->
                println("Failed to save entry: ${error.message}")
                onError(error)
            }
    }

    // Get all journal entries for a user
    fun getEntries(userId: String = "testUser", onSuccess: (List<JournalEntry>) -> Unit, onError: (Exception) -> Unit) {
        entriesRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val entries = mutableListOf<JournalEntry>()
                for (entrySnapshot in snapshot.children) {
                    entrySnapshot.getValue(JournalEntry::class.java)?.let {
                        entries.add(it)
                    }
                }
                println("Got ${entries.size} entries for user $userId")
                onSuccess(entries)
            }
            .addOnFailureListener {
                println("Failed to get entries: ${it.message}")
                onError(it)
            }
    }

    // Get a specific journal entry
    fun getEntry(entryId: String, userId: String = "testUser", onSuccess: (JournalEntry?) -> Unit, onError: (Exception) -> Unit) {
        entriesRef.child(userId).child(entryId).get()
            .addOnSuccessListener { snapshot ->
                val entry = if (snapshot.exists()) {
                    snapshot.getValue(JournalEntry::class.java)
                } else {
                    null
                }
                println("Got entry with ID $entryId: ${entry != null}")
                onSuccess(entry)
            }
            .addOnFailureListener {
                println("Failed to get entry $entryId: ${it.message}")
                onError(it)
            }
    }
    
    // Get entries for a specific date
    fun getEntriesByDate(dateString: String, userId: String = "testUser", onSuccess: (List<JournalEntry>) -> Unit, onError: (Exception) -> Unit) {
        println("Querying Firebase for date: $dateString and user: $userId")
        
        // Debug: List all entries to see what's in the database
        listAllEntries(userId)
        
        entriesRef.child(userId).orderByChild("date").equalTo(dateString).get()
            .addOnSuccessListener { snapshot ->
                val entries = mutableListOf<JournalEntry>()
                println("Query snapshot exists: ${snapshot.exists()}, child count: ${snapshot.childrenCount}")
                
                for (entrySnapshot in snapshot.children) {
                    entrySnapshot.getValue(JournalEntry::class.java)?.let { entry ->
                        println("Found entry: ID=${entry.id}, date=${entry.date}, title=${entry.title}")
                        entries.add(entry)
                    }
                }
                
                println("Found ${entries.size} entries for date $dateString")
                onSuccess(entries)
            }
            .addOnFailureListener { error ->
                println("Failed to get entries for date $dateString: ${error.message}")
                onError(error)
            }
    }
    
    // Debug function to list all entries in the database
    private fun listAllEntries(userId: String) {
        entriesRef.child(userId).get().addOnSuccessListener { snapshot ->
            println("--- Listing all entries for user $userId ---")
            if (!snapshot.exists()) {
                println("No entries found for user $userId")
                return@addOnSuccessListener
            }
            
            println("Found ${snapshot.childrenCount} total entries")
            for (entrySnapshot in snapshot.children) {
                val entry = entrySnapshot.getValue(JournalEntry::class.java)
                println("Entry: ID=${entry?.id}, date=${entry?.date}, title=${entry?.title}")
            }
            println("--- End of entries list ---")
        }
    }
    
    // Check if entries exist for a specific date
    fun hasEntryForDate(dateString: String, userId: String = "testUser", onSuccess: (Boolean) -> Unit, onError: (Exception) -> Unit) {
        println("Checking for entries on date $dateString for user $userId")
        
        entriesRef.child(userId).orderByChild("date").equalTo(dateString).limitToFirst(1).get()
            .addOnSuccessListener { snapshot ->
                val hasEntries = snapshot.exists() && snapshot.childrenCount > 0
                println("Has entries for date $dateString: $hasEntries (childCount=${snapshot.childrenCount})")
                
                if (hasEntries) {
                    // Debug: Print the entry that was found
                    val entry = snapshot.children.iterator().next().getValue(JournalEntry::class.java)
                    println("Found entry: ID=${entry?.id}, title=${entry?.title}")
                }
                
                onSuccess(hasEntries)
            }
            .addOnFailureListener { error ->
                println("Failed to check entries for date $dateString: ${error.message}")
                onError(error)
            }
    }
}