package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.justinianomarcelo.lucidanapp.data.model.EstadisticaNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.TipoEvento
import java.util.Date

@Dao
interface EstadisticaDao {

    @Insert
    suspend fun insertar(evento: EstadisticaNegocio)

    /** DS10: conteo por tipo de evento dentro del período consultado. */
    @Query("SELECT COUNT(*) FROM estadisticas_negocio WHERE negocioId = :negocioId AND tipo = :tipo AND fecha BETWEEN :desde AND :hasta")
    suspend fun contarPorTipo(negocioId: Long, tipo: TipoEvento, desde: Date, hasta: Date): Int

    /** Comparación con el rubro: eventos de los OTROS negocios publicados de la misma categoría. */
    @Query(
        "SELECT COUNT(*) FROM estadisticas_negocio e JOIN negocios n ON n.id = e.negocioId " +
            "WHERE n.categoria = :categoria AND n.estado = 'PUBLICADO' AND n.id != :negocioId " +
            "AND e.tipo = :tipo AND e.fecha BETWEEN :desde AND :hasta"
    )
    suspend fun contarDeOtrosDelRubro(negocioId: Long, categoria: Rubro, tipo: TipoEvento, desde: Date, hasta: Date): Int

    @Query("SELECT COUNT(*) FROM negocios WHERE categoria = :categoria AND estado = 'PUBLICADO' AND id != :negocioId")
    suspend fun contarOtrosDelRubro(negocioId: Long, categoria: Rubro): Int
}
