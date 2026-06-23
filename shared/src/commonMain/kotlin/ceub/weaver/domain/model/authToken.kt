package ceub.weaver.domain.model

data class AuthToken(
    val accessToken: String,
    val idToken: String?,
    val refreshToken: String?,
    val expiresIn: Long,
    val scope: String,
    val tokenType: String,
    val acquiredAt: Long
)
