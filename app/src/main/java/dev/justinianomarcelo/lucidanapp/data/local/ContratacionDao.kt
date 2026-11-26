package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import java.util.Date

data class ContratacionConNivel(
    val negocioId: Long,
    val nivel: Int,
    val duracionDias: Int,
    val fechaVencimiento: Long
)

@Dao
interface ContratacionDao {

    @Insert
    suspend fun insertar(contratacion: ContratacionPublicidad): Long

    @Query("SELECT * FROM contrataciones_publicidad WHERE negocioId = :negocioId AND activo = 1 LIMIT 1")
    suspend fun obtenerActivaPorNegocio(negocioId: Long): ContratacionPublicidad?

    @Query("SELECT * FROM contrataciones_publicidad WHERE negocioId = :negocioId ORDER BY fechaInicio DESC")
    suspend fun obtenerHistorialPorNegocio(negocioId: Long): List<ContratacionPublicidad>

    @Query("SELECT * FROM contrataciones_publicidad WHERE activo = 1 AND fechaVencimiento < :ahora")
    suspend fun obtenerVencidas(ahora: Date): List<ContratacionPublicidad>

    /** Contrataciones activas con el nivel de su plan, para calcular el plan efectivo de cada negocio. */
    @Query(
        "SELECT c.negocioId AS negocioId, p.nivel AS nivel, p.duracionDias AS duracionDias, " +
            "c.fechaVencimiento AS fechaVencimiento FROM contrataciones_publicidad c " +
            "JOIN planes_publicidad p ON p.id = c.planId WHERE c.activo = 1"
    )
    suspend fun obtenerActivasConNivel(): List<ContratacionConNivel>

    @Query("UPDATE contrataciones_publicidad SET activo = 0 WHERE id = :contratacionId")
    suspend fun desactivar(contratacionId: Long)
}
