package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data object StationHome : AppRoute

    @Serializable
    data object LocationsMap : AppRoute

    @Serializable
    data object RechargePay : AppRoute

    @Serializable
    data object OwnerPanel : AppRoute
}
