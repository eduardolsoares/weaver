package ceub.weaver

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

import weaver.shared.generated.resources.Res
enum class Rota {
    LOGIN,
    HOME
}

@Composable
fun App(
    onGoogleLoginRequest: () -> Unit,
    onLoginConfirmado: (() -> Unit) -> Unit
) {
    var telaAtual by remember { mutableStateOf(Rota.LOGIN) }

    onLoginConfirmado {
        telaAtual = Rota.HOME
    }

    when (telaAtual) {
        Rota.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    telaAtual = Rota.HOME
                },
                onGoogleLoginClick = onGoogleLoginRequest
            )
        }
        Rota.HOME -> {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    androidx.compose.material3.Text("BEM VINDO AO WEAVER STUDIO.")
}