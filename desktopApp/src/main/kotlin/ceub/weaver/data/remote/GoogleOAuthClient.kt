package ceub.weaver.data.remote

import ceub.weaver.domain.model.AuthToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
            val response = httpPost(tokenEndpoint, params)
            parseTokenResponse(response)
        }
    }

    suspend fun validateToken(accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$tokenInfoEndpoint?access_token=$accessToken")
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
            val response = httpPost(tokenEndpoint, params)
            parseTokenResponse(response)
        }
    }

    private fun httpPost(url: String, params: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

        DataOutputStream(connection.outputStream).use { it.writeBytes(params) }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            val errorReader = BufferedReader(
                InputStreamReader(connection.errorStream)
            )
            val body = errorReader.readText()
            throw OAuthRequestException(
                "Token exchange failed: HTTP ${connection.responseCode} — $body",
                body
            )
        }

        return BufferedReader(InputStreamReader(stream)).readText()
    }

    private fun parseTokenResponse(json: String): AuthToken {
        val idToken = extractJsonString(json, "id_token")

        val accessToken = extractJsonString(json, "access_token")
            ?: throw OAuthResponseException("Missing access_token in response: $json")

        val refreshToken = extractJsonString(json, "refresh_token")
        val expiresIn = extractJsonString(json, "expires_in")?.toLongOrNull() ?: 3600
        val scope = extractJsonString(json, "scope") ?: ""
        val tokenType = extractJsonString(json, "token_type") ?: "Bearer"

        return AuthToken(
            accessToken = idToken ?: accessToken,
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
}

class OAuthRequestException(message: String, val responseBody: String) : Exception(message)
class OAuthResponseException(message: String) : Exception(message)
