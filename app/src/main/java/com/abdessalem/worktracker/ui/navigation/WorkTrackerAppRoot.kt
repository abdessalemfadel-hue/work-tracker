package com.abdessalem.worktracker.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abdessalem.worktracker.ui.analytics.AnalyticsScreen
import com.abdessalem.worktracker.ui.calendar.CalendarScreen
import com.abdessalem.worktracker.ui.dashboard.DashboardScreen
import com.abdessalem.worktracker.ui.history.HistoryScreen
import com.abdessalem.worktracker.ui.more.MoreScreen

private data class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val destinations = listOf(
    Destination("today", "Today", Icons.Rounded.Home),
    Destination("history", "History", Icons.Rounded.History),
    Destination("analytics", "Analytics", Icons.Rounded.Analytics),
    Destination("calendar", "Calendar", Icons.Rounded.CalendarMonth),
    Destination("more", "More", Icons.Rounded.MoreHoriz)
)

@Composable
fun WorkTrackerAppRoot() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentDestination = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "today",
            modifier = Modifier,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(240))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(240))
            }
        ) {
            composable("today") {
                DashboardScreen(contentPadding = padding, onOpenHistory = { navController.navigate("history") })
            }
            composable("history") { HistoryScreen(contentPadding = padding) }
            composable("analytics") { AnalyticsScreen(contentPadding = padding) }
            composable("calendar") { CalendarScreen(contentPadding = padding) }
            composable("more") { MoreScreen(contentPadding = padding) }
        }
    }
}
