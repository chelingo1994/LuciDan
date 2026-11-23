package dev.justinianomarcelo.lucidanapp.auth

import android.util.Base64
import dev.justinianomarcelo.lucidanapp.data.model.Usuario
import dev.justinianomarcelo.lucidanapp.data.repository.UsuarioRepository
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cifra y verifica contraseñas (PBKDF2-HMAC-SHA256) y autentica credenciales
 * contra [UsuarioRepository], según DS06 (S1_DS06_Registro_Login.puml).
 */
class AuthService(private val usuarioRepository: UsuarioRepository) {

    fun cifrarContrasena(contrasena: String): String {
        val salt = ByteArray(SALT_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derivarHash(contrasena, salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    private fun verificarContrasena(contrasena: String, hashAlmacenado: String): Boolean {
        val partes = hashAlmacenado.split(":")
        if (partes.size != 2) return false
        val salt = Base64.decode(partes[0], Base64.NO_WRAP)
        val hashEsperado = Base64.decode(partes[1], Base64.NO_WRAP)
        val hashCalculado = derivarHash(contrasena, salt)
        return hashCalculado.contentEquals(hashEsperado)
    }

    /** TA-14-03: una cuenta suspendida por el administrador no puede iniciar sesión. */
    suspend fun autenticar(correo: String, contrasena: String): Usuario? {
        val usuario = usuarioRepository.buscarPorCorreo(correo) ?: return null
        if (!usuario.activo) return null
        return if (verificarContrasena(contrasena, usuario.contrasenaHash)) usuario else null
    }

    private fun derivarHash(contrasena: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(contrasena.toCharArray(), salt, ITERACIONES, LONGITUD_HASH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private companion object {
        const val SALT_LENGTH_BYTES = 16
        const val ITERACIONES = 120_000
        const val LONGITUD_HASH_BITS = 256
    }
}
