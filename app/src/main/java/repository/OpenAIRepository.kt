package repository

import api.OpenAIClient
import api.OpenAIRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import model.AIAnalysis
import model.JournalEntry
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OpenAIRepository : AIAnalysisRepository {
    private val database = FirebaseDatabase.getInstance()
    private val analysisRef = database.getReference("ai_analysis")

    override suspend fun analyzeJournalEntry(journalEntry: JournalEntry): AIAnalysis {
        try {
            // Create the request to OpenAI
            val messages = listOf(
                OpenAIRequest.Message(
                    role = "system",
                    content = "You are an AI assistant that analyzes journal entries. " +
                            "Provide sentiment analysis, identify emotions, and give supportive feedback."
                ),
                OpenAIRequest.Message(
                    role = "user",
                    content = "Analyze this journal entry and provide: " +
                            "1. A sentiment score from -1.0 (very negative) to 1.0 (very positive), " +
                            "2. The main emotions expressed (joy, sadness, anxiety, etc.), and " +
                            "3. Brief supportive feedback. " +
                            "Format as JSON with fields 'sentiment', 'emotions', and 'feedback'. " +
                            "Here's the entry: ${journalEntry.content}"
                )
            )

            val request = OpenAIRequest(messages = messages)

            // Make the API call
            val response = OpenAIClient.service.createChatCompletion(
                authorization = OpenAIClient.getAuthHeader(),
                request = request
            )

            if (response.isSuccessful) {
                val responseBody = response.body()
                val aiContent = responseBody?.choices?.firstOrNull()?.message?.content ?: ""

                // For now, create a simple AIAnalysis object
                // The actual parsing will happen in the SentimentAnalysisService
                return AIAnalysis(
                    entryId = journalEntry.id,
                    sentiment = 0.0,
                    emotions = emptyMap(),
                    feedback = aiContent
                )
            } else {
                throw Exception("API call failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            throw Exception("Error analyzing journal entry: ${e.message}")
        }
    }

    override suspend fun saveAnalysis(analysis: AIAnalysis): Boolean = suspendCancellableCoroutine { continuation ->
        analysisRef.child(analysis.entryId).setValue(analysis)
            .addOnSuccessListener {
                continuation.resume(true)
            }
            .addOnFailureListener {
                continuation.resume(false)
            }
    }

    override suspend fun getAnalysisForEntry(entryId: String): AIAnalysis? = suspendCancellableCoroutine { continuation ->
        analysisRef.child(entryId).get()
            .addOnSuccessListener { snapshot ->
                val analysis = if (snapshot.exists()) {
                    snapshot.getValue(AIAnalysis::class.java)
                } else {
                    null
                }
                continuation.resume(analysis)
            }
            .addOnFailureListener {
                continuation.resumeWithException(it)
            }
    }
}