package dev.justinianomarcelo.lucidanapp.data.local

import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad
import dev.justinianomarcelo.lucidanapp.util.BeneficiosPlan

/** Catálogo de planes de publicidad disponibles (TA-10-02), precios en Bs. */
object PlanesSemilla {
    val lista = listOf(
        plan("Plan Semanal", 7, 30.0, BeneficiosPlan.SEMANAL),
        plan("Plan Mensual", 30, 100.0, BeneficiosPlan.MENSUAL),
        plan("Plan Trimestral", 90, 250.0, BeneficiosPlan.TRIMESTRAL)
    )

    private fun plan(nombre: String, dias: Int, precio: Double, nivel: Int) = PlanPublicidad(
        nombre = nombre,
        duracionDias = dias,
        precio = precio,
        beneficios = BeneficiosPlan.textos(nivel).joinToString("\n"),
        nivel = nivel
    )
}
