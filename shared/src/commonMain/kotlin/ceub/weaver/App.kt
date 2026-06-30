package ceub.weaver

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ceub.weaver.domain.model.GraphSnapshot
import ceub.weaver.ui.mainScreen.MainScreen
import ceub.weaver.domain.model.ProjectResponse

enum class ScreenRoute {
    LOADING,
    LOGIN,
    MAIN,
    HOME
}

@Composable
fun App(
    token: String? = null,
    avatarBitmap: androidx.compose.ui.graphics.ImageBitmap? = null,
    onTokenChanged: (String?) -> Unit = {},
    onGoogleLoginRequest: (onSuccess: () -> Unit, onError: (Throwable) -> Unit) -> Unit = { _, _ -> },
    onFetchProjects: suspend (String) -> Result<List<ProjectResponse>> = { Result.failure(Exception("not implemented")) },
    onCreateProject: suspend (String, String) -> ProjectResponse? = { _, _ -> null },
    onDeleteProject: suspend (String, String) -> Boolean = { _, _ -> false },
    onRenameProject: suspend (String, String, String) -> Boolean = { _, _, _ -> false },
    onSaveGraph: suspend (String, GraphSnapshot) -> Boolean = { _, _ -> true },
    onLoadGraph: suspend (String) -> GraphSnapshot? = { null },
) {
    var currentScreen by remember(token) {
        mutableStateOf(
            when {
                token == "skipped" -> ScreenRoute.MAIN
                !token.isNullOrBlank() -> ScreenRoute.LOADING
                else -> ScreenRoute.LOGIN
            }
        )
    }

    var selectedProjectId by remember { mutableStateOf("") }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        if (token.isNullOrBlank()) {
            currentScreen = ScreenRoute.LOGIN
            return@LaunchedEffect
        }

        if (token == "skipped") {
            currentScreen = ScreenRoute.MAIN
            return@LaunchedEffect
        }

        println("Verificando token no servidor...")
        currentScreen = ScreenRoute.LOADING

        val result = onFetchProjects(token)

        if (result.isSuccess) {
            println("Token aprovado! Indo para MAIN.")
            authErrorMessage = null
            currentScreen = ScreenRoute.MAIN
        } else {
            val exception = result.exceptionOrNull()
            val errorMessage = exception?.message ?: ""
            println("Erro capturado na UI através do Result: $errorMessage")

            if (errorMessage == "AUTH_TOKEN_EXPIRED") {
                println("Token expirado. Limpando cache e indo para LOGIN.")
                authErrorMessage = "Sua sessão expirou. Faça login novamente."
                onTokenChanged(null)
            } else {
                println("Erro de rede ou servidor offline.")
                authErrorMessage = "Não foi possível conectar ao servidor. Verifique se o backend está rodando."
            }

            currentScreen = ScreenRoute.LOGIN
        }
    }

    when (currentScreen) {
        ScreenRoute.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        ScreenRoute.LOGIN -> {
            LoginScreen(
                errorMessage = authErrorMessage,
                onLoginSuccess = {
                    authErrorMessage = null
                    onTokenChanged("skipped")
                    currentScreen = ScreenRoute.MAIN
                },
                onGoogleLoginClick = {
                    onGoogleLoginRequest(
                        { println("Sucesso no fluxo Google. Aguardando validacao do token...") },
                        { error -> authErrorMessage = "Falha no login com o Google: ${error.message}" }
                    )
                }
            )
        }

        ScreenRoute.MAIN -> {
            MainScreen(
                idToken = token ?: "",
                avatarBitmap = avatarBitmap,

                onLogoutClick = {
                    println("Resetando token para null e limpando storage...")
                    onTokenChanged(null)
                    currentScreen = ScreenRoute.LOGIN
                },

                onNewProjectClick = { projectId ->
                    selectedProjectId = projectId
                    currentScreen = ScreenRoute.HOME
                },
                onProjectClick = { project ->
                    selectedProjectId = project.id
                    currentScreen = ScreenRoute.HOME
                },
                onFetchProjects = onFetchProjects,
                onCreateProject = onCreateProject,
                onDeleteProject = onDeleteProject,
                onRenameProject = onRenameProject
            )
        }

        ScreenRoute.HOME -> {
            HomeScreen(
                onBackToMain = { currentScreen = ScreenRoute.MAIN },
                projectId = selectedProjectId,
                onSaveGraph = onSaveGraph,
                onLoadGraph = onLoadGraph,
            )
        }
    }
}