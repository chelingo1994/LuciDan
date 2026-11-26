package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio

@Dao
interface NegocioDao {

    @Insert
    suspend fun insertar(negocio: Negocio): Long

    @Update
    suspend fun actualizar(negocio: Negocio)

    /** Upsert (UPDATE, no REPLACE) para no disparar el CASCADE sobre contrataciones y estadísticas. */
    @Upsert
    suspend fun upsertar(negocios: List<Negocio>)

    @Query("SELECT * FROM negocios WHERE duenoId = :duenoId ORDER BY id")
    suspend fun obtenerPorDueno(duenoId: String): List<Negocio>

    @Query("DELETE FROM negocios WHERE id IN (:ids)")
    suspend fun eliminarPorIds(ids: List<Long>)

    @Query("SELECT * FROM negocios WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Negocio?

    /** HU01/HU02: negocios visibles en el mapa (solo PUBLICADO, sección 2.2 de CLAUDE.md). */
    @Query("SELECT * FROM negocios WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: EstadoNegocio): List<Negocio>

    @Query("UPDATE negocios SET promedioCalificacion = :promedio WHERE id = :negocioId")
    suspend fun actualizarPromedio(negocioId: Long, promedio: Double)

    /** DS09: prioridad en los resultados mientras el plan de publicidad esté activo (HU10). */
    @Query("UPDATE negocios SET destacado = :destacado, planNivel = :nivel, destacadoHasta = :hasta WHERE id = :negocioId")
    suspend fun actualizarPlan(negocioId: Long, destacado: Boolean, nivel: Int, hasta: Long)

    /** HU14: sello «Negocio verificado». */
    @Query("UPDATE negocios SET verificado = :verificado WHERE id = :negocioId")
    suspend fun actualizarVerificado(negocioId: Long, verificado: Boolean)

    /** DS11 (HU14): panel de administración. */
    @Query("SELECT * FROM negocios")
    suspend fun obtenerTodos(): List<Negocio>

    @Query("UPDATE negocios SET estado = :estado WHERE id = :negocioId")
    suspend fun actualizarEstado(negocioId: Long, estado: EstadoNegocio)

    @Query("DELETE FROM negocios WHERE id = :negocioId")
    suspend fun eliminar(negocioId: Long)
}
