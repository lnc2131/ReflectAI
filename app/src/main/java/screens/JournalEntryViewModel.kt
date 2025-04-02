package screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reflectai.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import model.AIAnalysis
import model.JournalEntry
import repository.JournalRepository
import repository.FirebaseJournalRepository
import service.SentimentAnalysisService

class JournalEntryViewModel(
    private val journalRepository: JournalRepository = FirebaseJournalRepository(),
    private val sentimentAnalysisService: SentimentAnalysisService = SentimentAnalysisService()
) : ViewModel() {

    private val _analysis = MutableStateFlow<AIAnalysis?>(null)
    val analysis: StateFlow<AIAnalysis?> = _analysis

    // Add this function for API key testing
    fun testApiKey() {
        val apiKey = BuildConfig.OPENAI_API_KEY
        println("API Key Test ==========")
        println("API Key length: ${apiKey.length}")
        println("API Key first 5 chars: ${if (apiKey.length >= 5) apiKey.substring(0, 5) else apiKey}...")
        println("API Key is empty: ${apiKey.isEmpty()}")
        println("API Key contains 'sk-': ${apiKey.startsWith("sk-")}")
        println("=====================")
    }

    suspend fun saveAndAnalyzeEntry(entry: JournalEntry) {
        try {
            // Save the entry to the database
            val entryId = journalRepository.addJournalEntry(entry)

            // Update the entry with the generated ID if needed
            val updatedEntry = if (entry.id != entryId) entry.copy(id = entryId) else entry

            try {
                // Analyze the entry with OpenAI
                println("Starting analysis for entry: ${updatedEntry.id}")
                val result = sentimentAnalysisService.analyzeEntry(updatedEntry)
                println("Analysis completed successfully")
                
                // Update the UI with the analysis result
                _analysis.value = result
            } catch (e: Exception) {
                println("Error during analysis: ${e.javaClass.simpleName}: ${e.message}")
                e.printStackTrace()
                
                // Still provide a result even if analysis fails
                _analysis.value = AIAnalysis(
                    entryId = updatedEntry.id,
                    sentiment = 0.0,
                    emotions = emptyMap(),
                    feedback = "Sorry, we couldn't analyze your entry at this time. Your journal entry has been saved."
                )
            }
        } catch (e: Exception) {
            println("Error saving entry: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            
            // Handle errors
            _analysis.value = AIAnalysis(
                entryId = entry.id,
                sentiment = 0.0,
                emotions = emptyMap(),
                feedback = "Error: Unable to save your journal entry. Please try again."
            )
        }
    }
}