package ceub.weaver.data.local

import ceub.weaver.domain.model.AuthToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties

class TokenStorage(
    private val storageDir: String = "${System.getProperty("user.home")}/.weaver"
) {
    private val tokenFile: File get() = File(storageDir, "auth.properties")

    fun save(token: AuthToken) {
        val dir = File(storageDir)
        if (!dir.exists()) {
            dir.mkdirs()
            dir.setReadable(true, true)
            dir.setWritable(true, true)
        }

        val props = Properties()
        props.setProperty("accessToken", token.accessToken)
        props.setProperty("idToken", token.idToken ?: "")
        props.setProperty("refreshToken", token.refreshToken ?: "")
        props.setProperty("expiresIn", token.expiresIn.toString())
        props.setProperty("scope", token.scope)
        props.setProperty("tokenType", token.tokenType)
        props.setProperty("acquiredAt", token.acquiredAt.toString())

        if (!tokenFile.exists()) {
            tokenFile.createNewFile()
        }
        tokenFile.setReadable(true, true)
        tokenFile.setWritable(true, true)

        FileWriter(tokenFile).use { props.store(it, "Weaver Auth Token") }
    }

    fun load(): AuthToken? {
        if (!tokenFile.exists()) return null

        val props = Properties()
        FileReader(tokenFile).use { props.load(it) }

        val accessToken = props.getProperty("accessToken") ?: return null
        val idToken = props.getProperty("idToken")
        val refreshToken = props.getProperty("refreshToken")
        val expiresIn = props.getProperty("expiresIn")?.toLongOrNull() ?: return null
        val scope = props.getProperty("scope") ?: return null
        val tokenType = props.getProperty("tokenType") ?: return null
        val acquiredAt = props.getProperty("acquiredAt")?.toLongOrNull() ?: return null

        return AuthToken(
            accessToken = accessToken,
            idToken = idToken?.ifEmpty { null },
            refreshToken = refreshToken?.ifEmpty { null },
            expiresIn = expiresIn,
            scope = scope,
            tokenType = tokenType,
            acquiredAt = acquiredAt
        )
    }

    fun clear() {
        if (tokenFile.exists()) {
            tokenFile.delete()
        }
    }
}