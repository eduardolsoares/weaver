package ceub.weaver

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview

import weaver.shared.generated.resources.Res

enum class Rota {
    LOGIN,
    HOME
}

@Composable
@Preview
fun App() {
    var telaAtual by remember { mutableStateOf(Rota.LOGIN) }

    when (telaAtual){
        Rota.LOGIN -> {
            LoginScreen(
                onLoginSuccess = {
                    telaAtual = Rota.HOME
                }
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