package repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import model.JournalEntry
import model.MoodCounts
import model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseJournalRepository : JournalRepository {
    // Reference to Firebase database
    private val database = FirebaseDatabase.getInstance()
    private val entriesRef = database.getReference("journal_entries")
    private val usersRef = database.getReference("users")
    
    // Get all entries for a user
    override suspend fun getJournalEntries(userId: String): Flow<List<JournalEntry>> {
        val entriesFlow = MutableStateFlow<List<JournalEntry>>(emptyList())
        
        // Setup real-time listener
        entriesRef.child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val entries = mutableListOf<JournalEntry>()
                for (entrySnapshot in snapshot.children) {
                    entrySnapshot.getValue(JournalEntry::class.java)?.let {
                        entries.add(it)
                    }
                }
                entriesFlow.value = entries
            }
            
            override fun onCancelled(error: DatabaseError) {
                // Handle error
                println("Error loading entries: ${error.message}")
            }
        })
        
        return entriesFlow
    }
    
    // Get entries for a specific date
    override suspend fun getJournalEntriesByDate(userId: String, date: LocalDate): List<JournalEntry> {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId)
                .orderByChild("date")
                .equalTo(dateString)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val entries = mutableListOf<JournalEntry>()
                        for (entrySnapshot in snapshot.children) {
                            entrySnapshot.getValue(JournalEntry::class.java)?.let {
                                entries.add(it)
                            }
                        }
                        continuation.resume(entries)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
        }
    }
    
    // Check if entry exists for a date
    override suspend fun hasEntryForDate(userId: String, date: LocalDate): Boolean {
        val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId)
                .orderByChild("date")
                .equalTo(dateString)
                .limitToFirst(1)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val hasEntry = snapshot.exists() && snapshot.childrenCount > 0
                        continuation.resume(hasEntry)
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        continuation.resumeWithException(error.toException())
                    }
                })
        }
    }
    
    // Get entry by ID
    override suspend fun getJournalEntryById(entryId: String): JournalEntry? {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            return null
        }
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).get()
                .addOnSuccessListener { snapshot ->
                    val entry = snapshot.getValue(JournalEntry::class.java)
                    continuation.resume(entry)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
    
    // Add a new entry
    override suspend fun addJournalEntry(entry: JournalEntry): String {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            throw IllegalStateException("User must be logged in to add journal entries")
        }
        
        val entryId = if (entry.id.isNotEmpty()) {
            entry.id
        } else {
            entriesRef.child(userId).push().key ?: ""
        }
        
        val finalEntry = entry.copy(id = entryId, userId = userId)
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).setValue(finalEntry)
                .addOnSuccessListener {
                                    // After successful save, recalculate mood counts
                    recalculateMoodCounts(userId, onSuccess = {
                        continuation.resume(entryId)
                    }, onError = { error ->
                        // Still resume with ID even if mood count fails
                        println("Warning: Could not update mood count: ${error.message}")
                        continuation.resume(entryId)
                    })
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
    
    // Update an existing entry
    override suspend fun updateJournalEntry(entry: JournalEntry): Boolean {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            throw IllegalStateException("User must be logged in to update journal entries")
        }
        
        val entryId = entry.id
        
        if (entryId.isEmpty()) {
            throw IllegalArgumentException("Entry ID must not be empty for updates")
        }
        
        // Ensure the entry has the correct user ID
        val updatedEntry = entry.copy(userId = userId)
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).setValue(updatedEntry)
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
    
    // Delete an entry
    override suspend fun deleteJournalEntry(entryId: String): Boolean {
        val userRepo = UserRepository()
        val userId = userRepo.getCurrentUserId()
        
        if (userId.isEmpty()) {
            throw IllegalStateException("User must be logged in to delete journal entries")
        }
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).removeValue()
                .addOnSuccessListener {
                    continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
    
    // Get or create user
    fun getOrCreateUser(userId: String = "testUser", onSuccess: (User) -> Unit, onError: (Exception) -> Unit) {
        usersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = if (snapshot.exists()) {
                    snapshot.getValue(User::class.java) ?: User(id = userId)
                } else {
                    // Create a new user if none exists
                    val newUser = User(id = userId)
                    usersRef.child(userId).setValue(newUser)
                    newUser
                }
                onSuccess(user)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
    
    // Recalculate all mood counts based on actual entries
    fun recalculateMoodCounts(userId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
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
            
            // Convert to MoodCounts object
            val updatedMoodCounts = MoodCounts(
                happy = moodCounts["happy"] ?: 0,
                neutral = moodCounts["neutral"] ?: 0,
                sad = moodCounts["sad"] ?: 0
            )
            
            // Make sure the user exists
            getOrCreateUser(
                userId = userId,
                onSuccess = { user ->
                    // Save back to Firebase
                    usersRef.child(userId).child("moodCounts").setValue(updatedMoodCounts)
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { error ->
                            onError(error)
                        }
                },
                onError = onError
            )
        }.addOnFailureListener { error ->
            println("Failed to get entries for mood count calculation: ${error.message}")
            onError(error)
        }
    }
    
    // Get mood counts for a user
    fun getMoodCounts(userId: String = "testUser", onSuccess: (MoodCounts) -> Unit, onError: (Exception) -> Unit) {
        usersRef.child(userId).child("moodCounts").get()
            .addOnSuccessListener { snapshot ->
                val moodCounts = snapshot.getValue(MoodCounts::class.java) ?: MoodCounts()
                onSuccess(moodCounts)
            }
            .addOnFailureListener { error ->
                onError(error)
            }
    }
}