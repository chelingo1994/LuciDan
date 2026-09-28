package dev.justinianomarcelo.lucidanapp.util

/**
 * Zona de cobertura de la app (Bermejo, Tarija, y alrededores inmediatos).
 * Usada por HU03 para validar puntos seleccionados manualmente en el mapa.
 */
object BermejoZona {
    const val LATITUD_CENTRO = -22.7333
    const val LONGITUD_CENTRO = -64.3417

    private const val LAT_MIN = -22.82
    private const val LAT_MAX = -22.64
    private const val LNG_MIN = -64.46
    private const val LNG_MAX = -64.22

    fun estaDentroDeCobertura(lat: Double, lng: Double): Boolean =
        lat in LAT_MIN..LAT_MAX && lng in LNG_MIN..LNG_MAX
}
