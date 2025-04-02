package repository

import api.OpenAIClient
import api.OpenAIRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import model.AIAnalysis
import model.JournalEntry
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.example.reflectai.BuildConfig

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
                            "Provide sentiment analysis, identify emotions, and give supportive feedback. " +
                            "Your responses must be in valid JSON format only."
                ),
                OpenAIRequest.Message(
                    role = "user",
                    content = "Analyze this journal entry and provide your response in this exact JSON format:\n" +
                            "{\n" +
                            "  \"sentiment\": (a number from -1.0 to 1.0),\n" +
                            "  \"emotions\": {\n" +
                            "    \"emotion1\": (score between 0.0-1.0),\n" +
                            "    \"emotion2\": (score between 0.0-1.0)\n" +
                            "  },\n" +
                            "  \"feedback\": \"Your supportive feedback here\"\n" +
                            "}\n\n" +
                            "Here's the entry: ${journalEntry.content}"
                )
            )

            val request = OpenAIRequest(messages = messages)

            println("About to call OpenAI API with entry: ${journalEntry.id}")
            println("Request details: model=${request.model}, temp=${request.temperature}, max_tokens=${request.max_tokens}")
            
            try {
                println("Making API call...")
                val response = OpenAIClient.service.createChatCompletion(
                    authorization = OpenAIClient.getAuthHeader(),
                    request = request
                )
                println("OpenAI API call completed")
                println("OpenAI API response status: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    println("Response body: $responseBody")
                    val aiContent = responseBody?.choices?.firstOrNull()?.message?.content ?: ""
                    println("AI content: $aiContent")

                    // For now, create a simple AIAnalysis object
                    // The actual parsing will happen in the SentimentAnalysisService
                    return AIAnalysis(
                        entryId = journalEntry.id,
                        sentiment = 0.0,
                        emotions = emptyMap(),
                        feedback = aiContent
                    )
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    println("API call failed with error: $errorBody")
                    throw Exception("API call failed: $errorBody")
                }
            } catch (e: Exception) {
                println("Exception during API call: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                throw Exception("Error analyzing journal entry: ${e.message}")
            }
        } catch (e: Exception) {
            println("Exception in analyzeJournalEntry: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
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