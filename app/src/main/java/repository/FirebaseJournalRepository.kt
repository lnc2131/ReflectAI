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
        val userId = "testUser" // Default user ID
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
        val userId = entry.userId.ifEmpty { "testUser" }
        val entryId = if (entry.id.isNotEmpty()) {
            entry.id
        } else {
            entriesRef.child(userId).push().key ?: ""
        }
        
        val finalEntry = entry.copy(id = entryId)
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).setValue(finalEntry)
                .addOnSuccessListener {
                    // After successful save, update the mood count
                    updateMoodCount(
                        userId = userId,
                        mood = finalEntry.mood,
                        onSuccess = {
                            continuation.resume(entryId)
                        },
                        onError = { error ->
                            // Still resume with ID even if mood count fails
                            println("Warning: Could not update mood count: ${error.message}")
                            continuation.resume(entryId)
                        }
                    )
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        }
    }
    
    // Update an existing entry
    override suspend fun updateJournalEntry(entry: JournalEntry): Boolean {
        val userId = entry.userId.ifEmpty { "testUser" }
        val entryId = entry.id
        
        if (entryId.isEmpty()) {
            throw IllegalArgumentException("Entry ID must not be empty for updates")
        }
        
        return suspendCancellableCoroutine { continuation ->
            entriesRef.child(userId).child(entryId).setValue(entry)
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
        val userId = "testUser" // Default user ID
        
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
    
    // Update mood count in Firebase
    fun updateMoodCount(userId: String = "testUser", mood: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        // First get the current user data
        getOrCreateUser(
            userId = userId,
            onSuccess = { user ->
                // Update the mood count
                val updatedCounts = user.moodCounts.incrementMood(mood)
                
                // Save back to Firebase
                usersRef.child(userId).child("moodCounts").setValue(updatedCounts)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { error ->
                        onError(error)
                    }
            },
            onError = onError
        )
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