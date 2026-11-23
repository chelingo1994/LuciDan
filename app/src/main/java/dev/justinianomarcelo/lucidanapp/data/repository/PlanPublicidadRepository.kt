package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.PlanPublicidadDao
import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad

class PlanPublicidadRepository(private val planPublicidadDao: PlanPublicidadDao) {

    suspend fun obtenerPlanes(): List<PlanPublicidad> = planPublicidadDao.obtenerTodos()

    suspend fun obtenerPlanPorId(id: Long): PlanPublicidad? = planPublicidadDao.obtenerPorId(id)
}
