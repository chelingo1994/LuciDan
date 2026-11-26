package dev.justinianomarcelo.lucidanapp.util

import dev.justinianomarcelo.lucidanapp.data.model.Negocio

enum class RedSocial(
    val etiqueta: String,
    private val dominios: List<String>,
    private val base: String,
    private val usuarioValido: Regex
) {
    FACEBOOK("Facebook", listOf("facebook.com", "fb.com"), "https://www.facebook.com/", Regex("[A-Za-z0-9.\\-]{2,80}")),
    INSTAGRAM("Instagram", listOf("instagram.com"), "https://www.instagram.com/", Regex("[A-Za-z0-9._]{1,30}")),
    TIKTOK("TikTok", listOf("tiktok.com"), "https://www.tiktok.com/@", Regex("[A-Za-z0-9._]{2,24}"));

    /**
     * Acepta un usuario («@mitienda») o un enlace de esta red y devuelve el enlace https normalizado.
     * "" si el campo está vacío; null si no es válido.
     */
    fun normalizar(texto: String): String? {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return ""
        val sinEsquema = limpio.replaceFirst(Regex("^https?://", RegexOption.IGNORE_CASE), "")
        val host = sinEsquema.substringBefore('/').substringBefore('?').lowercase()
        val esEnlace = dominios.any { host == it || host.endsWith(".$it") }
        val usuario = if (esEnlace) {
            val ruta = sinEsquema.substringAfter('/', "")
            // Páginas de Facebook sin nombre de usuario: profile.php?id=123
            if (this == FACEBOOK) {
                Regex("^profile\\.php\\?id=(\\d+)").find(ruta)?.let { return "${base}profile.php?id=${it.groupValues[1]}" }
            }
            ruta.substringBefore('?').substringBefore('#').trim('/').substringBefore('/')
        } else {
            // Un enlace de otro sitio no es un usuario.
            if (limpio.contains('/') || limpio.contains(':') || esDeOtraRed(limpio)) return null
            limpio
        }.removePrefix("@")
        return if (usuarioValido.matches(usuario) && usuario.any { it.isLetterOrDigit() }) base + usuario else null
    }

    private fun esDeOtraRed(texto: String) =
        entries.any { red -> red.dominios.any { texto.lowercase().endsWith(it) } }
}

/** Enlaces que la ficha debe mostrar: solo con un plan vigente que incluya redes sociales. */
fun Negocio.redesVisibles(ahora: Long = System.currentTimeMillis()): List<Pair<RedSocial, String>> {
    if (!Destacados.beneficios(this, ahora).redesSociales) return emptyList()
    return listOf(RedSocial.FACEBOOK to facebook, RedSocial.INSTAGRAM to instagram, RedSocial.TIKTOK to tiktok)
        // Lo que llega de la nube no se abre tal cual: solo si es un enlace válido de su red.
        .mapNotNull { (red, valor) -> red.normalizar(valor)?.takeIf { it.isNotEmpty() }?.let { red to it } }
}
