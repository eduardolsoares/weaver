package ceub.weaver.data.remote

import ceub.weaver.domain.model.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI

class GoogleOAuthClient(
    private val clientId: String,
    private val redirectUri: String,
    private val clientSecret: String? = null
) {
    private val tokenEndpoint = "https://oauth2.googleapis.com/token"
    private val tokenInfoEndpoint = "https://www.googleapis.com/oauth2/v1/tokeninfo"

    suspend fun exchangeAuthorizationCode(code: String, codeVerifier: String): AuthToken {
        return withContext(Dispatchers.IO) {
            val params = buildString {
                append("grant_type=authorization_code")
                append("&client_id=$clientId")
                append("&redirect_uri=$redirectUri")
                append("&code_verifier=$codeVerifier")
                append("&code=$code")
                if (clientSecret != null) append("&client_secret=$clientSecret")
            }
            val response = httpPost(params)
            parseTokenResponse(response)
        }
    }

    suspend fun validateToken(accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URI.create("$tokenInfoEndpoint?access_token=$accessToken").toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.responseCode == 200
            } catch (_: Exception) {
                false
            }
        }
    }

    suspend fun refreshToken(refreshToken: String): AuthToken {
        return withContext(Dispatchers.IO) {
            val params = buildString {
                append("grant_type=refresh_token")
                append("&client_id=$clientId")
                append("&refresh_token=$refreshToken")
                if (clientSecret != null) append("&client_secret=$clientSecret")
            }
            val response = httpPost(params)
            parseTokenResponse(response)
        }
    }

    private fun httpPost(params: String): String {
        val url = URI.create(tokenEndpoint).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        DataOutputStream(connection.outputStream).use { it.writeBytes(params) }

        val isSuccess = connection.responseCode in 200..299
        val stream = if (isSuccess) connection.inputStream else connection.errorStream

        val responseBody = BufferedReader(InputStreamReader(stream)).use { it.readText() }

        if (!isSuccess) {
            throw OAuthRequestException(
                "Token request failed: HTTP ${connection.responseCode} — $responseBody",
                responseBody
            )
        }

        return responseBody
    }

    private fun parseTokenResponse(json: String): AuthToken {
        val accessToken = extractJsonString(json, "access_token")
            ?: throw OAuthResponseException("Missing access_token in response: $json")

        val idToken = extractJsonString(json, "id_token")

        val refreshToken = extractJsonString(json, "refresh_token")
        val expiresIn = extractExpiresIn(json) ?: 3600L
        val scope = extractJsonString(json, "scope") ?: ""
        val tokenType = extractJsonString(json, "token_type") ?: "Bearer"

        return AuthToken(
            accessToken = accessToken,
            idToken = idToken,
            refreshToken = refreshToken,
            expiresIn = expiresIn,
            scope = scope,
            tokenType = tokenType,
            acquiredAt = System.currentTimeMillis()
        )
    }

    private fun extractJsonString(json: String, key: String): String? {
        val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractExpiresIn(json: String): Long? {
        val regex = "\"expires_in\"\\s*:\\s*([0-9]+)".toRegex()
        return regex.find(json)?.groupValues?.getOrNull(1)?.toLongOrNull()
    }
}

class OAuthRequestException(message: String, responseBody: String) : Exception(message)
class OAuthResponseException(message: String) : Exception(message)