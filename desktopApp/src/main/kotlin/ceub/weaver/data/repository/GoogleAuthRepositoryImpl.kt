package ceub.weaver.data.repository

import ceub.weaver.data.local.TokenStorage
import ceub.weaver.data.remote.GoogleOAuthClient
import ceub.weaver.domain.model.AuthState
import ceub.weaver.domain.model.AuthToken
import ceub.weaver.domain.repository.AuthRepository

class GoogleAuthRepositoryImpl(
    private val storage: TokenStorage,
    private val client: GoogleOAuthClient
) : AuthRepository {

    override suspend fun getAuthState(): AuthState {
        val token = storage.load() ?: return AuthState.Idle
        return if (isTokenValid(token)) {
            AuthState.Authenticated(token)
        } else {
            val refreshed = tryRefreshToken(token)
            if (refreshed != null) {
                storage.save(refreshed)
                AuthState.Authenticated(refreshed)
            } else {
                storage.clear()
                AuthState.Idle
            }
        }
    }

    override suspend fun saveToken(token: AuthToken) {
        storage.save(token)
    }

    override suspend fun clearToken() {
        storage.clear()
    }

    override suspend fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String
    ): AuthToken {
        val token = client.exchangeAuthorizationCode(code, codeVerifier)

        storage.save(token)

        return token
    }

    override suspend fun refreshToken(refreshToken: String): AuthToken {
        val token = client.refreshToken(refreshToken)
        storage.save(token)
        return token
    }

    override suspend fun validateToken(token: AuthToken): Boolean {
        if (isTokenExpired(token)) {
            val refreshed = tryRefreshToken(token) ?: return false
            storage.save(refreshed)
            return true
        }
        return client.validateToken(token.accessToken)
    }

    private fun isTokenExpired(token: AuthToken): Boolean {
        val elapsed = System.currentTimeMillis() - token.acquiredAt
        return elapsed >= (token.expiresIn * 1000) - 60_000
    }

    private suspend fun isTokenValid(token: AuthToken): Boolean {
        if (isTokenExpired(token)) return false
        return client.validateToken(token.accessToken)
    }

    private suspend fun tryRefreshToken(token: AuthToken): AuthToken? {
        val refreshToken = token.refreshToken ?: return null
        return try {
            client.refreshToken(refreshToken)
        } catch (_: Exception) {
            null
        }
    }
}
