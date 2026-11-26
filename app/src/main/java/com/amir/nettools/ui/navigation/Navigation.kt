package com.amir.nettools.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.amir.nettools.domain.model.ToolType
import com.amir.nettools.ui.components.t
import com.amir.nettools.ui.screens.*

private const val HOME = "home"
private const val TOOLS = "tools"
private const val HISTORY = "history"
private const val SETTINGS = "settings"

private data class TopItem(val route: String, val fa: String, val en: String, val icon: ImageVector)

private val topItems = listOf(
    TopItem(HOME, "خانه", "Home", Icons.Filled.Home),
    TopItem(TOOLS, "ابزارها", "Tools", Icons.Filled.Build),
    TopItem(HISTORY, "تاریخچه", "History", Icons.Filled.History),
    TopItem(SETTINGS, "تنظیمات", "Settings", Icons.Filled.Settings)
)

private fun NavHostController.goTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = topItems.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                    topItems.forEach { item ->
                        NavigationBarItem(
                            selected = route == item.route,
                            onClick = { nav.goTop(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(t(item.fa, item.en)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = HOME, modifier = Modifier.padding(padding)) {
            composable(HOME) {
                HomeScreen(
                    onOpenTool = { nav.navigate(it.route) },
                    onOpenHistory = { nav.goTop(HISTORY) }
                )
            }
            composable(TOOLS) { ToolsScreen(onOpenTool = { nav.navigate(it.route) }) }
            composable(HISTORY) { HistoryScreen() }
            composable(SETTINGS) { SettingsScreen() }
            ToolType.entries.forEach { type ->
                composable(type.route) {
                    val back: () -> Unit = { nav.popBackStack() }
                    when (type) {
                        ToolType.PING -> PingScreen(back)
                        ToolType.DNS -> DnsScreen(back)
                        ToolType.IP_INFO -> IpScreen(back)
                        ToolType.HTTP -> HttpTestScreen(back)
                        ToolType.CONNECTIVITY -> ConnectivityScreen(back)
                        ToolType.NETWORK_INFO -> NetworkInfoScreen(back)
                        ToolType.PORT -> PortCheckerScreen(back)
                        ToolType.TRACEROUTE -> TraceRouteScreen(back)
                        ToolType.SPEED -> SpeedTestScreen(back)
                        ToolType.WIFI -> WifiInfoScreen(back)
                        ToolType.PUBLIC_IP -> PublicIpScreen(back)
                        ToolType.SUBNET -> SubnetScreen(back)
                    }
                }
            }
        }
    }
}
