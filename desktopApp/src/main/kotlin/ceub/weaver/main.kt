package ceub.weaver

import ceub.weaver.data.local.TokenStorage
import ceub.weaver.data.remote.GoogleOAuthClient
import ceub.weaver.data.repository.GoogleAuthRepositoryImpl
import ceub.weaver.domain.usecase.GoogleLoginUseCase
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.cdimascio.dotenv.Dotenv

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "weaver",
    ) {
        val clientSecret = remember {
            try {
                Dotenv.load()["GOOGLE_CLIENT_SECRET"]
            } catch (_: Exception) {
                null
            }
        }

        val tokenStorage = remember { TokenStorage() }
        val oauthClient = remember { GoogleOAuthClient(GOOGLE_CLIENT_ID, GOOGLE_REDIRECT_URI, clientSecret) }
        val repository = remember { GoogleAuthRepositoryImpl(tokenStorage, oauthClient) }
        val loginUseCase = remember { GoogleLoginUseCase(repository) }

        App(
            onGoogleLoginRequest = { onSuccess, onError ->
                GoogleAuth.iniciarLogin(
                    useCase = loginUseCase,
                    onSuccess = onSuccess,
                    onError = onError
                )
            }
        )
    }
}