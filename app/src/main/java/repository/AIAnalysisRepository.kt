package repository

import model.AIAnalysis
import model.JournalEntry

interface AIAnalysisRepository {
    suspend fun analyzeJournalEntry(journalEntry: JournalEntry): AIAnalysis
    suspend fun saveAnalysis(analysis: AIAnalysis): Boolean
    suspend fun getAnalysisForEntry(entryId: String): AIAnalysis?
}