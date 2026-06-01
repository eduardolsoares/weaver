package ceub.weaver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {
    var token by remember { mutableStateOf<String?>(null) }

    if (token == null) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "weaver",
            state = WindowState(size = DpSize(800.dp, 800.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C0F)),
                contentAlignment = Alignment.Center,
            ) {
                LoginScreen(
                    onLoginSuccess = {
                        token = "skipped"
                    },
                    onGoogleLoginClick = {
                        GoogleAuth.iniciarLogin { t ->
                            println("Token recebido no Desktop: $t")
                            token = t
                        }
                    }
                )
            }
        }
    }

    if (token != null) {
        Window(
            onCloseRequest = ::exitApplication,
            title = "weaver - database modeler",
            state = WindowState(
                placement = WindowPlacement.Maximized,
                size = DpSize(1400.dp, 960.dp),
            ),
        ) {
            Box(Modifier.fillMaxSize().background(Color(0xFF0A0C0F))) {
                HomeScreen()
            }
        }
    }
}
