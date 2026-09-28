package dev.justinianomarcelo.lucidanapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Lugar
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.data.model.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Usuario::class, Lugar::class, Resena::class, Favorito::class, Negocio::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun lugarDao(): LugarDao
    abstract fun resenaDao(): ResenaDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun negocioDao(): NegocioDao

    companion object {
        /**
         * Sincroniza la tabla `lugares` con [LugaresSemilla] por posición: el lugar n de la
         * lista es el id n. Actualiza los existentes (conserva reseñas, favoritos y
         * calificación) e inserta los que faltan. No borra lugares sobrantes.
         */
        private fun sincronizarLugaresSemilla(db: SupportSQLiteDatabase) {
            val converters = Converters()
            LugaresSemilla.lista.forEachIndexed { indice, lugar ->
                val id = indice + 1L
                val existe = db.query("SELECT COUNT(*) FROM lugares WHERE id = ?", arrayOf<Any>(id)).use {
                    it.moveToFirst() && it.getInt(0) > 0
                }
                val args = arrayOf<Any>(
                    lugar.nombre, converters.fromCategoriaLugar(lugar.categoria), lugar.latitud,
                    lugar.longitud, lugar.descripcion, lugar.horario, lugar.rangoPrecio,
                    converters.fromFotos(lugar.fotos), if (lugar.destacado) 1 else 0, id
                )
                if (existe) {
                    db.execSQL(
                        "UPDATE lugares SET nombre = ?, categoria = ?, latitud = ?, longitud = ?, " +
                            "descripcion = ?, horario = ?, rangoPrecio = ?, fotos = ?, destacado = ? WHERE id = ?",
                        args
                    )
                } else {
                    db.execSQL(
                        "INSERT INTO lugares (nombre, categoria, latitud, longitud, descripcion, horario, " +
                            "rangoPrecio, fotos, destacado, id, promedioCalificacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.0)",
                        args
                    )
                }
            }
        }

        /** Versión 2: lugares corregidos y ampliados desde docs/lugares_bermejo.txt. */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = sincronizarLugaresSemilla(db)
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lucidan.db"
                ).addMigrations(MIGRACION_1_2).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            instancia?.lugarDao()?.insertarTodos(LugaresSemilla.lista)
                        }
                    }
                }).build().also { instancia = it }
            }
    }
}
