package dev.justinianomarcelo.lucidanapp.data.local

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.justinianomarcelo.lucidanapp.data.model.ContratacionPublicidad
import dev.justinianomarcelo.lucidanapp.data.model.EstadisticaNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Favorito
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.model.PlanPublicidad
import dev.justinianomarcelo.lucidanapp.data.model.Resena
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.Usuario
import dev.justinianomarcelo.lucidanapp.util.FotoNegocio
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import java.security.SecureRandom
import kotlin.random.Random
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Database(
    entities = [
        Usuario::class, Resena::class, Favorito::class, Negocio::class,
        PlanPublicidad::class, ContratacionPublicidad::class, EstadisticaNegocio::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun resenaDao(): ResenaDao
    abstract fun favoritoDao(): FavoritoDao
    abstract fun negocioDao(): NegocioDao
    abstract fun planPublicidadDao(): PlanPublicidadDao
    abstract fun contratacionDao(): ContratacionDao
    abstract fun estadisticaDao(): EstadisticaDao

    companion object {
        /** Versión 2: corregía los lugares turísticos. La tabla `lugares` ya no existe (se borra en la versión 10). */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) = Unit
        }

        /** Versión 3: sección 2.2 de CLAUDE.md — el negocio suma qué vende, contacto y ubicación geográfica. */
        private val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE negocios ADD COLUMN descripcion TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE negocios ADD COLUMN telefono TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE negocios ADD COLUMN latitud REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE negocios ADD COLUMN longitud REAL NOT NULL DEFAULT 0.0")
            }
        }

        private fun insertarPlanesSemilla(db: SupportSQLiteDatabase) {
            PlanesSemilla.lista.forEach { plan ->
                db.execSQL(
                    "INSERT INTO planes_publicidad (nombre, duracionDias, precio, beneficios) VALUES (?, ?, ?, ?)",
                    arrayOf<Any>(plan.nombre, plan.duracionDias, plan.precio, plan.beneficios)
                )
            }
        }

        /** Versión 4: HU10 — planes de publicidad y contrataciones (DS09). */
        private val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `planes_publicidad` (" +
                        "`id` INTEGER NOT NULL, `nombre` TEXT NOT NULL, `duracionDias` INTEGER NOT NULL, " +
                        "`precio` REAL NOT NULL, `beneficios` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `contrataciones_publicidad` (" +
                        "`id` INTEGER NOT NULL, `negocioId` INTEGER NOT NULL, `planId` INTEGER NOT NULL, " +
                        "`fechaInicio` INTEGER NOT NULL, `fechaVencimiento` INTEGER NOT NULL, `activo` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`planId`) REFERENCES `planes_publicidad`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contrataciones_publicidad_negocioId` ON `contrataciones_publicidad` (`negocioId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contrataciones_publicidad_planId` ON `contrataciones_publicidad` (`planId`)")
                insertarPlanesSemilla(db)
            }
        }

        /** Versión 5: HU11 — estadísticas de vistas y clics del negocio (DS10). */
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `estadisticas_negocio` (" +
                        "`id` INTEGER NOT NULL, `negocioId` INTEGER NOT NULL, `tipo` TEXT NOT NULL, " +
                        "`fecha` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_estadisticas_negocio_negocioId` ON `estadisticas_negocio` (`negocioId`)")
            }
        }

        /**
         * Hash PBKDF2 idéntico al de [dev.justinianomarcelo.lucidanapp.auth.AuthService], duplicado
         * aquí porque la cuenta administradora semilla (TA-14) se crea antes de que exista una
         * instancia de Room sobre la que construir un UsuarioRepository.
         */
        private fun hashContrasenaSemilla(contrasena: String): String {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val spec = PBEKeySpec(contrasena.toCharArray(), salt, 120_000, 256)
            val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(hash, Base64.NO_WRAP)
        }

        /**
         * Versión 6: HU14 — rol de administrador (TA-14-02/03). Crea la cuenta admin semilla
         * admin@lucidan.bo / Admin123! para poder entrar al panel sin un flujo de alta aparte.
         */
        private val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    "INSERT INTO usuarios (correo, contrasenaHash, fechaRegistro, rol, notificacionesActivas, activo) " +
                        "SELECT ?, ?, ?, ?, 1, 1 WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = ?)",
                    arrayOf<Any>(
                        "admin@lucidan.bo", hashContrasenaSemilla("Admin123!"), System.currentTimeMillis(),
                        "ADMINISTRADOR", "admin@lucidan.bo"
                    )
                )
            }
        }

        /**
         * Versión 7: los negocios pasan a vivir en Firestore y Room solo los cachea. Se reconstruye la
         * tabla sin la FK ni el índice único sobre `usuarioId` (los negocios de otros teléfonos no
         * tienen un usuario local) y se agrega `duenoId`, el hash del correo del dueño.
         */
        private val MIGRACION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val duenos = mutableMapOf<Long, String>()
                db.query("SELECT n.id, u.correo FROM negocios n JOIN usuarios u ON u.id = n.usuarioId").use {
                    while (it.moveToNext()) duenos[it.getLong(0)] = IdDueno.deCorreo(it.getString(1))
                }
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `negocios_nueva` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, " +
                        "`nombre` TEXT NOT NULL, `categoria` TEXT NOT NULL, `descripcion` TEXT NOT NULL, " +
                        "`telefono` TEXT NOT NULL, `ubicacion` TEXT NOT NULL, `latitud` REAL NOT NULL, " +
                        "`longitud` REAL NOT NULL, `horario` TEXT NOT NULL, `fotos` TEXT NOT NULL, " +
                        "`estado` TEXT NOT NULL, `destacado` INTEGER NOT NULL, `duenoId` TEXT NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO negocios_nueva (id, usuarioId, nombre, categoria, descripcion, telefono, " +
                        "ubicacion, latitud, longitud, horario, fotos, estado, destacado, duenoId) " +
                        "SELECT id, usuarioId, nombre, categoria, descripcion, telefono, ubicacion, latitud, " +
                        "longitud, horario, fotos, estado, destacado, '' FROM negocios"
                )
                db.execSQL("DROP TABLE negocios")
                db.execSQL("ALTER TABLE negocios_nueva RENAME TO negocios")
                duenos.forEach { (id, duenoId) ->
                    db.execSQL("UPDATE negocios SET duenoId = ? WHERE id = ?", arrayOf<Any>(duenoId, id))
                }
            }
        }

        /**
         * Versión 8: índice único sobre `duenoId` (cuando el diagrama decía 0..1); la versión 9 lo vuelve
         * a quitar. Antes de crearlo se limpiaron los negocios sin dueño o repetidos y lo que colgaba de ellos.
         */
        private val MIGRACION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM negocios WHERE duenoId = '' OR id NOT IN (SELECT MIN(id) FROM negocios GROUP BY duenoId)")
                db.execSQL("DELETE FROM contrataciones_publicidad WHERE negocioId NOT IN (SELECT id FROM negocios)")
                db.execSQL("DELETE FROM estadisticas_negocio WHERE negocioId NOT IN (SELECT id FROM negocios)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_negocios_duenoId` ON `negocios` (`duenoId`)")
            }
        }

        /**
         * Versión 9: el diagrama de clases pasó a Usuario 1 — 0..* Negocio (un dueño puede tener varios
         * negocios), así que el índice sobre `duenoId` deja de ser único. No se pierde ningún dato.
         */
        private val MIGRACION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS `index_negocios_duenoId`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_negocios_duenoId` ON `negocios` (`duenoId`)")
            }
        }

        /** Inserta los negocios de docs/negocios_bermejo.txt sin pisar los que ya tengan ese id. */
        private fun insertarNegociosSemilla(db: SupportSQLiteDatabase, negocios: List<Negocio>) {
            val converters = Converters()
            negocios.forEach { n ->
                db.execSQL(
                    "INSERT OR IGNORE INTO negocios (id, usuarioId, nombre, categoria, descripcion, telefono, " +
                        "direccion, latitud, longitud, horario, fotos, promedioCalificacion, estado, destacado, duenoId) " +
                        "VALUES (?, 0, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0.0, ?, ?, ?)",
                    arrayOf<Any>(
                        n.id, n.nombre, converters.fromRubro(n.categoria), n.descripcion, n.telefono, n.direccion,
                        n.latitud, n.longitud, n.horario, converters.fromFotos(n.fotos),
                        converters.fromEstadoNegocio(n.estado), if (n.destacado) 1 else 0, n.duenoId
                    )
                )
                // Los negocios ya cargados antes de que existiera la foto la reciben aquí; si ya tienen una
                // (por ejemplo, elegida desde la galería) no se toca.
                if (n.fotos.isNotEmpty()) {
                    db.execSQL(
                        "UPDATE negocios SET fotos = ? WHERE id = ? AND duenoId = ? AND fotos = ''",
                        arrayOf<Any>(converters.fromFotos(n.fotos), n.id, n.duenoId)
                    )
                }
            }
        }

        private fun insertarAdminSemilla(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO usuarios (correo, contrasenaHash, fechaRegistro, rol, notificacionesActivas, activo) " +
                    "SELECT ?, ?, ?, ?, 1, 1 WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = ?)",
                arrayOf<Any>(
                    "admin@lucidan.bo", hashContrasenaSemilla("Admin123!"), System.currentTimeMillis(),
                    "ADMINISTRADOR", "admin@lucidan.bo"
                )
            )
        }

        private const val SQL_CONTRATACIONES =
            "CREATE TABLE IF NOT EXISTS `contrataciones_publicidad` (" +
                "`id` INTEGER NOT NULL, `negocioId` INTEGER NOT NULL, `planId` INTEGER NOT NULL, " +
                "`fechaInicio` INTEGER NOT NULL, `fechaVencimiento` INTEGER NOT NULL, `activo` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`planId`) REFERENCES `planes_publicidad`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"

        private const val SQL_ESTADISTICAS =
            "CREATE TABLE IF NOT EXISTS `estadisticas_negocio` (" +
                "`id` INTEGER NOT NULL, `negocioId` INTEGER NOT NULL, `tipo` TEXT NOT NULL, " +
                "`fecha` INTEGER NOT NULL, PRIMARY KEY(`id`), " +
                "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"

        /**
         * Versión 10: guía 100% de negocios (sección 2.2 de CLAUDE.md).
         *  - Se elimina la tabla `lugares`. Las reseñas y favoritos que apuntaban a lugares se descartan
         *    (ya no hay a qué asociarlos) y las tablas se recrean apuntando a `negocios` (`negocioId`).
         *  - `negocios` pasa `ubicacion` a `direccion`, suma `promedioCalificacion` y reduce `categoria` a los
         *    nuevos rubros (lo que no coincide queda como OTRO).
         *  - El rol TURISTA pasa a VISITANTE.
         *  - Se cargan los negocios de ejemplo de docs/negocios_bermejo.txt.
         * Contrataciones y estadísticas se respaldan y restauran porque la FK a `negocios` las borraría
         * en cascada al reconstruir la tabla.
         */
        private fun migracion9a10(semilla: List<Negocio>) = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `respaldo_contrataciones` AS SELECT * FROM `contrataciones_publicidad`")
                db.execSQL("CREATE TABLE `respaldo_estadisticas` AS SELECT * FROM `estadisticas_negocio`")
                db.execSQL("DROP TABLE `contrataciones_publicidad`")
                db.execSQL("DROP TABLE `estadisticas_negocio`")
                db.execSQL("DROP TABLE `resenas`")
                db.execSQL("DROP TABLE `favoritos`")
                db.execSQL("DROP TABLE `lugares`")

                db.execSQL(
                    "CREATE TABLE `negocios_nueva` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, " +
                        "`nombre` TEXT NOT NULL, `categoria` TEXT NOT NULL, `descripcion` TEXT NOT NULL, " +
                        "`telefono` TEXT NOT NULL, `direccion` TEXT NOT NULL, `latitud` REAL NOT NULL, " +
                        "`longitud` REAL NOT NULL, `horario` TEXT NOT NULL, `fotos` TEXT NOT NULL, " +
                        "`promedioCalificacion` REAL NOT NULL, `estado` TEXT NOT NULL, " +
                        "`destacado` INTEGER NOT NULL, `duenoId` TEXT NOT NULL)"
                )
                val rubros = Rubro.entries.joinToString(",") { "'${it.name}'" }
                db.execSQL(
                    "INSERT INTO `negocios_nueva` (id, usuarioId, nombre, categoria, descripcion, telefono, " +
                        "direccion, latitud, longitud, horario, fotos, promedioCalificacion, estado, destacado, duenoId) " +
                        "SELECT id, usuarioId, nombre, CASE WHEN categoria IN ($rubros) THEN categoria ELSE 'OTRO' END, " +
                        "descripcion, telefono, ubicacion, latitud, longitud, horario, fotos, 0.0, estado, destacado, duenoId " +
                        "FROM `negocios`"
                )
                db.execSQL("DROP TABLE `negocios`")
                db.execSQL("ALTER TABLE `negocios_nueva` RENAME TO `negocios`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_negocios_duenoId` ON `negocios` (`duenoId`)")

                db.execSQL(SQL_CONTRATACIONES)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contrataciones_publicidad_negocioId` ON `contrataciones_publicidad` (`negocioId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_contrataciones_publicidad_planId` ON `contrataciones_publicidad` (`planId`)")
                db.execSQL("INSERT INTO `contrataciones_publicidad` SELECT * FROM `respaldo_contrataciones`")
                db.execSQL(SQL_ESTADISTICAS)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_estadisticas_negocio_negocioId` ON `estadisticas_negocio` (`negocioId`)")
                db.execSQL("INSERT INTO `estadisticas_negocio` SELECT * FROM `respaldo_estadisticas`")
                db.execSQL("DROP TABLE `respaldo_contrataciones`")
                db.execSQL("DROP TABLE `respaldo_estadisticas`")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `resenas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, " +
                        "`negocioId` INTEGER NOT NULL, `calificacion` INTEGER NOT NULL, `comentario` TEXT NOT NULL, " +
                        "`fecha` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_resenas_usuarioId` ON `resenas` (`usuarioId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_resenas_negocioId` ON `resenas` (`negocioId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `favoritos` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `usuarioId` INTEGER NOT NULL, " +
                        "`negocioId` INTEGER NOT NULL, `fechaGuardado` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`usuarioId`) REFERENCES `usuarios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                        "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favoritos_usuarioId_negocioId` ON `favoritos` (`usuarioId`, `negocioId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_favoritos_negocioId` ON `favoritos` (`negocioId`)")

                db.execSQL("UPDATE `usuarios` SET `rol` = 'VISITANTE' WHERE `rol` = 'TURISTA'")
                insertarNegociosSemilla(db, semilla)
            }
        }

        /**
         * Versión 11: docs/negocios_bermejo.txt pasó de 11 negocios de ejemplo a los negocios reales de
         * Bermejo, con ids nuevos. Se borran los de ejemplo (con sus reseñas, favoritos, contrataciones y
         * estadísticas en cascada); los reales los carga [insertarNegociosSemilla] al abrir la base.
         */
        private val MIGRACION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `negocios` WHERE `duenoId` = 'semilla'")
            }
        }

        /**
         * Versión 12: las reseñas viajan a Firestore. Se reconstruye la tabla sin la FK a `usuarios` (las reseñas
         * de otros teléfonos no tienen usuario local) y `usuarioId` pasa a `autorId`, el hash del correo del autor.
         * Las reseñas ya guardadas se conservan; la próxima sincronización las sube a la nube.
         */
        private val MIGRACION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val autores = mutableMapOf<Long, String>()
                db.query("SELECT r.id, u.correo FROM resenas r JOIN usuarios u ON u.id = r.usuarioId").use {
                    while (it.moveToNext()) autores[it.getLong(0)] = IdDueno.deCorreo(it.getString(1))
                }
                db.execSQL(
                    "CREATE TABLE `resenas_nueva` (" +
                        "`id` INTEGER NOT NULL, `autorId` TEXT NOT NULL, `negocioId` INTEGER NOT NULL, " +
                        "`calificacion` INTEGER NOT NULL, `comentario` TEXT NOT NULL, `fecha` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`negocioId`) REFERENCES `negocios`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                // El id local (1, 2, 3...) se vuelve aleatorio para no chocar con reseñas de otros teléfonos.
                db.query("SELECT id, negocioId, calificacion, comentario, fecha FROM resenas").use {
                    while (it.moveToNext()) {
                        val viejoId = it.getLong(0)
                        db.execSQL(
                            "INSERT INTO resenas_nueva (id, autorId, negocioId, calificacion, comentario, fecha) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf<Any>(
                                Random.nextLong(1, 1L shl 52), autores[viejoId] ?: "", it.getLong(1),
                                it.getInt(2), it.getString(3), it.getLong(4)
                            )
                        )
                    }
                }
                db.execSQL("DROP TABLE `resenas`")
                db.execSQL("ALTER TABLE `resenas_nueva` RENAME TO `resenas`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_resenas_negocioId` ON `resenas` (`negocioId`)")
            }
        }

        @Volatile
        private var instancia: AppDatabase? = null

        fun obtenerInstancia(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                FotoNegocio.iniciar(context.applicationContext)
                instancia ?: crear(context.applicationContext).also { instancia = it }
            }

        private fun crear(context: Context): AppDatabase {
            val semilla = NegociosSemilla.cargar(context)
            return Room.databaseBuilder(context, AppDatabase::class.java, "lucidan.db")
                .addMigrations(
                    MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4, MIGRACION_4_5, MIGRACION_5_6,
                    MIGRACION_6_7, MIGRACION_7_8, MIGRACION_8_9, migracion9a10(semilla), MIGRACION_10_11,
                    MIGRACION_11_12
                ).addCallback(object : Callback() {
                    // Síncrono y dentro de la transacción de creación: al abrir el mapa ya están los datos.
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        insertarPlanesSemilla(db)
                        insertarAdminSemilla(db)
                    }

                    // INSERT OR IGNORE por id: el primer arranque carga los negocios y las aperturas siguientes no
                    // duplican nada. Los negocios agregados al final del archivo se cargan en la próxima apertura.
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        if (!db.isReadOnly) insertarNegociosSemilla(db, semilla)
                    }
                }).build()
        }
    }
}
