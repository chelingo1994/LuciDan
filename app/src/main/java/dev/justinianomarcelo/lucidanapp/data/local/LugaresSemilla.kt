package dev.justinianomarcelo.lucidanapp.data.local

import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.Lugar

/**
 * Datos de demostración para poblar el mapa la primera vez que se crea la base
 * de datos local. La fuente editable es docs/lugares_bermejo.txt; el orden
 * de la lista define el id de cada lugar (1, 2, 3...), así que los cambios
 * posteriores se sincronizan por posición (ver AppDatabase). Se reemplazarán por datos reales cuando el backend
 * (PostgreSQL + PostGIS) esté disponible — ver CLAUDE.md, sección 8.
 */
object LugaresSemilla {
    val lista = listOf(
        Lugar(
            nombre = "Plaza Principal Narciso Campero",
            categoria = CategoriaLugar.TURISMO,
            latitud = -22.73264,
            longitud = -64.34273,
            descripcion = "Plaza central de Bermejo, punto de encuentro y referencia de la ciudad.",
            horario = "Todo el día",
            rangoPrecio = "Gratis",
            fotos = emptyList(),
            destacado = true
        ),
        Lugar(
            nombre = "Costanera del Río Bermejo",
            categoria = CategoriaLugar.TURISMO,
            latitud = -22.7395,
            longitud = -64.3450,
            descripcion = "Paseo junto al río, ideal para caminar y ver el atardecer.",
            horario = "06:00 - 22:00",
            rangoPrecio = "Gratis",
            fotos = emptyList(),
            destacado = true
        ),
        Lugar(
            nombre = "Mercado Central de Bermejo",
            categoria = CategoriaLugar.GASTRONOMIA,
            latitud = -22.73206,
            longitud = -64.33724,
            descripcion = "Puestos de comida típica y productos frescos de la región.",
            horario = "07:00 - 18:00",
            rangoPrecio = "Bs 10 - Bs 40",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Paso de Frontera Aguas Blancas",
            categoria = CategoriaLugar.TURISMO,
            latitud = -22.732424491168242,
            longitud = -64.34949874883714,
            descripcion = "Paso fronterizo entre Bolivia y Argentina sobre el río Bermejo.",
            horario = "06:00 - 22:00",
            rangoPrecio = "Gratis",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Parque Los Chaguancos",
            categoria = CategoriaLugar.ACTIVIDADES,
            latitud = -22.72723391223682,
            longitud = -64.3552709398531,
            descripcion = "Espacio verde con senderos y áreas de recreación familiar.",
            horario = "08:00 - 18:00",
            rangoPrecio = "Bs 5",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Restaurante El Rancho Bermejeño",
            categoria = CategoriaLugar.GASTRONOMIA,
            latitud = -22.7320,
            longitud = -64.3440,
            descripcion = "Comida típica tarijeña y parrillas.",
            horario = "11:00 - 23:00",
            rangoPrecio = "Bs 30 - Bs 80",
            fotos = listOf("logo_restaurante")
        ),
        Lugar(
            nombre = "Hotel Cristal",
            categoria = CategoriaLugar.ALOJAMIENTO,
            latitud = -22.730828009474468,
            longitud = -64.34321299708054,
            descripcion = "Hospedaje céntrico",
            horario = "Recepción 24 h",
            rangoPrecio = "Bs 150 - Bs 300",
            fotos = listOf("logo_hotel")
        ),
        Lugar(
            nombre = "Feria Dominical de Bermejo",
            categoria = CategoriaLugar.EVENTOS,
            latitud = -22.73644879222158,
            longitud = -64.33540410673646,
            descripcion = "Feria semanal de artesanías y productos locales.",
            horario = "Domingos 08:00 - 14:00",
            rangoPrecio = "Gratis",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Cancha Polideportivo Municipal",
            categoria = CategoriaLugar.ACTIVIDADES,
            latitud = -22.721832388182875,
            longitud = -64.34741626525599,
            descripcion = "Complejo deportivo municipal para fútbol y básquet.",
            horario = "08:00 - 21:00",
            rangoPrecio = "Bs 20 por hora",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Cabañas Sol de Bermejo",
            categoria = CategoriaLugar.ALOJAMIENTO,
            latitud = -22.7420,
            longitud = -64.3510,
            descripcion = "Cabañas familiares a orillas del río, ideales para descanso.",
            horario = "Recepción 24 h",
            rangoPrecio = "Bs 200 - Bs 400",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Facultad de Ciencias Integradas de Bermejo",
            categoria = CategoriaLugar.ACTIVIDADES,
            latitud = -22.725511694935772,
            longitud = -64.32607913770349,
            descripcion = "Sede de la UAJMS en Bermejo, dentro del Campus Universitario.",
            horario = "Lunes a viernes, horario académico",
            rangoPrecio = "Gratis",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "Complejo Polideportivo Bermejo",
            categoria = CategoriaLugar.ACTIVIDADES,
            latitud = -22.735182113622106,
            longitud = -64.3308666721783,
            descripcion = "Complejo deportivo municipal",
            horario = "08:00 - 21:00",
            rangoPrecio = "Bs 20 por hora",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "La Casona del Mirador Restaurante",
            categoria = CategoriaLugar.GASTRONOMIA,
            latitud = -22.73522375043071,
            longitud = -64.34528216727612,
            descripcion = "Restaurante",
            horario = "12:00 - 15:00",
            rangoPrecio = "Bs 20 por hora",
            fotos = emptyList()
        ),
        Lugar(
            nombre = "El Mirador",
            categoria = CategoriaLugar.TURISMO,
            latitud = -22.71875712537423,
            longitud = -64.3440376224104,
            descripcion = "Cristo Mirador",
            horario = "7:00 - 18:00",
            rangoPrecio = "Gratis",
            fotos = emptyList()
        )
    )
}
