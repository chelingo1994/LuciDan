package dev.justinianomarcelo.lucidanapp.util

/** Tipo de ícono para una condición del clima; la pantalla lo traduce a un ícono de Material. */
enum class IconoClima { SOL, PARCIAL, NUBLADO, NIEBLA, LLUVIA, TORMENTA, NIEVE, DESCONOCIDO }

data class CondicionClima(val descripcion: String, val icono: IconoClima)

/** Traduce el `weather_code` de Open-Meteo (códigos WMO) a una descripción en español y un ícono. */
object ClimaCodigos {
    fun deCodigo(codigo: Int?): CondicionClima = when (codigo) {
        0 -> CondicionClima("Despejado", IconoClima.SOL)
        1 -> CondicionClima("Mayormente despejado", IconoClima.SOL)
        2 -> CondicionClima("Parcialmente nublado", IconoClima.PARCIAL)
        3 -> CondicionClima("Nublado", IconoClima.NUBLADO)
        45, 48 -> CondicionClima("Niebla", IconoClima.NIEBLA)
        51 -> CondicionClima("Llovizna ligera", IconoClima.LLUVIA)
        53 -> CondicionClima("Llovizna", IconoClima.LLUVIA)
        55 -> CondicionClima("Llovizna intensa", IconoClima.LLUVIA)
        56, 57 -> CondicionClima("Llovizna helada", IconoClima.LLUVIA)
        61 -> CondicionClima("Lluvia ligera", IconoClima.LLUVIA)
        63 -> CondicionClima("Lluvia", IconoClima.LLUVIA)
        65 -> CondicionClima("Lluvia fuerte", IconoClima.LLUVIA)
        66, 67 -> CondicionClima("Lluvia helada", IconoClima.LLUVIA)
        71, 73, 75, 77 -> CondicionClima("Nevada", IconoClima.NIEVE)
        80 -> CondicionClima("Chubascos ligeros", IconoClima.LLUVIA)
        81 -> CondicionClima("Chubascos", IconoClima.LLUVIA)
        82 -> CondicionClima("Chubascos fuertes", IconoClima.LLUVIA)
        85, 86 -> CondicionClima("Chubascos de nieve", IconoClima.NIEVE)
        95 -> CondicionClima("Tormenta eléctrica", IconoClima.TORMENTA)
        96, 99 -> CondicionClima("Tormenta con granizo", IconoClima.TORMENTA)
        else -> CondicionClima("Condición desconocida", IconoClima.DESCONOCIDO)
    }
}
