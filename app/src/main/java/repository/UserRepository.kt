package repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.suspendCancellableCoroutine
import model.User
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository {
    private val auth = Firebase.auth
    private val database = FirebaseDatabase.getInstance()
    private val usersRef = database.getReference("users")
    
    suspend fun getCurrentUser(): User? = suspendCancellableCoroutine { continuation ->
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        
        usersRef.child(currentUser.uid).get()
            .addOnSuccessListener { snapshot ->
                val user = if (snapshot.exists()) {
                    snapshot.getValue(User::class.java)
                } else {
                    // Create a new user if it doesn't exist in the database yet
                    val newUser = User(
                        id = currentUser.uid,
                        displayName = currentUser.displayName ?: "",
                        email = currentUser.email ?: ""
                    )
                    usersRef.child(currentUser.uid).setValue(newUser)
                    newUser
                }
                continuation.resume(user)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
    
    suspend fun updateUserProfile(user: User): Boolean = suspendCancellableCoroutine { continuation ->
        if (user.id.isEmpty()) {
            continuation.resumeWithException(IllegalArgumentException("User ID cannot be empty"))
            return@suspendCancellableCoroutine
        }
        
        usersRef.child(user.id).setValue(user)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
    
    suspend fun getUserById(userId: String): User? = suspendCancellableCoroutine { continuation ->
        usersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->
                val user = if (snapshot.exists()) {
                    snapshot.getValue(User::class.java)
                } else {
                    null
                }
                continuation.resume(user)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
    
    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
}