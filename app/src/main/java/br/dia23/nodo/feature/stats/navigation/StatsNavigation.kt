package br.dia23.nodo.feature.stats.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.dia23.nodo.feature.stats.ui.StatsRoute
import kotlinx.serialization.Serializable

@Serializable
data object StatsDestination

fun NavGraphBuilder.statsGraph() {
    composable<StatsDestination> { StatsRoute() }
}
