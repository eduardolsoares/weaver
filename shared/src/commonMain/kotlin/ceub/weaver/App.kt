package ceub.weaver

import androidx.compose.runtime.*
import ceub.weaver.ui.mainScreen.MainScreen
import ceub.weaver.domain.model.ProjectResponse

enum class ScreenRoute {
    LOGIN,
    MAIN,
    HOME
}

@Composable
fun App(
    token: String?,
    onTokenChanged: (String?) -> Unit,
    onGoogleLoginRequest: (onSuccess: () -> Unit, onError: (Throwable) -> Unit) -> Unit,
    onFetchProjects: suspend (String) -> List<ProjectResponse>,
    onCreateProject: suspend (String, String) -> ProjectResponse?,
    onDeleteProject: suspend (String) -> Boolean,
    onRenameProject: suspend (String, String) -> Boolean,
) {
    var currentScreen by remember(token) {
        mutableStateOf(if (token != null) ScreenRoute.MAIN else ScreenRoute.LOGIN)
    }
    var selectedProject by remember { mutableStateOf("") }

    when (currentScreen) {
        ScreenRoute.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    onTokenChanged("skipped")
                    currentScreen = ScreenRoute.MAIN
                },
                onGoogleLoginClick = {
                    onGoogleLoginRequest(
                        { currentScreen = ScreenRoute.MAIN },
                        { println("Login failed: ${it.message}") }
                    )
                }
            )
        }

        ScreenRoute.MAIN -> {
            MainScreen(
                userEmail = "arthur@email.com",
                onNewProjectClick = {
                    selectedProject = "New Project"
                    currentScreen = ScreenRoute.HOME
                },
                onProjectClick = { projectName ->
                    selectedProject = projectName
                    currentScreen = ScreenRoute.HOME
                },
                onFetchProjects = onFetchProjects,
                onCreateProject = onCreateProject,
                onDeleteProject = onDeleteProject,
                onRenameProject = onRenameProject
            )
        }

        ScreenRoute.HOME -> {
            HomeScreen()
        }
    }
}