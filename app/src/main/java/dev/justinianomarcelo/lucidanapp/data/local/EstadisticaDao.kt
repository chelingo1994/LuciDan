package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.EstadisticaNegocio
import dev.justinianomarcelo.lucidanapp.data.model.TipoEvento
import java.util.Date

@Dao
interface EstadisticaDao {

    @Insert
    suspend fun insertar(evento: EstadisticaNegocio)

    /** DS10: conteo por tipo de evento dentro del período consultado. */
    @Query("SELECT COUNT(*) FROM estadisticas_negocio WHERE negocioId = :negocioId AND tipo = :tipo AND fecha BETWEEN :desde AND :hasta")
    suspend fun contarPorTipo(negocioId: Long, tipo: TipoEvento, desde: Date, hasta: Date): Int
}
