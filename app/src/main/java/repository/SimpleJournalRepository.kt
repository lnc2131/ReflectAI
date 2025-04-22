package repository

import com.google.firebase.database.FirebaseDatabase
import model.JournalEntry
import java.util.UUID

class SimpleJournalRepository {
    // Reference to Firebase database
    private val database = FirebaseDatabase.getInstance()
    private val entriesRef = database.getReference("journal_entries")

    // Reference to users collection for mood counting
    private val usersRef = database.getReference("users")
    
    // Save a journal entry directly
    fun saveEntry(entry: JournalEntry, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        val userRepo = UserRepository()
        val currentUserId = userRepo.getCurrentUserId()
        
        if (currentUserId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to save journal entries"))
            return
        }
        
        // Always use the current user's ID
        val entryWithUserId = entry.copy(userId = currentUserId)
        
        // Generate a reference under the user's ID
        val entryRef = if (entryWithUserId.id.isEmpty()) {
            // Generate a new ID if one isn't provided
            entriesRef.child(currentUserId).push()
        } else {
            entriesRef.child(currentUserId).child(entryWithUserId.id)
        }

        // If we generated a new ID, use it
        val finalEntry = if (entryWithUserId.id.isEmpty()) {
            entryWithUserId.copy(id = entryRef.key ?: UUID.randomUUID().toString())
        } else {
            entryWithUserId
        }

        // Log the save details for debugging
        println("Saving entry: ID=${finalEntry.id}, Date=${finalEntry.date}, User=${finalEntry.userId}, Mood=${finalEntry.mood}")
        
        // Save the entry to Firebase
        entryRef.setValue(finalEntry)
            .addOnSuccessListener {
                println("Entry saved successfully: ID=${finalEntry.id}, Date=${finalEntry.date}, Path=${entryRef.path}")
                
                // After successfully saving the entry, update the mood count
                updateMoodCount(finalEntry.userId, finalEntry.mood)
                
                // Immediately verify the entry was saved correctly
                entryRef.get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        val savedEntry = snapshot.getValue(JournalEntry::class.java)
                        println("Verification: Entry exists in database with ID=${savedEntry?.id}, Date=${savedEntry?.date}, Mood=${savedEntry?.mood}")
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
    
    // Update the mood count for a user
    private fun updateMoodCount(userId: String, mood: String) {
        // Validate mood value
        if (mood !in listOf("happy", "neutral", "sad")) {
            println("Invalid mood value: $mood - must be happy, neutral, or sad")
            return
        }
        
        println("Updating mood count for user $userId, mood: $mood")
        
        // First get all entries to calculate accurate mood counts
        recalculateMoodCounts(userId)
    }
    
    // Recalculate all mood counts based on actual entries
    private fun recalculateMoodCounts(userId: String) {
        // Get all entries for this user
        entriesRef.child(userId).get().addOnSuccessListener { entriesSnapshot ->
            println("Recalculating mood counts for user $userId")
            
            // Initialize counters for each mood
            val moodCounts = mutableMapOf(
                "happy" to 0,
                "neutral" to 0,
                "sad" to 0
            )
            
            // Map to track which dates we've already processed
            val processedDates = mutableSetOf<String>()
            
            // Process all entries
            entriesSnapshot.children.forEach { entrySnapshot ->
                val entry = entrySnapshot.getValue(JournalEntry::class.java)
                entry?.let {
                    // Only count each date once
                    if (!processedDates.contains(it.date)) {
                        // Add the date to processed set
                        processedDates.add(it.date)
                        
                        // Increment the appropriate mood counter
                        if (it.mood in moodCounts.keys) {
                            moodCounts[it.mood] = moodCounts[it.mood]!! + 1
                            println("Counting entry date=${it.date}, mood=${it.mood}")
                        }
                    } else {
                        println("Skipping duplicate date entry: ${it.date}")
                    }
                }
            }
            
            println("Final mood counts after recalculation: $moodCounts")
            
            // Make sure the user node exists
            val userRef = usersRef.child(userId)
            userRef.get().addOnSuccessListener { userSnapshot ->
                if (!userSnapshot.exists()) {
                    // Create a basic user entry first
                    val user = mapOf(
                        "id" to userId,
                        "displayName" to "User"
                    )
                    userRef.updateChildren(user)
                }
                
                // Update all mood counts at once
                userRef.child("moodCounts").setValue(moodCounts)
                    .addOnSuccessListener {
                        println("Successfully updated all mood counts: $moodCounts")
                    }
                    .addOnFailureListener { error ->
                        println("Failed to update mood counts: ${error.message}")
                    }
            }
        }.addOnFailureListener { error ->
            println("Failed to get entries for mood count calculation: ${error.message}")
        }
    }

    // Get all journal entries for a user
    fun getEntries(onSuccess: (List<JournalEntry>) -> Unit, onError: (Exception) -> Unit) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to get entries"))
            return
        }
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
    fun getEntry(entryId: String, onSuccess: (JournalEntry?) -> Unit, onError: (Exception) -> Unit) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to get entries"))
            return
        }
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
    fun getEntriesByDate(dateString: String, onSuccess: (List<JournalEntry>) -> Unit, onError: (Exception) -> Unit) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to get entries"))
            return
        }
        println("Querying Firebase for date: $dateString and user: $userId")
        
        // Debug: List all entries to see what's in the database
        listAllEntries(userId)
        
        entriesRef.child(userId).orderByChild("date").equalTo(dateString).get()
            .addOnSuccessListener { snapshot ->
                val entries = mutableListOf<JournalEntry>()
                println("Query snapshot exists: ${snapshot.exists()}, child count: ${snapshot.childrenCount}")
                
                for (entrySnapshot in snapshot.children) {
                    entrySnapshot.getValue(JournalEntry::class.java)?.let { entry ->
                        println("Found entry: ID=${entry.id}, date=${entry.date}, mood=${entry.mood}")
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
                println("Entry: ID=${entry?.id}, date=${entry?.date}, mood=${entry?.mood}")
            }
            println("--- End of entries list ---")
        }
    }
    
    // Check if entries exist for a specific date
    fun hasEntryForDate(dateString: String, onSuccess: (Boolean) -> Unit, onError: (Exception) -> Unit) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to check entries"))
            return
        }
        println("Checking for entries on date $dateString for user $userId")
        
        entriesRef.child(userId).orderByChild("date").equalTo(dateString).limitToFirst(1).get()
            .addOnSuccessListener { snapshot ->
                val hasEntries = snapshot.exists() && snapshot.childrenCount > 0
                println("Has entries for date $dateString: $hasEntries (childCount=${snapshot.childrenCount})")
                
                if (hasEntries) {
                    // Debug: Print the entry that was found
                    val entry = snapshot.children.iterator().next().getValue(JournalEntry::class.java)
                    println("Found entry: ID=${entry?.id}, mood=${entry?.mood}")
                }
                
                onSuccess(hasEntries)
            }
            .addOnFailureListener { error ->
                println("Failed to check entries for date $dateString: ${error.message}")
                onError(error)
            }
    }
    
    // Get entry dates for a range of dates (for the sidebar)
    fun getEntryDatesForRange(
        startDate: String, 
        endDate: String, 
        onSuccess: (Map<String, Boolean>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            onError(IllegalStateException("User must be logged in to get entry dates"))
            return
        }
        println("Getting entry dates from $startDate to $endDate for user $userId")
        
        // Query all entries for this user
        entriesRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val result = mutableMapOf<String, Boolean>()
                
                // Process all entries and filter by date
                for (entrySnapshot in snapshot.children) {
                    val entry = entrySnapshot.getValue(JournalEntry::class.java)
                    entry?.let {
                        // If the entry date is in our range, add it to the result
                        if (it.date >= startDate && it.date <= endDate) {
                            result[it.date] = true
                        }
                    }
                }
                
                println("Found entries on ${result.size} different dates in the range")
                onSuccess(result)
            }
            .addOnFailureListener { error ->
                println("Failed to get entry dates for range: ${error.message}")
                onError(error)
            }
    }
}