package ceub.weaver

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.localStorage

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val api = WebProjectApi()

    ComposeViewport {
        var token by remember { mutableStateOf(localStorage.getItem("token")) }

        App(
            token = token,
            avatarBitmap = null,
            onTokenChanged = { newToken ->
                token = newToken
                if (newToken != null) {
                    localStorage.setItem("token", newToken)
                } else {
                    localStorage.removeItem("token")
                }
            },
            onGoogleLoginRequest = { onSuccess, _ ->
                localStorage.setItem("token", "skipped")
                token = "skipped"
                onSuccess()
            },
            onFetchProjects = { idToken -> api.fetchUserProjects(idToken) },
            onCreateProject = { name, idToken -> api.createProject(name, idToken) },
            onDeleteProject = { id, idToken -> api.deleteProject(id, idToken) },
            onRenameProject = { id, name, idToken -> api.renameProject(id, name, idToken) }
        )
    }
}
