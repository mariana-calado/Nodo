package br.dia23.nodo.feature.flashcards.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import br.dia23.nodo.feature.flashcards.ui.cardedit.CardEditRoute
import br.dia23.nodo.feature.flashcards.ui.deckdetail.DeckDetailRoute
import br.dia23.nodo.feature.flashcards.ui.decks.DeckListRoute
import br.dia23.nodo.feature.flashcards.ui.study.StudyRoute
import kotlinx.serialization.Serializable

/*
 * Rotas tipadas: cada destino é uma classe/objeto @Serializable (nada de strings soltas como "decks/{id}").
 * Os argumentos viram propriedades da classe e o compilador confere os tipos.
 */

@Serializable
data object DeckListDestination

@Serializable
data class DeckDetailDestination(val deckId: String)

/** cardId == null significa "criar carta nova"; com valor, "editar esta carta". */
@Serializable
data class CardEditDestination(val deckId: String, val cardId: String? = null)

/** reviewAll = true: "Revisar todas" (inclui cartas que ainda não venceram). */
@Serializable
data class StudyDestination(val deckId: String, val reviewAll: Boolean = false)

/**
 * Registra as telas de flashcards no NavHost. Cada funcionalidade tem a sua função assim
 * (pomodoroGraph, plannerGraph...), e o NodoNavHost só as chama: uma não conhece as telas da outra.
 */
fun NavGraphBuilder.flashcardsGraph(navController: NavController) {
    composable<DeckListDestination> {
        DeckListRoute(
            onDeckClick = { deckId -> navController.navigate(DeckDetailDestination(deckId)) },
        )
    }
    composable<DeckDetailDestination> { backStackEntry ->
        // toRoute() lê os argumentos da rota atual de volta como objeto.
        val deckId = backStackEntry.toRoute<DeckDetailDestination>().deckId
        DeckDetailRoute(
            onNavigateUp = { navController.navigateUp() },
            onAddCard = { navController.navigate(CardEditDestination(deckId)) },
            onCardClick = { cardId -> navController.navigate(CardEditDestination(deckId, cardId)) },
            onStudy = { navController.navigate(StudyDestination(deckId)) },
            onStudyAll = { navController.navigate(StudyDestination(deckId, reviewAll = true)) },
        )
    }
    composable<CardEditDestination> {
        CardEditRoute(onNavigateUp = { navController.navigateUp() })
    }
    composable<StudyDestination> {
        StudyRoute(onNavigateUp = { navController.navigateUp() })
    }
}
