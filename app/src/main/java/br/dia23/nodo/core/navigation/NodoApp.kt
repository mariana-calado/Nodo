package br.dia23.nodo.core.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.dia23.nodo.R
import br.dia23.nodo.feature.flashcards.navigation.DeckListDestination
import br.dia23.nodo.feature.flashcards.navigation.flashcardsGraph
import br.dia23.nodo.feature.planner.navigation.PlannerDestination
import br.dia23.nodo.feature.planner.navigation.plannerGraph
import br.dia23.nodo.feature.pomodoro.navigation.PomodoroDestination
import br.dia23.nodo.feature.pomodoro.navigation.pomodoroGraph
import br.dia23.nodo.feature.stats.navigation.StatsDestination
import br.dia23.nodo.feature.stats.navigation.statsGraph

/** As abas da barra inferior. Ícone "cheio" quando a aba está selecionada, como pede o Material 3. */
enum class TopLevelDestination(
    val route: Any,
    @get:StringRes val label: Int,
    @get:DrawableRes val icon: Int,
    @get:DrawableRes val selectedIcon: Int,
) {
    DECKS(DeckListDestination, R.string.nav_decks, R.drawable.ic_style, R.drawable.ic_style_filled),
    PLANNER(PlannerDestination, R.string.nav_planner, R.drawable.ic_calendar_month, R.drawable.ic_calendar_month_filled),
    POMODORO(PomodoroDestination, R.string.nav_pomodoro, R.drawable.ic_timer, R.drawable.ic_timer_filled),
    STATS(StatsDestination, R.string.nav_stats, R.drawable.ic_bar_chart, R.drawable.ic_bar_chart),
}

/** Raiz da interface: barra inferior + NavHost com os grafos de cada funcionalidade. */
@Composable
fun NodoApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // A barra só aparece nas telas principais; some no detalhe do deck, edição de carta e estudo.
    val currentTab = TopLevelDestination.entries.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
    }

    Scaffold(
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        val selected = tab == currentTab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab) },
                            icon = {
                                // O texto da aba já descreve o ícone para o leitor de tela.
                                Icon(painterResource(if (selected) tab.selectedIcon else tab.icon), contentDescription = null)
                            },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
        // Cada tela cuida das margens do sistema; este Scaffold só reserva o espaço da barra inferior.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DeckListDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            flashcardsGraph(navController)
            plannerGraph()
            pomodoroGraph()
            statsGraph()
        }
    }
}

/**
 * Navegação entre abas no padrão recomendado:
 * - popUpTo(início) + saveState: não empilha abas (voltar sai do app em vez de passear pelas abas);
 * - restoreState: ao voltar a uma aba, ela reaparece como estava (ex.: dentro de um deck);
 * - launchSingleTop: tocar na aba atual não abre uma segunda cópia da tela.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
