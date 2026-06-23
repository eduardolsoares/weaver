package ceub.weaver.domain.repository

import ceub.weaver.domain.model.AuthState
import ceub.weaver.domain.model.AuthToken

interface AuthRepository {
    suspend fun getAuthState(): AuthState
    suspend fun saveToken(token: AuthToken)
    suspend fun clearToken()
    suspend fun exchangeAuthorizationCode(code: String, codeVerifier: String): AuthToken
    suspend fun refreshToken(refreshToken: String): AuthToken
    suspend fun validateToken(token: AuthToken): Boolean
}
