// AIAnalysis.kt
package model

data class AIAnalysis(
    val entryId: String = "",
    val sentiment: Double = 0.0,
    val emotions: Map<String, Double> = emptyMap(),
    val feedback: String = ""
)