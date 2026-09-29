package br.dia23.nodo.feature.flashcards.ui.decks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.dia23.nodo.feature.flashcards.data.DeckEntity
import br.dia23.nodo.feature.flashcards.data.DeckWithStats
import br.dia23.nodo.feature.flashcards.data.FlashcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qual diálogo está aberto (no máximo um). Guardado no ViewModel para sobreviver à rotação da tela. */
sealed interface DeckDialog {
    data object None : DeckDialog
    data object Create : DeckDialog
    data class Edit(val deck: DeckEntity) : DeckDialog
    data class ConfirmDelete(val deck: DeckEntity) : DeckDialog
}

/**
 * Tudo o que a tela precisa para se desenhar. A tela é uma "função" deste objeto:
 * mesmo UiState = mesma aparência (é isso que torna previews e testes simples).
 */
data class DeckListUiState(
    val decks: List<DeckWithStats> = emptyList(),
    /** true até o Room entregar a primeira lista; evita piscar "nenhum deck" na abertura. */
    val isLoading: Boolean = true,
    val dialog: DeckDialog = DeckDialog.None,
)

@HiltViewModel // o Hilt cria este ViewModel e injeta o repositório no construtor
class DeckListViewModel @Inject constructor(
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val dialog = MutableStateFlow<DeckDialog>(DeckDialog.None)

    /**
     * Junta duas fontes (lista do banco + diálogo aberto) num único UiState.
     * `stateIn` transforma o Flow "frio" em StateFlow "quente" com valor atual.
     * `WhileSubscribed(5_000)`: para de consultar o banco 5s depois que a tela sai de vista,
     * mas não reinicia numa rotação (que leva menos que isso).
     */
    val uiState: StateFlow<DeckListUiState> = combine(repository.observeDecks(), dialog) { decks, dialog ->
        DeckListUiState(decks = decks, isLoading = false, dialog = dialog)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DeckListUiState(),
    )

    // --- Eventos vindos da tela (a tela só avisa "o usuário fez X"; a decisão é daqui) ---

    fun onAddClick() = dialog.update { DeckDialog.Create }

    fun onEditClick(deck: DeckEntity) = dialog.update { DeckDialog.Edit(deck) }

    fun onDeleteClick(deck: DeckEntity) = dialog.update { DeckDialog.ConfirmDelete(deck) }

    fun onDismissDialog() = dialog.update { DeckDialog.None }

    fun onSaveDeck(name: String, description: String) {
        // A tela já desabilita o botão com nome vazio, mas a regra também vive aqui.
        if (name.isBlank()) return
        val current = dialog.value
        dialog.update { DeckDialog.None }
        viewModelScope.launch {
            when (current) {
                is DeckDialog.Edit -> repository.updateDeck(current.deck.id, name, description)
                is DeckDialog.Create -> repository.createDeck(name, description)
                else -> Unit
            }
        }
    }

    fun onConfirmDelete() {
        val current = dialog.value as? DeckDialog.ConfirmDelete ?: return
        dialog.update { DeckDialog.None }
        viewModelScope.launch { repository.deleteDeck(current.deck.id) }
    }
}
