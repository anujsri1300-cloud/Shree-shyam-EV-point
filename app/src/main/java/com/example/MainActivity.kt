package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.LocationsMapScreen
import com.example.ui.screens.OwnerPanelScreen
import com.example.ui.screens.RechargeScreen
import com.example.ui.screens.StationHomeScreen
import com.example.ui.theme.ShreeShyamTheme
import com.example.ui.viewmodel.ChargingViewModel
import com.example.util.FormattingUtils

private data class NavDestinationItem(
    val route: AppRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShreeShyamTheme {
                ShreeShyamApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShreeShyamApp(
    viewModel: ChargingViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val config by viewModel.stationConfig.collectAsStateWithLifecycle()
    val locations by viewModel.locations.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val selectedStationId by viewModel.selectedStationId.collectAsStateWithLifecycle()
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val isOwnerUnlocked by viewModel.isOwnerUnlocked.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val latestReceipt by viewModel.latestReceipt.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(
                route = AppRoute.StationHome,
                label = "Station",
                selectedIcon = Icons.Filled.EvStation,
                unselectedIcon = Icons.Outlined.EvStation,
                testTag = "nav_tab_station"
            ),
            NavDestinationItem(
                route = AppRoute.LocationsMap,
                label = "Locations",
                selectedIcon = Icons.Filled.LocationOn,
                unselectedIcon = Icons.Outlined.LocationOn,
                testTag = "nav_tab_locations"
            ),
            NavDestinationItem(
                route = AppRoute.RechargePay,
                label = "Recharge",
                selectedIcon = Icons.Filled.Bolt,
                unselectedIcon = Icons.Outlined.Bolt,
                testTag = "nav_tab_recharge"
            ),
            NavDestinationItem(
                route = AppRoute.OwnerPanel,
                label = "Owner",
                selectedIcon = Icons.Filled.AdminPanelSettings,
                unselectedIcon = Icons.Outlined.AdminPanelSettings,
                testTag = "nav_tab_owner"
            )
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: AppRoute) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = config.businessName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "${FormattingUtils.formatInr(config.ratePerUnitInr)}/u",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(item.route::class)
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.hasRoute(item.route::class)
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = AppRoute.StationHome
                    ) {
                        composable<AppRoute.StationHome> {
                            StationHomeScreen(
                                config = config,
                                locations = locations,
                                transactions = transactions,
                                summary = summary,
                                activeSession = activeSession,
                                onNavigateToRecharge = { navigateToTab(AppRoute.RechargePay) },
                                onNavigateToMap = { navigateToTab(AppRoute.LocationsMap) },
                                onNavigateToOwner = { navigateToTab(AppRoute.OwnerPanel) },
                                onToggleMeter = viewModel::toggleMeterRunning,
                                onStopMeter = viewModel::stopAndCompleteSession,
                                onDismissSession = viewModel::clearCompletedSession
                            )
                        }
                        composable<AppRoute.LocationsMap> {
                            LocationsMapScreen(
                                locations = locations,
                                selectedStationId = selectedStationId,
                                userLocation = userLocation,
                                ratePerUnit = config.ratePerUnitInr,
                                onSelectStation = viewModel::selectStation,
                                onUserLocationFound = viewModel::updateUserLocation,
                                onRechargeAtStation = { stationId ->
                                    viewModel.selectStation(stationId)
                                    navigateToTab(AppRoute.RechargePay)
                                },
                                onStatusMessage = viewModel::showStatusMessage
                            )
                        }
                        composable<AppRoute.RechargePay> {
                            RechargeScreen(
                                config = config,
                                locations = locations,
                                selectedStationId = selectedStationId,
                                latestReceipt = latestReceipt,
                                onSelectStation = viewModel::selectStation,
                                onSubmitRecharge = viewModel::processRecharge,
                                onDismissReceipt = viewModel::dismissReceipt,
                                onGoToMeter = { navigateToTab(AppRoute.StationHome) },
                                onStatusMessage = viewModel::showStatusMessage
                            )
                        }
                        composable<AppRoute.OwnerPanel> {
                            OwnerPanelScreen(
                                config = config,
                                locations = locations,
                                transactions = transactions,
                                summary = summary,
                                isOwnerUnlocked = isOwnerUnlocked,
                                userLocation = userLocation,
                                onUnlockOwner = viewModel::unlockOwner,
                                onLockOwner = viewModel::lockOwner,
                                onUpdateRate = viewModel::updatePerUnitRate,
                                onSaveSettings = viewModel::saveOwnerSettings,
                                onSaveLocation = viewModel::saveStationLocation,
                                onDeleteLocation = viewModel::deleteStationLocation,
                                onDeleteTransaction = viewModel::deleteTransaction
                            )
                        }
                    }
                }
            }
        }
    }
}
