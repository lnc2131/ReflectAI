package api

import com.example.reflectai.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object OpenAIClient {
    const val BASE_URL = "https://api.openai.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: OpenAIService = retrofit.create(OpenAIService::class.java)

    // Helper function to format the authorization header
    fun getAuthHeader(): String {
        val apiKey = BuildConfig.OPENAI_API_KEY
        println("API Key length: ${apiKey.length}")
        println("API Key first 5 chars: ${apiKey.take(5)}...")
        println("API Key is empty: ${apiKey.isEmpty()}")
        return "Bearer $apiKey"
    }
}