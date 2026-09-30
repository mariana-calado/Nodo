package br.dia23.nodo.feature.flashcards.ui.deckdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.data.FlashcardRepository
import br.dia23.nodo.feature.flashcards.navigation.DeckDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeckDetailUiState(
    val deckName: String = "",
    val cards: List<CardEntity> = emptyList(),
    /** Cartas para revisar hoje: o número do botão "Estudar". */
    val dueCount: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class DeckDetailViewModel @Inject constructor(
    // SavedStateHandle traz os argumentos da rota (o deckId) e sobrevive até à morte do processo:
    // se o Android fechar o app em segundo plano, ao voltar a tela sabe qual deck mostrar.
    savedStateHandle: SavedStateHandle,
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val deckId = savedStateHandle.toRoute<DeckDetailDestination>().deckId

    val uiState: StateFlow<DeckDetailUiState> = combine(
        repository.observeDeck(deckId),
        repository.observeCards(deckId),
        repository.observeDueCount(deckId),
    ) { deck, cards, dueCount ->
        DeckDetailUiState(deckName = deck?.name.orEmpty(), cards = cards, dueCount = dueCount, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckDetailUiState(),
    )

    fun onDeleteCard(cardId: String) {
        viewModelScope.launch { repository.deleteCard(cardId) }
    }

    /** "Desfazer" do Snackbar: como a exclusão é lógica, basta desmarcar isDeleted. */
    fun onUndoDelete(cardId: String) {
        viewModelScope.launch { repository.restoreCard(cardId) }
    }
}
