package ceub.weaver

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "weaver",
    ) {
        val composeScope = rememberCoroutineScope()

        var mudarParaHome: (() -> Unit)? by remember { mutableStateOf(null) }

        App(
            onGoogleLoginRequest = {
                GoogleAuth.iniciarLogin { token ->
                    println("Token recebido no Desktop: $token")

                    composeScope.launch {
                        mudarParaHome?.invoke()
                    }
                }
            },
            onLoginConfirmado = { acaoDeMudarTela ->
                mudarParaHome = acaoDeMudarTela
            }
        )
    }
}