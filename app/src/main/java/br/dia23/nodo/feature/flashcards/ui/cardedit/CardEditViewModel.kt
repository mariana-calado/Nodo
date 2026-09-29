package br.dia23.nodo.feature.flashcards.ui.cardedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.dia23.nodo.feature.flashcards.data.FlashcardRepository
import br.dia23.nodo.feature.flashcards.navigation.CardEditDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<CardEditDestination>()

    val isNewCard: Boolean = route.cardId == null

    /*
     * Estado desta tela como `mutableStateOf` (estado do Compose) em vez de StateFlow.
     * É a recomendação oficial para texto de TextField: a atualização é síncrona,
     * o que evita o cursor "pular" quando se digita rápido.
     * `private set`: a tela lê, mas só o ViewModel altera.
     */
    var front by mutableStateOf("")
        private set
    var back by mutableStateOf("")
        private set

    /** true enquanto a carta existente é lida do banco (só no modo edição). */
    var isLoading by mutableStateOf(!isNewCard)
        private set

    /** Vira true quando o "Salvar" termina; a tela observa e volta para a lista. */
    var isSaved by mutableStateOf(false)
        private set

    private var isSaving = false

    // Getter que lê estados do Compose: a tela recompõe sozinha quando front/back mudam.
    val canSave: Boolean
        get() = front.isNotBlank() && back.isNotBlank() && !isLoading

    init {
        // `init` roda uma vez por ViewModel; girar a tela não recarrega do banco (e não apaga o que foi digitado).
        route.cardId?.let { cardId ->
            viewModelScope.launch {
                repository.getCard(cardId)?.let { card ->
                    front = card.front
                    back = card.back
                }
                isLoading = false
            }
        }
    }

    fun onFrontChange(value: String) {
        front = value
    }

    fun onBackChange(value: String) {
        back = value
    }

    fun onSave() {
        if (!canSave || isSaving) return // evita salvar duas vezes com toque duplo
        isSaving = true
        viewModelScope.launch {
            val cardId = route.cardId
            if (cardId == null) {
                repository.createCard(route.deckId, front, back)
            } else {
                repository.updateCard(cardId, front, back)
            }
            // Só avisamos a tela depois de gravar: se voltássemos antes, o ViewModel seria destruído
            // e a coroutine de salvar poderia ser cancelada no meio.
            isSaved = true
        }
    }

    /** Só existe no modo criação: salva e limpa os campos para a próxima carta. */
    fun onSaveAndNew() {
        if (!canSave || !isNewCard) return
        // Copiamos os valores antes de limpar, porque a gravação termina depois (é assíncrona).
        val savedFront = front
        val savedBack = back
        front = ""
        back = ""
        viewModelScope.launch { repository.createCard(route.deckId, savedFront, savedBack) }
    }
}
