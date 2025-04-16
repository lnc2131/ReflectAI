package screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import api.OpenAIClient
import api.OpenAIRequest
import com.example.reflectai.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import model.AIAnalysis
import model.JournalEntry
import repository.JournalRepository
import repository.SimpleJournalRepository
import service.SentimentAnalysisService
import java.util.UUID

class JournalEntryViewModel : ViewModel() {
    // States for the UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    // Our simplified repository
    private val repository = SimpleJournalRepository()

    // Process entry and save
    fun processJournalEntry(title: String, content: String) {
        _isLoading.value = true
        _errorMessage.value = ""
        _responseText.value = ""
        _saveSuccess.value = false

        viewModelScope.launch {
            try {
                // Create simple request - EXACTLY like TestAPIScreen
                val messages = listOf(
                    OpenAIRequest.Message(
                        role = "user",
                        content = "Analyze this journal entry: $content"
                    )
                )

                val request = OpenAIRequest(messages = messages)

                // Make API call - EXACTLY like TestAPIScreen
                val response = OpenAIClient.service.createChatCompletion(
                    authorization = OpenAIClient.getAuthHeader(),
                    request = request
                )

                if (response.isSuccessful) {
                    val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                    _responseText.value = aiResponse

                    // Create the journal entry
                    val entry = JournalEntry(
                        id = "", // Let Firebase generate an ID
                        title = title,
                        content = content,
                        aiAnalysis = aiResponse,
                        timestamp = System.currentTimeMillis()
                    )

                    // Save to Firebase
                    repository.saveEntry(
                        entry = entry,
                        onSuccess = {
                            _saveSuccess.value = true
                            _isLoading.value = false
                        },
                        onError = { e ->
                            _errorMessage.value = "Error saving to Firebase: ${e.message}"
                            _isLoading.value = false
                        }
                    )
                } else {
                    val error = response.errorBody()?.string() ?: "Unknown error"
                    _errorMessage.value = "API Error: $error"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Exception: ${e.message}"
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }

    // Add a function to save without analysis for backup
    fun saveWithoutAnalysis(title: String, content: String) {
        _isLoading.value = true
        _errorMessage.value = ""
        _saveSuccess.value = false

        val entry = JournalEntry(
            id = "", // Let Firebase generate an ID
            title = title,
            content = content,
            aiAnalysis = "No analysis performed",
            timestamp = System.currentTimeMillis()
        )

        repository.saveEntry(
            entry = entry,
            onSuccess = {
                _saveSuccess.value = true
                _isLoading.value = false
            },
            onError = { e ->
                _errorMessage.value = "Failed to save: ${e.message}"
                _isLoading.value = false
            }
        )
    }
}