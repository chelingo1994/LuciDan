package dev.justinianomarcelo.lucidanapp.data.model

/** Rubro (categoría) de un negocio. Los nombres coinciden con docs/negocios_bermejo.txt. */
enum class Rubro(val etiqueta: String) {
    GASTRONOMIA("Gastronomía"),
    FARMACIA_SALUD("Farmacia y salud"),
    MODA_ROPA("Moda y ropa"),
    CAMBIO_MONEDA("Cambio de moneda"),
    TRANSPORTE("Transporte"),
    ALOJAMIENTO("Alojamiento"),
    ABARROTES("Abarrotes"),
    TECNOLOGIA("Tecnología"),
    SERVICIOS("Servicios"),
    OTRO("Otro");

    companion object {
        /** Rubro por nombre del enum; null si no existe. */
        fun deNombre(nombre: String?): Rubro? = entries.firstOrNull { it.name == nombre?.trim() }
    }
}
