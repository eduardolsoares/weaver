package ceub.weaver.domain.usecase

import ceub.weaver.domain.model.AuthToken
import ceub.weaver.domain.repository.AuthRepository

class GoogleLoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(authorizationCode: String, codeVerifier: String): Result<AuthToken> {
        return try {
            val token = repository.exchangeAuthorizationCode(authorizationCode, codeVerifier)
            repository.saveToken(token)
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
