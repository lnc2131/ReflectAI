package screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    suspend fun saveAndAnalyzeEntry(entry: JournalEntry) {
        try {
            // Save the entry to the database
            val entryId = journalRepository.addJournalEntry(entry)

            // Update the entry with the generated ID if needed
            val updatedEntry = if (entry.id != entryId) entry.copy(id = entryId) else entry

            // Analyze the entry with OpenAI
            val result = sentimentAnalysisService.analyzeEntry(updatedEntry)

            // Update the UI with the analysis result
            _analysis.value = result
        } catch (e: Exception) {
            // Handle errors (in a real app, you'd want better error handling)
            _analysis.value = AIAnalysis(
                entryId = entry.id,
                sentiment = 0.0,
                emotions = emptyMap(),
                feedback = "Error: ${e.message}"
            )
        }
    }
}