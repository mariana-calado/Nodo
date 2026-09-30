package br.dia23.nodo.feature.planner.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.dia23.nodo.feature.planner.ui.PlannerRoute
import kotlinx.serialization.Serializable

@Serializable
data object PlannerDestination

fun NavGraphBuilder.plannerGraph() {
    composable<PlannerDestination> { PlannerRoute() }
}
