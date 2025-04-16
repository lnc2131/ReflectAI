package api

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo-0125", // Updated to latest stable model version
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 500// Increased token limit for more detailed responses
) {
    data class Message(
        val role: String,
        val content: String
    )
}
