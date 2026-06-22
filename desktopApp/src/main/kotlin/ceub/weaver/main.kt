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
import ceub.weaver.data.remote.ProjectApiService
import ceub.weaver.data.repository.GoogleAuthRepositoryImpl
import ceub.weaver.domain.usecase.GoogleLoginUseCase
import ceub.weaver.domain.model.AuthToken
import io.github.cdimascio.dotenv.dotenv

import androidx.compose.ui.graphics.toComposeImageBitmap

fun main() = application {
    val storage = remember { TokenStorage() }
    val apiService = remember { ProjectApiService() }
    val env = remember { dotenv() }
    val clientSecret = env["GOOGLE_CLIENT_SECRET"]
    var authTokenState by remember { mutableStateOf(storage.load()) }
    var avatarBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(authTokenState) {
        val currentAuth = authTokenState
        val idTokenLocal = currentAuth?.idToken

        if (!idTokenLocal.isNullOrBlank()) {
            try {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val photoUrl = apiService.getPhotoUrl(idTokenLocal)

                    if (!photoUrl.isNullOrBlank()) {
                        val uri = java.net.URI.create(photoUrl)
                        val url = uri.toURL()

                        val connection = url.openConnection() as java.net.HttpURLConnection
                        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        connection.connectTimeout = 5000
                        connection.readTimeout = 5000

                        val inputStream = connection.inputStream
                        val bufferedImage = javax.imageio.ImageIO.read(inputStream)

                        if (bufferedImage != null) {
                            avatarBitmap = bufferedImage.toComposeImageBitmap()
                        }
                        inputStream.close()
                    }
                }
            } catch (e: Exception) {
                println("[DEBUG FOTO] ERRO CRÍTICO NO PROCESSO: ${e.message}")
                e.printStackTrace()
                avatarBitmap = null
            }
        } else {
            avatarBitmap = null
        }
    }

    val oauthClient = remember {
        GoogleOAuthClient(
            clientId = GOOGLE_CLIENT_ID,
            redirectUri = GOOGLE_REDIRECT_URI,
            clientSecret = clientSecret,
        )
    }
    val repository = remember { GoogleAuthRepositoryImpl(storage, oauthClient) }
    val loginUseCase = remember { GoogleLoginUseCase(repository) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "weaver - database modeler",
        state = remember {
            WindowState(
                placement = WindowPlacement.Floating,
                size = DpSize(1280.dp, 800.dp)
            )
        },
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0C0F)),
            contentAlignment = Alignment.Center,
        ) {
            App(
                token = authTokenState?.accessToken,
                avatarBitmap = avatarBitmap,
                onTokenChanged = { novoTokenString ->
                    if (novoTokenString == null) {
                        authTokenState = null
                        storage.clear()
                    } else {
                        authTokenState = AuthToken(
                            accessToken = novoTokenString,
                            idToken = null,
                            refreshToken = null,
                            expiresIn = 3600,
                            scope = "",
                            tokenType = "",
                            acquiredAt = System.currentTimeMillis()
                        )
                    }
                },
                onGoogleLoginRequest = { onSuccess, onError ->
                    GoogleAuth.iniciarLogin(
                        useCase = loginUseCase,
                        onSuccess = {
                            val authToken = storage.load()
                            authTokenState = authToken
                            onSuccess()
                        },
                        onError = { error ->
                            println("Login error: ${error.message}")
                            onError(error)
                        }
                    )
                },

                onFetchProjects = { _ -> apiService.fetchUserProjects(authTokenState?.idToken ?: "") },
                onCreateProject = { name, _ -> apiService.createProject(name, authTokenState?.idToken ?: "") },
                onDeleteProject = { id, _ -> apiService.deleteProject(id, authTokenState?.idToken ?: "") },
                onRenameProject = { id, name, _ -> apiService.renameProject(id, name, authTokenState?.idToken ?: "") }
            )
        }
    }
}