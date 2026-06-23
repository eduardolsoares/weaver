package ceub.weaver

import ceub.weaver.domain.model.ProjectResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class WebProjectApi {
    private val baseUrl = "http://127.0.0.1:8000/api/projects"
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                coerceInputValues = true
            })
        }
    }

    suspend fun fetchUserProjects(idToken: String): Result<List<ProjectResponse>> {
        return try {
            val response = client.get("$baseUrl/users") {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
            when (response.status.value) {
                401 -> Result.failure(Exception("AUTH_TOKEN_EXPIRED"))
                in 200..299 -> Result.success(response.body())
                else -> Result.failure(Exception("SERVER_ERROR"))
            }
        } catch (e: ResponseException) {
            if (e.response.status.value == 401) {
                Result.failure(Exception("AUTH_TOKEN_EXPIRED"))
            } else {
                Result.failure(Exception("SERVER_ERROR"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("NETWORK_ERROR"))
        }
    }

    suspend fun createProject(name: String, idToken: String): ProjectResponse? {
        return try {
            val response = client.post("$baseUrl/create") {
                parameter("name", name)
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
            if (response.status.value == 401) throw Exception("AUTH_TOKEN_EXPIRED")
            if (response.status.isSuccess()) response.body() else null
        } catch (e: ResponseException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteProject(id: String, idToken: String): Boolean {
        return try {
            val response = client.delete("$baseUrl/$id") {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
            if (response.status.value == 401) throw Exception("AUTH_TOKEN_EXPIRED")
            response.status.isSuccess()
        } catch (e: ResponseException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun renameProject(id: String, newName: String, idToken: String): Boolean {
        return try {
            val response = client.put("$baseUrl/$id") {
                parameter("new_name", newName)
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
            if (response.status.value == 401) throw Exception("AUTH_TOKEN_EXPIRED")
            response.status.isSuccess()
        } catch (e: ResponseException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
