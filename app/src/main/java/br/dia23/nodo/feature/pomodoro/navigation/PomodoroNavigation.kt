package br.dia23.nodo.feature.pomodoro.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import br.dia23.nodo.feature.pomodoro.ui.PomodoroRoute
import kotlinx.serialization.Serializable

@Serializable
data object PomodoroDestination

fun NavGraphBuilder.pomodoroGraph() {
    composable<PomodoroDestination> { PomodoroRoute() }
}
