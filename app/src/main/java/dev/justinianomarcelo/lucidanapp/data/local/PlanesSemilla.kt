package dev.justinianomarcelo.lucidanapp.data.local

import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

/** Catálogo de planes de publicidad disponibles (TA-10-02), precios en Bs. */
object PlanesSemilla {
    val lista = listOf(
        PlanPublicidad(
            nombre = "Plan Semanal",
            duracionDias = 7,
            precio = 30.0,
            beneficios = "Tu negocio aparece destacado en los resultados cercanos durante 7 días."
        ),
        PlanPublicidad(
            nombre = "Plan Mensual",
            duracionDias = 30,
            precio = 100.0,
            beneficios = "Tu negocio aparece destacado en los resultados cercanos durante 30 días."
        ),
        PlanPublicidad(
            nombre = "Plan Trimestral",
            duracionDias = 90,
            precio = 250.0,
            beneficios = "Tu negocio aparece destacado en los resultados cercanos durante 90 días, el de mejor precio por día."
        )
    )
}
