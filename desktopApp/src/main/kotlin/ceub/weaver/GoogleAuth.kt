package ceub.weaver

import ceub.weaver.domain.usecase.GoogleLoginUseCase
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

const val GOOGLE_CLIENT_ID = "648742436532-tk04caat6vqhidvt7tf8f0ipmcm3f3qu.apps.googleusercontent.com"
const val GOOGLE_REDIRECT_URI = "http://localhost:8082/callback"

object GoogleAuth {
    fun iniciarLogin(
        useCase: GoogleLoginUseCase,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier)

        val url = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$GOOGLE_CLIENT_ID" +
                "&redirect_uri=$GOOGLE_REDIRECT_URI" +
                "&response_type=code" +
                "&scope=https://www.googleapis.com/auth/userinfo.email" +
                "&code_challenge=$codeChallenge" +
                "&code_challenge_method=S256"

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                val os = System.getProperty("os.name").lowercase()
                when {
                    os.contains("win") -> Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                    os.contains("mac") -> Runtime.getRuntime().exec(arrayOf("open", url))
                    else -> Runtime.getRuntime().exec(arrayOf("xdg-open", url))
                }
            }
        } catch (e: Exception) {
            onError(e)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                lateinit var server: EmbeddedServer<*, *>
                server = embeddedServer(Netty, port = 8082) {
                    routing {
                        get("/callback") {
                            val code = call.request.queryParameters["code"]
                            if (code != null) {
                                val result = useCase(code, codeVerifier)
                                result
                                    .onSuccess {
                                        call.respondText(
                                            "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px; background: #111; color: #fff;'>" +
                                                    "<h2>Login efetuado com sucesso!</h2>" +
                                                    "<p>Pode fechar esta aba e voltar ao Weaver Studio.</p>" +
                                                    "</body></html>",
                                            io.ktor.http.ContentType.Text.Html
                                        )
                                        onSuccess()
                                    }
                                    .onFailure { error ->
                                        call.respondText(
                                            "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px; background: #111; color: #fff;'>" +
                                                    "<h2>Erro no login</h2>" +
                                                    "<p>${error.message}</p>" +
                                                    "<p>Tente novamente.</p>" +
                                                    "</body></html>",
                                            io.ktor.http.ContentType.Text.Html
                                        )
                                        onError(error)
                                    }
                            } else {
                                call.respondText(
                                    "<html><body style='font-family: sans-serif; text-align: center; padding-top: 50px; background: #111; color: #fff;'>" +
                                            "<h2>Erro: Código de autorização não encontrado.</h2>" +
                                            "<p>Não foi possível autenticar. Tente novamente.</p>" +
                                            "</body></html>",
                                    io.ktor.http.ContentType.Text.Html
                                )
                                onError(Exception("Authorization code not found in callback"))
                            }
                            server.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
                        }
                    }
                }
                server.start(wait = false)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun generateCodeVerifier(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(verifier.toByteArray())
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash)
    }
}
