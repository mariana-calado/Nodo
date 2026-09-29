package br.dia23.nodo.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.dia23.nodo.feature.flashcards.ui.decks.DeckListRoute
import kotlinx.serialization.Serializable

/**
 * Rotas tipadas: cada destino é uma classe/objeto @Serializable (nada de strings soltas como "decks/{id}").
 * Se um destino precisar de argumento, ele vira propriedade da classe e o compilador confere os tipos.
 */
@Serializable
data object DeckListDestination

@Composable
fun NodoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = DeckListDestination,
        modifier = modifier,
    ) {
        composable<DeckListDestination> {
            DeckListRoute(
                // Etapa 3: aqui navegaremos para o detalhe do deck (lista de cartas).
                onDeckClick = { },
            )
        }
    }
}
