package dev.justinianomarcelo.lucidanapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Contacto directo con un negocio: WhatsApp si el número es celular y la app está instalada; si no, el marcador. */
object Contacto {
    private const val CODIGO_BOLIVIA = "591"
    private val APPS_WHATSAPP = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** Solo dígitos, tal como se marca. Vacío si no hay número. */
    fun soloDigitos(telefono: String): String = telefono.filter { it.isDigit() }

    /**
     * Número en formato internacional para WhatsApp (591XXXXXXXX), o null si no es un celular boliviano:
     * los celulares tienen 8 dígitos y empiezan con 6 o 7; un fijo no tiene WhatsApp.
     */
    fun numeroWhatsapp(telefono: String): String? {
        var digitos = soloDigitos(telefono)
        if (digitos.startsWith(CODIGO_BOLIVIA) && digitos.length == 11) digitos = digitos.removePrefix(CODIGO_BOLIVIA)
        return if (digitos.length == 8 && (digitos[0] == '6' || digitos[0] == '7')) CODIGO_BOLIVIA + digitos else null
    }

    fun esCelular(telefono: String) = numeroWhatsapp(telefono) != null

    /** Abre el chat de WhatsApp con el negocio; si no hay WhatsApp (o no es celular) abre el marcador. */
    fun contactar(context: Context, telefono: String) {
        val numero = numeroWhatsapp(telefono)
        if (numero != null) {
            for (paquete in APPS_WHATSAPP) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero")).setPackage(paquete)
                try {
                    context.startActivity(intent)
                    return
                } catch (_: ActivityNotFoundException) {
                    // Esa variante de WhatsApp no está instalada: se prueba la siguiente.
                }
            }
        }
        llamar(context, telefono)
    }

    fun llamar(context: Context, telefono: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${soloDigitos(telefono)}")))
        } catch (_: ActivityNotFoundException) {
            // Dispositivo sin marcador (tablet): no hay nada más que ofrecer.
        }
    }
}
