package ceub.weaver

import java.awt.Desktop
import java.net.URI
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object GoogleAuth {
    private const val CLIENT_ID = "648742436532-tk04caat6vqhidvt7tf8f0ipmcm3f3qu.apps.googleusercontent.com"
    private const val REDIRECT_URI = "http://localhost:8082/callback"

    fun iniciarLogin(onTokenRecebido: (String) -> Unit) {
        val urlLoginGoogle = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$CLIENT_ID" +
                "&redirect_uri=$REDIRECT_URI" +
                "&response_type=token" +
                "&scope=https://www.googleapis.com/auth/userinfo.email"

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(urlLoginGoogle))
            } else {
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("win") -> Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", urlLoginGoogle))
                    os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", urlLoginGoogle))
                    else -> Runtime.getRuntime().exec(arrayOf("xdg-open", urlLoginGoogle))
                }
            }
        } catch (e: Exception) {
            println("ERRO AO ABRIR O NAVEGADOR: ${e.message}")
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                embeddedServer(Netty, port = 8082) {
                    routing {
                        get("/callback") {
                            call.respondText(
                                "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px; background: #111; color: #fff;'>" +
                                        "<h2>Login efetuado com sucesso!</h2>" +
                                        "<p>Pode fechar esta aba e voltar ao Weaver Studio.</p>" +
                                        "</body></html>",
                                io.ktor.http.ContentType.Text.Html
                            )

                            onTokenRecebido("token_sucesso_google")
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                println("Servidor Ktor já inicializado ou porta ocupada: ${e.message}")
            }
        }
    }
}