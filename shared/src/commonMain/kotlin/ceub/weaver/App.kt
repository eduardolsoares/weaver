package ceub.weaver

import androidx.compose.runtime.*

enum class Rota {
    LOGIN,
    HOME
}

@Composable
fun App(
    onGoogleLoginRequest: (onSuccess: () -> Unit, onError: (Throwable) -> Unit) -> Unit
) {
    var telaAtual by remember { mutableStateOf(Rota.LOGIN) }

    when (telaAtual) {
        Rota.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    telaAtual = Rota.HOME
                },
                onGoogleLoginClick = {
                    onGoogleLoginRequest(
                        { telaAtual = Rota.HOME },
                        { println("Login failed: ${it.message}") }
                    )
                }
            )
        }
        Rota.HOME -> {
            HomeScreen()
        }
    }
}