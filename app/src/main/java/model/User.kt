

// User.kt
package model

data class User(
    val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val moodCounts: MoodCounts = MoodCounts()
)

data class MoodCounts(
    val happy: Int = 0,
    val neutral: Int = 0,
    val sad: Int = 0
) {
    fun total(): Int = happy + neutral + sad
    
    fun incrementMood(mood: String): MoodCounts {
        return when (mood) {
            "happy" -> this.copy(happy = happy + 1)
            "neutral" -> this.copy(neutral = neutral + 1)
            "sad" -> this.copy(sad = sad + 1)
            else -> this
        }
    }
}