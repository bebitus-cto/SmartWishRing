package com.wishring.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wishring.app.presentation.wishdetail.WishDetailScreen
import com.wishring.app.presentation.home.HomeScreen
import com.wishring.app.presentation.splash.SplashScreen

import com.wishring.app.presentation.wishinput.WishInputScreen
import com.wishring.app.presentation.main.MainViewModel

/**
 * Navigation graph for the app
 */
@Composable
fun WishRingNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route,
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Splash screen
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Home screen
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { date ->
                    navController.navigate(Screen.Detail.createRoute(date))
                },
                onNavigateToWishInput = {
                    navController.navigate(Screen.WishInput.route)
                },
                mainViewModel = mainViewModel
            )
        }

        // Detail screen with date parameter
        composable(
            route = Screen.Detail.route,
            arguments = listOf(
                navArgument(Screen.Detail.ARG_DATE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString(Screen.Detail.ARG_DATE)
            WishDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Wish input screen
        composable(route = Screen.WishInput.route) {
            WishInputScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onWishSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.WishInput.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Screen definitions
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object WishInput : Screen("wish_input")

    object Detail : Screen("detail?date={date}") {
        const val ARG_DATE = "date"
        fun createRoute(date: String? = null): String {
            return if (date != null) {
                "detail?date=$date"
            } else {
                "detail"
            }
        }
    }
}

/**
 * Navigation extensions
 */
fun NavHostController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

fun NavHostController.navigateToDetail(date: String? = null) {
    navigate(Screen.Detail.createRoute(date))
}

fun NavHostController.navigateToWishInput() {
    navigate(Screen.WishInput.route)
}

fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}