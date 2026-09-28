package dev.justinianomarcelo.lucidanapp.data.local

import androidx.room.TypeConverter
import dev.justinianomarcelo.lucidanapp.data.model.CategoriaLugar
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.RolUsuario
import java.util.Date

class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromRolUsuario(rol: RolUsuario): String = rol.name

    @TypeConverter
    fun toRolUsuario(valor: String): RolUsuario = RolUsuario.valueOf(valor)

    @TypeConverter
    fun fromCategoriaLugar(categoria: CategoriaLugar): String = categoria.name

    @TypeConverter
    fun toCategoriaLugar(valor: String): CategoriaLugar = CategoriaLugar.valueOf(valor)

    @TypeConverter
    fun fromEstadoNegocio(estado: EstadoNegocio): String = estado.name

    @TypeConverter
    fun toEstadoNegocio(valor: String): EstadoNegocio = EstadoNegocio.valueOf(valor)

    @TypeConverter
    fun fromFotos(fotos: List<String>): String = fotos.joinToString("|")

    @TypeConverter
    fun toFotos(valor: String): List<String> = if (valor.isBlank()) emptyList() else valor.split("|")
}
