package dev.justinianomarcelo.lucidanapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.ui.admin.AdminScreen
import dev.justinianomarcelo.lucidanapp.ui.auth.LoginScreen
import dev.justinianomarcelo.lucidanapp.ui.auth.RecuperarContrasenaScreen
import dev.justinianomarcelo.lucidanapp.ui.auth.RegistroScreen
import dev.justinianomarcelo.lucidanapp.ui.detalle.DetalleScreen
import dev.justinianomarcelo.lucidanapp.ui.estadisticas.PanelEstadisticasScreen
import dev.justinianomarcelo.lucidanapp.ui.favoritos.FavoritosScreen
import dev.justinianomarcelo.lucidanapp.ui.home.HomeScreen
import dev.justinianomarcelo.lucidanapp.ui.negocio.EditarNegocioScreen
import dev.justinianomarcelo.lucidanapp.ui.negocio.NUEVO_NEGOCIO
import dev.justinianomarcelo.lucidanapp.ui.negocio.PanelNegocioScreen
import dev.justinianomarcelo.lucidanapp.ui.planes.PlanesScreen

object LuciDanRutas {
    const val ARG_NEGOCIO_ID = "negocioId"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val HOME = "home"
    const val FAVORITOS = "favoritos"
    const val PANEL_NEGOCIO = "panel_negocio"
    const val EDITAR_NEGOCIO = "editar_negocio/{$ARG_NEGOCIO_ID}"
    const val PLANES = "planes/{$ARG_NEGOCIO_ID}"
    const val ESTADISTICAS = "estadisticas/{$ARG_NEGOCIO_ID}"
    const val ADMIN = "admin"
    const val INFO_UTIL = "info_util"
    const val EDITAR_FRONTERA = "info_util_editar_frontera"
    const val DETALLE = "detalle/{$ARG_NEGOCIO_ID}"

    fun detalle(negocioId: Long) = "detalle/$negocioId"
    fun editarNegocio(negocioId: Long) = "editar_negocio/$negocioId"
    fun planes(negocioId: Long) = "planes/$negocioId"
    fun estadisticas(negocioId: Long) = "estadisticas/$negocioId"
}

@Composable
fun LuciDanNavHost(
    navController: NavHostController = rememberNavController(),
    negocioIdInicial: Long? = null
) {
    val argumentoNegocio = listOf(navArgument(LuciDanRutas.ARG_NEGOCIO_ID) { type = NavType.LongType })
    val destinoInicial = if (negocioIdInicial != null) LuciDanRutas.detalle(negocioIdInicial) else LuciDanRutas.LOGIN

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(LuciDanRutas.LOGIN) {
            LoginScreen(
                onIniciarSesionExitoso = {
                    navController.navigate(LuciDanRutas.HOME) {
                        popUpTo(LuciDanRutas.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(LuciDanRutas.REGISTRO) },
                onIrARecuperar = { navController.navigate(LuciDanRutas.RECUPERAR) }
            )
        }
        composable(LuciDanRutas.REGISTRO) {
            RegistroScreen(
                onRegistroExitoso = {
                    navController.navigate(LuciDanRutas.HOME) {
                        popUpTo(LuciDanRutas.LOGIN) { inclusive = true }
                    }
                },
                onVolver = { navController.popBackStack() }
            )
        }
        composable(LuciDanRutas.RECUPERAR) {
            RecuperarContrasenaScreen(
                onRecuperacionExitosa = { navController.popBackStack() },
                onVolver = { navController.popBackStack() }
            )
        }
        composable(LuciDanRutas.HOME) {
            HomeScreen(
                onNegocioClick = { negocioId -> navController.navigate(LuciDanRutas.detalle(negocioId)) },
                onIrAFavoritos = { navController.navigate(LuciDanRutas.FAVORITOS) },
                onIrAMiNegocio = { navController.navigate(LuciDanRutas.PANEL_NEGOCIO) },
                onIrAAdmin = { navController.navigate(LuciDanRutas.ADMIN) },
                onCerrarSesion = {
                    SessionManager.cerrarSesion()
                    navController.navigate(LuciDanRutas.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = LuciDanRutas.DETALLE,
            arguments = argumentoNegocio
        ) { entrada ->
            DetalleScreen(
                negocioId = entrada.arguments?.getLong(LuciDanRutas.ARG_NEGOCIO_ID) ?: 0L,
                onVolver = {
                    if (!navController.popBackStack()) {
                        navController.navigate(LuciDanRutas.HOME)
                    }
                },
                onRequiereSesion = {
                    navController.navigate(LuciDanRutas.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(LuciDanRutas.FAVORITOS) {
            FavoritosScreen(
                onVolver = { navController.popBackStack() },
                onNegocioClick = { negocioId -> navController.navigate(LuciDanRutas.detalle(negocioId)) }
            )
        }
        composable(LuciDanRutas.PANEL_NEGOCIO) {
            PanelNegocioScreen(
                onVolver = { navController.popBackStack() },
                onAgregarNegocio = { navController.navigate(LuciDanRutas.editarNegocio(NUEVO_NEGOCIO)) },
                onEditarNegocio = { negocioId -> navController.navigate(LuciDanRutas.editarNegocio(negocioId)) },
                onVerPlanes = { negocioId -> navController.navigate(LuciDanRutas.planes(negocioId)) },
                onVerEstadisticas = { negocioId -> navController.navigate(LuciDanRutas.estadisticas(negocioId)) }
            )
        }
        composable(LuciDanRutas.EDITAR_NEGOCIO, arguments = argumentoNegocio) { entrada ->
            EditarNegocioScreen(
                negocioId = entrada.arguments?.getLong(LuciDanRutas.ARG_NEGOCIO_ID) ?: NUEVO_NEGOCIO,
                onVolver = { navController.popBackStack() }
            )
        }
        composable(LuciDanRutas.PLANES, arguments = argumentoNegocio) { entrada ->
            PlanesScreen(
                negocioId = entrada.arguments?.getLong(LuciDanRutas.ARG_NEGOCIO_ID) ?: 0L,
                onVolver = { navController.popBackStack() }
            )
        }
        composable(LuciDanRutas.ESTADISTICAS, arguments = argumentoNegocio) { entrada ->
            PanelEstadisticasScreen(
                negocioId = entrada.arguments?.getLong(LuciDanRutas.ARG_NEGOCIO_ID) ?: 0L,
                onVolver = { navController.popBackStack() }
            )
        }
        composable(LuciDanRutas.ADMIN) {
            AdminScreen(onVolver = { navController.popBackStack() })
        }
        // «Info útil» (cambio, clima, frontera) queda para una fase posterior: el código sigue en ui/infoutil,
        // pero sin ruta ni acceso desde Home. Para reactivarla, volver a registrar INFO_UTIL y EDITAR_FRONTERA aquí.
    }
}
