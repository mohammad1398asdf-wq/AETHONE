package com.aethone.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aethone.client.ui.AppViewModel
import com.aethone.client.ui.home.HomeScreen
import com.aethone.client.ui.settings.AboutScreen
import com.aethone.client.ui.settings.AdvancedScreen
import com.aethone.client.ui.settings.AppearanceScreen
import com.aethone.client.ui.settings.AppsScreen
import com.aethone.client.ui.settings.ConnectionScreen
import com.aethone.client.ui.settings.DiagnosticsScreen
import com.aethone.client.ui.settings.FeaturesScreen
import com.aethone.client.ui.settings.RoutingScreen
import com.aethone.client.ui.settings.SettingsRootScreen

@Composable
fun AethoneNavHost() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(vm = vm, onOpenSettings = { nav.navigate("settings") })
        }
        composable("settings") {
            SettingsRootScreen(vm = vm, onOpen = { nav.navigate(it) })
        }
        composable("connection") { ConnectionScreen(vm) { nav.popBackStack() } }
        composable("routing") { RoutingScreen(vm) { nav.popBackStack() } }
        composable("apps") { AppsScreen(vm) { nav.popBackStack() } }
        composable("appearance") { AppearanceScreen(vm) { nav.popBackStack() } }
        composable("features") { FeaturesScreen(vm) { nav.popBackStack() } }
        composable("advanced") { AdvancedScreen(vm) { nav.popBackStack() } }
        composable("diagnostics") { DiagnosticsScreen(vm) { nav.popBackStack() } }
        composable("about") { AboutScreen(vm) { nav.popBackStack() } }
    }
}
