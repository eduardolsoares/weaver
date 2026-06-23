package ceub.weaver.data.remote

import ceub.weaver.domain.model.ProjectResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.util.Base64

class ProjectApiService {

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                coerceInputValues = true
            })
        }
    }

    private val baseUrl = "http://127.0.0.1:8000/api/projects"

    fun getPhotoUrl(idToken: String): String? {
        if (idToken.isBlank() || idToken == "skipped") return null
        return try {
            val parts = idToken.split(".")
            if (parts.size < 2) {
                println("O token recebido não é um JWT válido (Faltam pontos). Tamanho: ${parts.size}")
                return null
            }

            val payload64 = parts[1]
            val decodedBytes = Base64.getUrlDecoder().decode(payload64)
            val jsonString = String(decodedBytes)

            val jsonElement = Json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject

            val url = jsonObject["picture"]?.jsonPrimitive?.content
                ?: jsonObject["avatar"]?.jsonPrimitive?.content
                ?: jsonObject["picture_url"]?.jsonPrimitive?.content

            url
        } catch (e: Exception) {
            println("[ApiService DEBUG] Erro bruto ao decodificar Base64: ${e.message}")
            null
        }
    }

    suspend fun fetchUserProjects(idToken: String): Result<List<ProjectResponse>> {
        return try {
            val response = client.get("$baseUrl/users") {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }

            when (response.status.value) {
                401 -> Result.failure(IllegalArgumentException("AUTH_TOKEN_EXPIRED"))
                in 200..299 -> Result.success(response.body())
                else -> Result.failure(Exception("SERVER_ERROR"))
            }
        } catch (e: ResponseException) {
            println("[ApiService] Erro de resposta do servidor: Status ${e.response.status.value}")
            if (e.response.status.value == 401) {
                Result.failure(IllegalArgumentException("AUTH_TOKEN_EXPIRED"))
            } else {
                Result.failure(Exception("SERVER_ERROR"))
            }
        } catch (e: IOException) {
            println("[ApiService] Erro de conexão de rede/timeout: ${e.message}")
            Result.failure(Exception("NETWORK_ERROR"))
        }
    }

    suspend fun createProject(projectName: String, idToken: String): ProjectResponse? {
        return try {
            val response = client.post("$baseUrl/create") {
                parameter("name", projectName)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $idToken")
                }
            }
            if (response.status.value == 401) throw IllegalArgumentException("AUTH_TOKEN_EXPIRED")
            if (response.status.isSuccess()) response.body() else null
        } catch (e: ResponseException) {
            println("Erro de resposta do servidor ao criar projeto: Status ${e.response.status.value}")
            null
        } catch (e: IOException) {
            println("Erro de rede ao criar projeto: ${e.message}")
            null
        }
    }

    suspend fun deleteProject(projectId: String, idToken: String): Boolean {
        return try {
            val response = client.delete("$baseUrl/$projectId") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $idToken")
                }
            }
            if (response.status.value == 401) throw IllegalArgumentException("AUTH_TOKEN_EXPIRED")
            response.status.isSuccess()
        } catch (e: ResponseException) {
            println("Erro de resposta do servidor ao deletar projeto: Status ${e.response.status.value}")
            false
        } catch (e: IOException) {
            println("Erro de rede ao deletar projeto: ${e.message}")
            false
        }
    }

    suspend fun renameProject(projectId: String, newName: String, idToken: String): Boolean {
        return try {
            val response = client.put("$baseUrl/$projectId") {
                parameter("new_name", newName)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $idToken")
                }
            }
            if (response.status.value == 401) throw IllegalArgumentException("AUTH_TOKEN_EXPIRED")
            response.status.isSuccess()
        } catch (e: ResponseException) {
            println("Erro de resposta do servidor ao renomear projeto: Status ${e.response.status.value}")
            false
        } catch (e: IOException) {
            println("Erro de rede ao renomear projeto: ${e.message}")
            false
        }
    }
}