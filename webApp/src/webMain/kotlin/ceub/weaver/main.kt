package ceub.weaver

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(
            onGoogleLoginRequest = { onSuccess, _ -> onSuccess() },
            onSaveGraph = { _, _ -> true },
            onLoadGraph = { null }
        )
    }
}