package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import java.util.Date

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

    @Query("UPDATE contrataciones_publicidad SET activo = 0 WHERE id = :contratacionId")
    suspend fun desactivar(contratacionId: Long)
}
