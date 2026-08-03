package com.gorilla.music.ui.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gorilla.music.ui.AppViewModel
import com.gorilla.music.ui.screens.browse.BrowseScreen
import com.gorilla.music.ui.screens.home.HomeScreen
import com.gorilla.music.ui.screens.library.LibraryScreen
import com.gorilla.music.ui.screens.playlists.PlaylistsScreen
import com.gorilla.music.ui.screens.radio.RadioScreen
import com.gorilla.music.ui.screens.search.SearchScreen
import com.gorilla.music.ui.screens.settings.SettingsScreen

/** Hosts the five top-level tab destinations. Now Playing is an app-level overlay. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun GorillaNavHost(
    navController: NavHostController,
    app: AppViewModel,
    onOpenNowPlaying: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
        enterTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            val direction = navigationDirection(
                fromRoute = fromRoute,
                toRoute = toRoute,
            )
            if (isHomeLibraryTransition(fromRoute, toRoute)) {
                slideInHorizontally(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> direction * (width / 3) },
                ) + fadeIn()
            } else {
                slideInHorizontally(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing),
                    initialOffsetX = { width -> direction * (width / 4) },
                ) + fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing))
            }
        },
        exitTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            val direction = navigationDirection(
                fromRoute = fromRoute,
                toRoute = toRoute,
            )
            if (isHomeLibraryTransition(fromRoute, toRoute)) {
                slideOutHorizontally(
                    animationSpec = tween(260),
                    targetOffsetX = { width -> -direction * (width / 5) },
                ) + fadeOut()
            } else {
                slideOutHorizontally(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    targetOffsetX = { width -> -direction * (width / 8) },
                ) + fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
            }
        },
        popEnterTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            val direction = navigationDirection(
                fromRoute = fromRoute,
                toRoute = toRoute,
            )
            if (isHomeLibraryTransition(fromRoute, toRoute)) {
                slideInHorizontally(
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> direction * (width / 3) },
                ) + fadeIn()
            } else {
                slideInHorizontally(
                    animationSpec = tween(240, easing = LinearOutSlowInEasing),
                    initialOffsetX = { width -> direction * (width / 4) },
                ) + fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing))
            }
        },
        popExitTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            val direction = navigationDirection(
                fromRoute = fromRoute,
                toRoute = toRoute,
            )
            if (isHomeLibraryTransition(fromRoute, toRoute)) {
                slideOutHorizontally(
                    animationSpec = tween(260),
                    targetOffsetX = { width -> -direction * (width / 5) },
                ) + fadeOut()
            } else {
                slideOutHorizontally(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    targetOffsetX = { width -> -direction * (width / 8) },
                ) + fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
            }
        },
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                app = app, 
                contentPadding = contentPadding, 
                onNavigateToLibrary = {
                    navController.navigate(Destination.Library.route) {
                        popUpTo(Destination.Home.route) { saveState = false }
                        launchSingleTop = true
                        restoreState = false
                    }
                },
                onNavigateToPlaylists = {
                    navController.navigate(Destination.Playlists.route) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destination.Library.route) {
            LibraryScreen(
                app = app,
                contentPadding = contentPadding,
                onOpenNowPlaying = onOpenNowPlaying,
                onOpenPlaylists = {
                    navController.navigate(Destination.Playlists.route) {
                        launchSingleTop = true
                    }
                },
                onOpenRadio = {
                    navController.navigate(Destination.Radio.route) {
                        launchSingleTop = true
                    }
                },
                onBackToOrigin = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destination.Home.route) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
        composable(Destination.Search.route) {
            SearchScreen(app = app, contentPadding = contentPadding, onOpenNowPlaying = onOpenNowPlaying)
        }
        composable(Destination.Browse.route) {
            BrowseScreen(app = app, contentPadding = contentPadding, onOpenNowPlaying = onOpenNowPlaying)
        }
        composable(Destination.Playlists.route) {
            PlaylistsScreen(
                app = app, 
                contentPadding = contentPadding, 
                onOpenNowPlaying = onOpenNowPlaying,
                onBackToOrigin = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Destination.Home.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(Destination.Radio.route) {
            RadioScreen(app = app, contentPadding = contentPadding, onOpenNowPlaying = onOpenNowPlaying)
        }
        composable(Destination.Settings.route) {
            SettingsScreen(app = app, contentPadding = contentPadding)
        }
    }
}

private fun navigationDirection(fromRoute: String?, toRoute: String?): Int {
    val routeOrder = listOf(
        Destination.Home.route,
        Destination.Browse.route,
        Destination.Search.route,
        Destination.Library.route,
        Destination.Radio.route,
        Destination.Playlists.route,
        Destination.Settings.route,
    )
    val fromIndex = routeOrder.indexOf(fromRoute).coerceAtLeast(0)
    val toIndex = routeOrder.indexOf(toRoute).coerceAtLeast(0)
    return if (toIndex >= fromIndex) 1 else -1
}

private fun isHomeLibraryTransition(fromRoute: String?, toRoute: String?): Boolean =
    (fromRoute == Destination.Home.route && toRoute == Destination.Library.route) ||
        (fromRoute == Destination.Library.route && toRoute == Destination.Home.route)
