package ceub.weaver.data.remote

import ceub.weaver.domain.model.ProjectResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

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

    suspend fun fetchUserProjects(idToken: String): List<ProjectResponse> {
        return try {
            val response = client.get("$baseUrl/users") {
                headers { append(HttpHeaders.Authorization, "Bearer $idToken") }
            }
            if (response.status.isSuccess()) response.body() else emptyList()
        } catch (e: Exception) {
            println("Erro ao buscar projetos: ${e.message}")
            emptyList()
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
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            println("Erro ao criar projeto: ${e.message}")
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
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erro ao deletar projeto: ${e.message}")
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
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erro ao renomear projeto: ${e.message}")
            false
        }
    }
}