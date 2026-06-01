package ceub.weaver.domain.model

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(val token: AuthToken) : AuthState
    data class Error(val message: String) : AuthState
}
