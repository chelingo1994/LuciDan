package dev.justinianomarcelo.lucidanapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

/** Contacto directo con un negocio: WhatsApp si el número es celular y la app está instalada; si no, el marcador. */
object Contacto {
    private const val CODIGO_BOLIVIA = "591"
    const val MENSAJE_WHATSAPP = "Hola, vi su negocio en LuciDan"
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

    /** Enlace del chat con el mensaje ya escrito, o null si el número no tiene WhatsApp. */
    fun enlaceWhatsapp(telefono: String): String? = numeroWhatsapp(telefono)?.let {
        "https://wa.me/$it?text=" + URLEncoder.encode(MENSAJE_WHATSAPP, "UTF-8").replace("+", "%20")
    }

    /** Abre el chat de WhatsApp con el negocio; si no hay WhatsApp (o no es celular) abre el marcador. */
    fun contactar(context: Context, telefono: String) {
        val enlace = enlaceWhatsapp(telefono)
        if (enlace != null) {
            for (paquete in APPS_WHATSAPP) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(enlace)).setPackage(paquete)
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

    /** Abre un enlace en la app que lo maneje (red social) o en el navegador. */
    fun abrirEnlace(context: Context, enlace: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(enlace)))
        } catch (_: ActivityNotFoundException) {
            // Sin navegador ni app que abra el enlace: no hay nada más que ofrecer.
        }
    }

    fun llamar(context: Context, telefono: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${soloDigitos(telefono)}")))
        } catch (_: ActivityNotFoundException) {
            // Dispositivo sin marcador (tablet): no hay nada más que ofrecer.
        }
    }
}
