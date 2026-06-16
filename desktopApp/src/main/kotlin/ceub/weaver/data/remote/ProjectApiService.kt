package ceub.weaver.data.remote

import ceub.weaver.domain.model.ProjectCreateRequest
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
            })
        }
    }

    private val baseUrl = "http://127.0.0.1:8000/api/projects"

    suspend fun fetchUserProjects(email: String): List<ProjectResponse> {
        return try {
            client.get("$baseUrl/users/$email").body()
        } catch (e: Exception) {
            println("Erro ao buscar projetos: ${e.message}")
            emptyList()
        }
    }

    suspend fun createProject(name: String, email: String): ProjectResponse? {
        return try {
            client.post("$baseUrl/create") {
                contentType(ContentType.Application.Json)

                setBody(ProjectCreateRequest(name = name, userEmail = email))
            }.body()
        } catch (e: Exception) {
            println("Erro ao criar projeto: ${e.message}")
            null
        }
    }

    suspend fun deleteProject(projectId: String): Boolean {
        return try {
            val response = client.delete("$baseUrl/delete/$projectId")
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Erro ao deletar projeto: ${e.message}")
            false
        }
    }
}