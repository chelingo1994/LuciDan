package dev.justinianomarcelo.lucidanapp.data.repository

import dev.justinianomarcelo.lucidanapp.data.local.NegocioDao
import dev.justinianomarcelo.lucidanapp.data.model.Rubro
import dev.justinianomarcelo.lucidanapp.data.model.EstadoNegocio
import dev.justinianomarcelo.lucidanapp.data.model.Negocio
import dev.justinianomarcelo.lucidanapp.data.remote.NubeService
import dev.justinianomarcelo.lucidanapp.util.Busqueda
import dev.justinianomarcelo.lucidanapp.util.FotoNegocio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.justinianomarcelo.lucidanapp.util.IdDueno
import kotlin.random.Random

/**
 * Firestore es la fuente de verdad de los negocios y Room su caché local: la app lee siempre de Room
 * (funciona sin internet) y [sincronizar] la pone al día; las escrituras van a Room y a Firestore.
 */
class NegocioRepository(
    private val negocioDao: NegocioDao,
    private val nube: NubeService = NubeService()
) {

    /** Negocios del usuario con ese [correo] (0..*); el dueño se identifica por el hash del correo, no por el id local. */
    suspend fun obtenerPorDueno(correo: String): List<Negocio> = negocioDao.obtenerPorDueno(IdDueno.deCorreo(correo))

    /** El negocio [negocioId] solo si pertenece al usuario con ese [correo]: evita editar negocios ajenos. */
    suspend fun obtenerDelDueno(negocioId: Long, correo: String): Negocio? =
        negocioDao.obtenerPorId(negocioId)?.takeIf { it.duenoId == IdDueno.deCorreo(correo) }

    suspend fun obtenerNegocioPorId(id: Long): Negocio? = negocioDao.obtenerPorId(id)

    /**
     * DS07: registra el negocio en estado Pendiente. El id se genera aquí (no con autoincremento
     * de Room) para que no choque con los negocios creados desde otros teléfonos.
     */
    suspend fun registrarNegocio(negocio: Negocio): Long {
        val nuevo = negocio.copy(id = Random.nextLong(1, MAX_ID))
        negocioDao.insertar(nuevo)
        nube.guardarNegocio(nuevo)
        return nuevo.id
    }

    /** HU09: guarda cambios del perfil y los refleja de inmediato. */
    suspend fun actualizarNegocio(negocio: Negocio) {
        negocioDao.actualizar(negocio)
        nube.guardarNegocio(negocio)
    }

    /**
     * HU01/HU02: negocios publicados dentro de [radioMetros] desde el punto dado (sección 2.2
     * de CLAUDE.md). TA-10-05: los destacados (plan de publicidad activo) van primero.
     */
    suspend fun obtenerNegociosCercanos(
        lat: Double,
        lng: Double,
        radioMetros: Double,
        categoria: Rubro? = null,
        consulta: String = ""
    ): List<Negocio> {
        return negocioDao.obtenerPorEstado(EstadoNegocio.PUBLICADO)
            .filter { it.calcularDistancia(lat, lng) <= radioMetros }
            .filter { categoria == null || it.categoria == categoria }
            // La descripción dice qué vende el negocio: buscar "pan" encuentra una panadería de otro nombre.
            .filter { Busqueda.coincide(consulta, it.nombre, it.categoria.etiqueta, it.descripcion) }
            .sortedWith(compareByDescending<Negocio> { it.destacado }.thenBy { it.calcularDistancia(lat, lng) })
    }

    /** DS08: negocios destacados y publicados dentro del radio, para las notificaciones de cercanía. */
    suspend fun buscarDestacadosCercanos(lat: Double, lng: Double, radioMetros: Double): List<Negocio> =
        negocioDao.obtenerPorEstado(EstadoNegocio.PUBLICADO)
            .filter { it.destacado && it.calcularDistancia(lat, lng) <= radioMetros }

    /** DS05: guarda el promedio de reseñas (se calcula en el teléfono con las reseñas sincronizadas). */
    suspend fun recalcularPromedio(negocioId: Long, promedio: Double) {
        negocioDao.actualizarPromedio(negocioId, promedio)
    }

    /** DS09: activa o quita el destacado mientras el plan de publicidad esté vigente (HU10). */
    suspend fun actualizarEstadoDestacado(negocioId: Long, destacado: Boolean) {
        negocioDao.actualizarEstadoDestacado(negocioId, destacado)
        nube.actualizarDestacadoNegocio(negocioId, destacado)
    }

    /** DS11 (HU14): panel de administración. */
    suspend fun obtenerTodos(): List<Negocio> = negocioDao.obtenerTodos()

    suspend fun actualizarEstado(negocioId: Long, estado: EstadoNegocio) {
        negocioDao.actualizarEstado(negocioId, estado)
        nube.actualizarEstadoNegocio(negocioId, estado)
    }

    suspend fun eliminar(negocioId: Long) {
        negocioDao.eliminar(negocioId)
        nube.eliminarNegocio(negocioId)
    }

    /**
     * Baja los negocios de Firestore a Room. Devuelve true si la respuesta vino del servidor (no solo
     * de la caché de Firestore), que es cuando se puede confiar en ella para borrar lo que ya no existe.
     *
     * Los negocios que ya no están en la nube se borran de la caché (los eliminó un administrador),
     * salvo los que cumplan [subirLocal] (los creados antes de usar la nube; el llamador lo pide una sola
     * vez): esos se suben a Firestore. Los negocios de ejemplo ([IdDueno.SEMILLA]) nunca se borran por
     * sincronización: si faltan en la nube se vuelven a subir. Las escrituras no esperan al servidor y no
     * hay forma de distinguir "se borró" de "la subida falló" (reglas sin publicar, sin red), así que para
     * quitar un ejemplo hay que borrarlo de docs/negocios_bermejo.txt o suspenderlo desde el panel admin.
     */
    suspend fun sincronizar(subirLocal: (Negocio) -> Boolean = { false }): Boolean {
        val instantanea = nube.obtenerNegocios() ?: return false
        if (instantanea.desdeServidor) {
            val idsRemotos = instantanea.datos.map { it.id }.toSet()
            val ausentes = negocioDao.obtenerTodos().filter { it.id !in idsRemotos }
            val porSubir = ausentes.filter { it.duenoId == IdDueno.SEMILLA || subirLocal(it) }
            porSubir.forEach { nube.guardarNegocio(it) }
            negocioDao.eliminarPorIds((ausentes - porSubir.toSet()).map { it.id })
        }
        val locales = negocioDao.obtenerTodos().associateBy { it.id }
        // Las fotos viajan como texto Base64: se guardan como archivo y Room conserva solo la ruta.
        val remotos = withContext(Dispatchers.IO) {
            instantanea.datos.map { it.copy(fotos = it.fotos.mapNotNull(FotoNegocio::deTextoDeNube)) }
        }
        // Foto elegida en este teléfono que la nube todavía no tiene (negocio anterior a las fotos en la nube).
        remotos.forEach { remoto ->
            val local = locales[remoto.id] ?: return@forEach
            if (remoto.fotos.isEmpty() && local.fotos.firstOrNull()?.startsWith("/") == true) {
                nube.actualizarFotosNegocio(remoto.id, local.fotos)
            }
        }
        negocioDao.upsertar(remotos.map { remoto ->
            val local = locales[remoto.id]
            remoto.copy(
                usuarioId = local?.usuarioId ?: 0,
                // El promedio no viaja en el documento del negocio: lo recalcula ResenaRepository con las reseñas.
                promedioCalificacion = local?.promedioCalificacion ?: 0.0,
                // Si la nube no trae foto se conserva la local (la de los negocios de ejemplo viene dentro de la app).
                fotos = if (remoto.fotos.isEmpty()) local?.fotos ?: remoto.fotos else remoto.fotos
            )
        })
        return instantanea.desdeServidor
    }

    private companion object {
        /** Tope de 2^52 para que el id viaje sin pérdida como número y como texto. */
        const val MAX_ID = 1L shl 52
    }
}
