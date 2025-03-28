package service

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import model.AIAnalysis
import model.JournalEntry
import repository.AIAnalysisRepository
import repository.OpenAIRepository

class SentimentAnalysisService(
    private val repository: AIAnalysisRepository = OpenAIRepository()
) {
    suspend fun analyzeEntry(entry: JournalEntry): AIAnalysis {
        // First check if we already have an analysis for this entry
        val existingAnalysis = repository.getAnalysisForEntry(entry.id)

        if (existingAnalysis != null) {
            return existingAnalysis
        }

        // If not, request a new analysis
        val analysis = repository.analyzeJournalEntry(entry)

        // Try to parse the AI's response to extract structured data
        val parsedAnalysis = parseAIResponse(analysis.feedback, entry.id)

        // Save the parsed analysis
        repository.saveAnalysis(parsedAnalysis)

        return parsedAnalysis
    }

    // This function attempts to parse the JSON response from OpenAI
    private suspend fun parseAIResponse(aiResponse: String, entryId: String): AIAnalysis {
        return withContext(Dispatchers.IO) {
            try {
                // Look for JSON in the response
                val jsonStart = aiResponse.indexOf('{')
                val jsonEnd = aiResponse.lastIndexOf('}')

                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    val jsonString = aiResponse.substring(jsonStart, jsonEnd + 1)
                    val jsonElement = JsonParser.parseString(jsonString)

                    if (jsonElement.isJsonObject) {
                        val jsonObject = jsonElement.asJsonObject

                        // Extract sentiment score
                        val sentiment = if (jsonObject.has("sentiment")) {
                            jsonObject.get("sentiment").asDouble
                        } else {
                            0.0
                        }

                        // Extract emotions
                        val emotions = if (jsonObject.has("emotions") && jsonObject.get("emotions").isJsonObject) {
                            val emotionsObj = jsonObject.getAsJsonObject("emotions")
                            emotionsObj.entrySet().associate {
                                it.key to it.value.asDouble
                            }
                        } else {
                            emptyMap()
                        }

                        // Extract feedback
                        val feedback = if (jsonObject.has("feedback")) {
                            jsonObject.get("feedback").asString
                        } else {
                            aiResponse // Use the whole response if no feedback field
                        }

                        return@withContext AIAnalysis(
                            entryId = entryId,
                            sentiment = sentiment,
                            emotions = emotions,
                            feedback = feedback
                        )
                    }
                }

                // If parsing fails, return a basic analysis with the raw response
                AIAnalysis(
                    entryId = entryId,
                    sentiment = 0.0,
                    emotions = emptyMap(),
                    feedback = aiResponse
                )
            } catch (e: Exception) {
                // Handle any parsing errors by returning a basic analysis
                AIAnalysis(
                    entryId = entryId,
                    sentiment = 0.0,
                    emotions = emptyMap(),
                    feedback = "Error parsing AI response: ${e.message}. Raw response: $aiResponse"
                )
            }
        }
    }
}