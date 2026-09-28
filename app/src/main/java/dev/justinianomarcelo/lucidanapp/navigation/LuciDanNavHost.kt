package dev.justinianomarcelo.lucidanapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.justinianomarcelo.lucidanapp.auth.SessionManager
import dev.justinianomarcelo.lucidanapp.ui.auth.LoginScreen
import dev.justinianomarcelo.lucidanapp.ui.auth.RecuperarContrasenaScreen
import dev.justinianomarcelo.lucidanapp.ui.auth.RegistroScreen
import dev.justinianomarcelo.lucidanapp.ui.detalle.DetalleScreen
import dev.justinianomarcelo.lucidanapp.ui.favoritos.FavoritosScreen
import dev.justinianomarcelo.lucidanapp.ui.home.HomeScreen
import dev.justinianomarcelo.lucidanapp.ui.negocio.EditarNegocioScreen
import dev.justinianomarcelo.lucidanapp.ui.negocio.PanelNegocioScreen

object LuciDanRutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"
    const val HOME = "home"
    const val FAVORITOS = "favoritos"
    const val PANEL_NEGOCIO = "panel_negocio"
    const val EDITAR_NEGOCIO = "editar_negocio"
    const val ARG_LUGAR_ID = "lugarId"
    const val DETALLE = "detalle/{$ARG_LUGAR_ID}"

    fun detalle(lugarId: Long) = "detalle/$lugarId"
}

@Composable
fun LuciDanNavHost(
    navController: NavHostController = rememberNavController(),
    lugarIdInicial: Long? = null
) {
    val destinoInicial = if (lugarIdInicial != null) LuciDanRutas.detalle(lugarIdInicial) else LuciDanRutas.LOGIN

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
                onLugarClick = { lugarId -> navController.navigate(LuciDanRutas.detalle(lugarId)) },
                onIrAFavoritos = { navController.navigate(LuciDanRutas.FAVORITOS) },
                onIrAMiNegocio = { navController.navigate(LuciDanRutas.PANEL_NEGOCIO) },
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
            arguments = listOf(navArgument(LuciDanRutas.ARG_LUGAR_ID) { type = NavType.LongType })
        ) { entrada ->
            val lugarId = entrada.arguments?.getLong(LuciDanRutas.ARG_LUGAR_ID) ?: 0L
            DetalleScreen(
                lugarId = lugarId,
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
                onLugarClick = { lugarId -> navController.navigate(LuciDanRutas.detalle(lugarId)) }
            )
        }
        composable(LuciDanRutas.PANEL_NEGOCIO) {
            PanelNegocioScreen(
                onVolver = { navController.popBackStack() },
                onEditarPerfil = { navController.navigate(LuciDanRutas.EDITAR_NEGOCIO) }
            )
        }
        composable(LuciDanRutas.EDITAR_NEGOCIO) {
            EditarNegocioScreen(onVolver = { navController.popBackStack() })
        }
    }
}
