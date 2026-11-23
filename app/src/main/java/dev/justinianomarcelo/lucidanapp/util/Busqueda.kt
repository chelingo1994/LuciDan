package dev.justinianomarcelo.lucidanapp.util

import java.text.Normalizer

/** Búsqueda de texto del mapa: sin distinguir mayúsculas ni tildes ("cafe" encuentra "Café"). */
object Busqueda {
    private val MARCAS_DE_ACENTO = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val ESPACIOS = Regex("\\s+")

    fun normalizar(texto: String): String =
        MARCAS_DE_ACENTO.replace(Normalizer.normalize(texto, Normalizer.Form.NFD), "").lowercase().trim()

    /** true si cada palabra de [consulta] aparece en alguno de los [campos]; una consulta vacía coincide con todo. */
    fun coincide(consulta: String, vararg campos: String): Boolean {
        val palabras = normalizar(consulta).split(ESPACIOS).filter { it.isNotEmpty() }
        if (palabras.isEmpty()) return true
        val texto = normalizar(campos.joinToString(" "))
        return palabras.all { it in texto }
    }
}
