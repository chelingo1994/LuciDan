package dev.justinianomarcelo.lucidanapp.data.local

/**
 * Versión 13 -> 14: redes sociales, oferta del día y sello verificado del negocio. Solo agrega columnas, no
 * borra datos. Vive aparte de [AppDatabase] para que una prueba JVM ejecute el mismo SQL sobre SQLite.
 */
internal val SQL_MIGRACION_13_14 = listOf(
    "ALTER TABLE `negocios` ADD COLUMN `facebook` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `negocios` ADD COLUMN `instagram` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `negocios` ADD COLUMN `tiktok` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `negocios` ADD COLUMN `ofertaTexto` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `negocios` ADD COLUMN `ofertaHasta` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `negocios` ADD COLUMN `verificado` INTEGER NOT NULL DEFAULT 0"
)
