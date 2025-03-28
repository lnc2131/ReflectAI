package api

data class OpenAIRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<Message>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 150
) {
    data class Message(
        val role: String,
        val content: String
    )
}
