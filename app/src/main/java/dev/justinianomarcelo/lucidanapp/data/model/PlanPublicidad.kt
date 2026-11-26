package dev.justinianomarcelo.lucidanapp.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planes_publicidad")
data class PlanPublicidad(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val duracionDias: Int,
    val precio: Double,
    /** Una línea por beneficio. */
    val beneficios: String,
    @ColumnInfo(defaultValue = "0")
    val nivel: Int = 0
)
