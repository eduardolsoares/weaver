package ceub.weaver.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectCreateRequest(
    val name: String,
    @SerialName("user_email") val userEmail: String
)


@Serializable
    data class ProjectResponse(
        val id: String,
        val name: String,
        @SerialName("user_email") val userEmail: String? = "",
        @SerialName("updated_at") val updatedAt: String
    )
