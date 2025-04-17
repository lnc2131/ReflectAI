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
import repository.FirebaseJournalRepository
import repository.SimpleJournalRepository
import service.SentimentAnalysisService
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class JournalEntryViewModel(
    initialDate: String? = null,
    entryId: String? = null
) : ViewModel() {
    // States for the UI
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _responseText = MutableStateFlow("")
    val responseText: StateFlow<String> = _responseText

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess
    
    // Entry state for loading existing entries
    private val _entry = MutableStateFlow<JournalEntry?>(null)
    val entry: StateFlow<JournalEntry?> = _entry
    
    // Repository for Firebase operations
    private val simpleRepository = SimpleJournalRepository()
    private val repository = FirebaseJournalRepository()
    
    // Current date or entry ID
    private var currentDate: String? = null
    private var currentEntryId: String? = null
    
    init {
        // Set initial values from constructor
        if (initialDate != null) {
            currentDate = initialDate
            println("ViewModel initialized with date: $initialDate")
            
            // If we have an initial date, try to load the entry
            if (initialDate.isNotEmpty()) {
                loadEntryByDate(initialDate)
            }
        }
        
        if (entryId != null) {
            currentEntryId = entryId
            println("ViewModel initialized with entryId: $entryId")
            
            // If we have an entry ID, load that entry
            if (entryId.isNotEmpty()) {
                loadEntryById(entryId)
            }
        }
        
        println("JournalEntryViewModel initialized, currentDate=$currentDate, currentEntryId=$currentEntryId")
    }
    
    // Load entry by date
    fun loadEntryByDate(dateString: String) {
        _isLoading.value = true
        currentDate = dateString
        
        // First check if the date format is valid
        if (dateString.isEmpty()) {
            _errorMessage.value = "Invalid date format"
            _isLoading.value = false
            return
        }
        
        println("Loading entry for date: $dateString (raw input)")
        
        // Ensure date string is in the correct format
        val formattedDate = try {
            // Try to parse and reformat to ensure consistent format
            val date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            // If parsing fails, use the original string
            println("Error parsing date: ${e.message}, using original")
            dateString
        }
        
        println("Using formatted date: $formattedDate")
        
        // Use simple repository to get entries by date
        simpleRepository.getEntriesByDate(
            dateString = formattedDate,
            userId = "testUser",
            onSuccess = { entries ->
                if (entries.isNotEmpty()) {
                    // Use the first entry if multiple exist for the same date
                    val foundEntry = entries.first()
                    _entry.value = foundEntry
                    _responseText.value = foundEntry.aiAnalysis
                    println("Loaded entry: ${foundEntry.id}, title: ${foundEntry.title}")
                } else {
                    _errorMessage.value = "No entry found for this date"
                    println("No entries found for date: $dateString")
                }
                _isLoading.value = false
            },
            onError = { error ->
                _errorMessage.value = "Error loading entry: ${error.message}"
                println("Error loading entry for date $dateString: ${error.message}")
                _isLoading.value = false
            }
        )
    }
    
    // Load entry by ID
    fun loadEntryById(entryId: String) {
        _isLoading.value = true
        currentEntryId = entryId
        
        simpleRepository.getEntry(
            entryId = entryId,
            onSuccess = { loadedEntry ->
                _entry.value = loadedEntry
                loadedEntry?.let { 
                    _responseText.value = it.aiAnalysis
                }
                _isLoading.value = false
            },
            onError = { e ->
                _errorMessage.value = "Error loading entry: ${e.message}"
                _isLoading.value = false
            }
        )
    }

    // Process entry and save
    fun processJournalEntry(title: String, content: String) {
        _isLoading.value = true
        _errorMessage.value = ""
        _responseText.value = ""
        _saveSuccess.value = false

        viewModelScope.launch {
            try {
                // Create simple request
                val messages = listOf(
                    OpenAIRequest.Message(
                        role = "user",
                        content = "Analyze this journal entry: $content"
                    )
                )

                val request = OpenAIRequest(messages = messages)

                // Make API call
                val response = OpenAIClient.service.createChatCompletion(
                    authorization = OpenAIClient.getAuthHeader(),
                    request = request
                )

                if (response.isSuccessful) {
                    val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                    _responseText.value = aiResponse

                    // Create or update the journal entry
                    val date = currentDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    println("Using date for entry: $date (currentDate=$currentDate)")
                    
                    val entry = if (_entry.value != null) {
                        // Update existing entry
                        _entry.value!!.copy(
                            title = title,
                            content = content,
                            aiAnalysis = aiResponse,
                            date = date,
                            timestamp = System.currentTimeMillis()
                        )
                    } else {
                        // Create new entry
                        JournalEntry(
                            id = currentEntryId ?: "", // Use existing ID or let Firebase generate one
                            title = title,
                            content = content,
                            aiAnalysis = aiResponse,
                            date = date,
                            timestamp = System.currentTimeMillis()
                        )
                    }

                    // Save to Firebase
                    simpleRepository.saveEntry(
                        entry = entry,
                        onSuccess = {
                            _entry.value = entry
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

    // Save without analysis for backup
    fun saveWithoutAnalysis(title: String, content: String) {
        _isLoading.value = true
        _errorMessage.value = ""
        _saveSuccess.value = false

        // Create or update the journal entry
        val date = currentDate ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        println("Using date for entry (no analysis): $date (currentDate=$currentDate)")
        
        val entry = if (_entry.value != null) {
            // Update existing entry
            _entry.value!!.copy(
                title = title,
                content = content,
                date = date,
                timestamp = System.currentTimeMillis()
            )
        } else {
            // Create new entry
            JournalEntry(
                id = currentEntryId ?: "", // Use existing ID or let Firebase generate one
                title = title,
                content = content,
                aiAnalysis = "No analysis performed",
                date = date,
                timestamp = System.currentTimeMillis()
            )
        }

        simpleRepository.saveEntry(
            entry = entry,
            onSuccess = {
                _entry.value = entry
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