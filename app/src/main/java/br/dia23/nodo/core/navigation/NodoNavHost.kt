package br.dia23.nodo.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import br.dia23.nodo.feature.flashcards.navigation.DeckListDestination
import br.dia23.nodo.feature.flashcards.navigation.flashcardsGraph

/** NavHost do app: só junta os grafos de cada funcionalidade. */
@Composable
fun NodoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DeckListDestination,
        modifier = modifier,
    ) {
        flashcardsGraph(navController)
    }
}
