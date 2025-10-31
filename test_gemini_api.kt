// Simple test script to validate Google AI API connection
// This can be run as a standalone test

import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    runBlocking {
        testGeminiAPI()
    }
}

suspend fun testGeminiAPI() {
    val apiKey = "YOUR_API_KEY_HERE" // Replace with actual API key
    val model = "gemini-pro"
    val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
    val endpoint = "$baseUrl/models/$model:generateContent?key=$apiKey"
    
    val requestBody = """
    {
        "contents": [
            {
                "parts": [
                    {
                        "text": "Hello, this is a test."
                    }
                ],
                "role": "user"
            }
        ],
        "generationConfig": {
            "temperature": 0.7,
            "maxOutputTokens": 10
        }
    }
    """.trimIndent()
    
    try {
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        
        // Send request
        val writer = OutputStreamWriter(connection.outputStream)
        writer.write(requestBody)
        writer.flush()
        writer.close()
        
        // Read response
        val responseCode = connection.responseCode
        println("Response Code: $responseCode")
        
        val reader = if (responseCode == 200) {
            BufferedReader(InputStreamReader(connection.inputStream))
        } else {
            BufferedReader(InputStreamReader(connection.errorStream))
        }
        
        val response = reader.readText()
        reader.close()
        
        println("Response: $response")
        
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }
}