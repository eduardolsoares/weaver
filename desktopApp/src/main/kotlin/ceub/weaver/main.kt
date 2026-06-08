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
import ceub.weaver.data.local.TokenStorage
import ceub.weaver.data.remote.GoogleOAuthClient
import ceub.weaver.data.repository.GoogleAuthRepositoryImpl
import ceub.weaver.domain.usecase.GoogleLoginUseCase
import io.github.cdimascio.dotenv.dotenv
fun main() = application {
    var token by remember { mutableStateOf<String?>(null) }
    val env = remember { 
        dotenv()
     }
    val clientSecret = env["GOOGLE_CLIENT_SECRET"]
    if (token == null) {
        val oauthClient = remember {
            GoogleOAuthClient(
                clientId = GOOGLE_CLIENT_ID,
                redirectUri = GOOGLE_REDIRECT_URI,
                clientSecret = clientSecret,
            )
        }
        val storage = remember { TokenStorage() }
        val repository = remember { GoogleAuthRepositoryImpl(storage, oauthClient) }
        val loginUseCase = remember { GoogleLoginUseCase(repository) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "weaver",
            state = WindowState(size = DpSize(800.dp, 960.dp)),
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
                        GoogleAuth.iniciarLogin(
                            useCase = loginUseCase,
                            onSuccess = { token = "skipped" },
                            onError = { println("Login error: ${it.message}") }
                        )
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
