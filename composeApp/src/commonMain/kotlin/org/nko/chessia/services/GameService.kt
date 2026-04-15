package org.nko.chessia.services


import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.nko.chessia.models.AIProvider
import org.nko.chessia.models.Extractor


class GameService {


    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun fetchMoveFromAIProvider(
        fen: String,
        difficulty: String,
        provider: AIProvider
    ): String? {
        val body = provider.bodyTemplate
            .replace("{{fen}}", fen)
            .replace("{{difficulty}}", difficulty)
            .replace("{{apiKey}}", provider.apiKey)

        println("URL: ${provider.endpoint.replace("{{apiKey}}", provider.apiKey)}")


        val response: HttpResponse = httpClient.request(provider.endpoint.replace("{{apiKey}}", provider.apiKey)) {
            method = HttpMethod.parse(provider.method)
            provider.headers.forEach { (key, value) ->
                header(key, value.replace("{{apiKey}}", provider.apiKey))
            }
            setBody(TextContent(body, contentType = ContentType.Application.Json))
        }

        val responseText = response.bodyAsText()

        return when (provider.extract.type) {
            "regex" -> {
                val pattern = Regex(provider.extract.pattern ?: "")
                pattern.find(responseText)?.value
            }
            "jsonpath" -> {
                try {
                    val jsonElement = Json.parseToJsonElement(responseText)
                    extractJsonPath(jsonElement, provider.extract.path ?: "")
                } catch (e: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    private fun extractJsonPath(json: JsonElement, path: String): String? {
        val keys = path.removePrefix("$.").split(".")
        var current: JsonElement = json
        for (key in keys) {
            val arrayRegex = Regex("(.+)\\[(\\d+)]")
            current = when {
                key.matches(arrayRegex) -> {
                    val (k, index) = arrayRegex.matchEntire(key)!!.destructured
                    current.jsonObject[k]?.jsonArray?.get(index.toInt()) ?: return null
                }
                else -> current.jsonObject[key] ?: return null
            }
        }
        return current.jsonPrimitive.contentOrNull
    }


    fun getAIProviders(): List<AIProvider> {
        return listOf(
            AIProvider(
                name = "Gemini",
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key={{apiKey}}",
                method = "POST",
                headers = mapOf(
                    "Content-Type" to "application/json"
                ),
                bodyTemplate = """
                {
                  "contents": [{
                    "parts": [{
                      "text": "Estamos jugando ajedrez, es tu turno, responde solamente con el movimiento en formato uci, este es el fen: {{fen}}, considera la dificultad: {{difficulty}}."
                    }]
                  }]
                }
            """.trimIndent(),
                extract = Extractor(type = "jsonpath", path = "$.candidates[0].content.parts[0].text"),
                apiKey = ""
            ),
            AIProvider(
                name = "Groq",
                endpoint = "https://api.groq.com/v1/chat/completions",
                method = "POST",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer {{apiKey}}"
                ),
                bodyTemplate = """
                    {
                      "model": "mixtral-8x7b-32768",
                      "messages": [{
                        "role": "user",
                        "content": "FEN: {{fen}}, play {{difficulty}}, return UCI move only."
                      }]
                    }
                """.trimIndent(),
                extract = Extractor(type = "jsonpath", path = "$.choices[0].message.content"),
                apiKey = ""
            ),
            AIProvider(
                name = "DeepSeek",
                endpoint = "https://api.deepseek.com/chat/completions",
                method = "POST",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer {{apiKey}}"
                ),
                bodyTemplate = """
                    {
                      "model": "deepseek-chat",
                      "messages": [
                        {"role": "system", "content": "You are a chess AI assistant. You will receive a FEN and a difficulty level."},
                        {"role": "user", "content": "FEN: {{fen}}, play {{difficulty}}, return only UCI move."}
                      ],
                      "stream": false
                    }
                """.trimIndent(),
                extract = Extractor(
                    type = "jsonpath",
                    path = "$.choices[0].message.content"
                ),
                apiKey = ""
            )

        )
    }


}