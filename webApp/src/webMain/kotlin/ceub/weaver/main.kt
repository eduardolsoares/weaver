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
            onGoogleLoginRequest = { onSuccess, _ -> onSuccess() },
            onSaveGraph = { _, _ -> true },
            onLoadGraph = { null }
        )
    }
}
