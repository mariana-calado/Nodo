package br.dia23.nodo.feature.flashcards.ui.study

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.data.FlashcardRepository
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import br.dia23.nodo.feature.flashcards.domain.Sm2
import br.dia23.nodo.feature.flashcards.domain.StudySession
import br.dia23.nodo.feature.flashcards.domain.sm2State
import br.dia23.nodo.feature.flashcards.navigation.StudyDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class StudyUiState(
    val isLoading: Boolean = true,
    val deckName: String = "",
    /** Carta atual; null quando a sessão terminou (ou não havia nada para revisar). */
    val card: CardEntity? = null,
    /** Muda a cada resposta. A tela usa como chave para a próxima carta começar pela frente, sem animação. */
    val turn: Int = 0,
    val isFlipped: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    /** Próximo intervalo (dias) de cada botão. Vazio quando a carta é repetição da sessão (não reagenda). */
    val nextIntervals: Map<ReviewGrade, Int> = emptyMap(),
    /** Contagem das respostas, preenchida quando a sessão termina. */
    val summary: Map<ReviewGrade, Int> = emptyMap(),
) {
    val isFinished: Boolean get() = !isLoading && card == null
}

/**
 * ViewModel "fino": a regra da fila está na StudySession e o cálculo no Sm2 (ambos testados em JVM).
 * Aqui só ligamos as peças: carregar, repassar respostas, gravar e publicar o estado.
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FlashcardRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<StudyDestination>()
    private val deckId = route.deckId
    private var session: StudySession? = null
    private var turn = 0

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val deckName = repository.observeDeck(deckId).first()?.name.orEmpty()
            val cards = if (route.reviewAll) repository.getAllCards(deckId) else repository.getDueCards(deckId)
            val newSession = StudySession(cards)
            session = newSession
            _uiState.value = buildState(newSession, deckName)
        }
    }

    fun onFlip() = _uiState.update { it.copy(isFlipped = !it.isFlipped) }

    fun onAnswer(grade: ReviewGrade) {
        val session = session ?: return
        val card = session.current ?: return
        // Só aceita resposta com o verso visível. Também barra toque duplo: após a 1ª resposta, isFlipped volta a false.
        if (!_uiState.value.isFlipped) return

        if (session.answer(grade)) {
            viewModelScope.launch {
                // Se a pessoa sair logo depois da última resposta, o viewModelScope é cancelado.
                // NonCancellable garante que a gravação já iniciada termine mesmo assim.
                withContext(NonCancellable) { repository.reviewCard(card, grade) }
            }
        }
        turn++
        _uiState.value = buildState(session, _uiState.value.deckName)
    }

    private fun buildState(session: StudySession, deckName: String): StudyUiState {
        val card = session.current
        return StudyUiState(
            isLoading = false,
            deckName = deckName,
            card = card,
            turn = turn,
            isFlipped = false,
            completed = session.completed,
            total = session.total,
            nextIntervals = if (card == null || session.isCurrentRepeat) {
                emptyMap()
            } else {
                // Simula cada resposta sem gravar nada: é só uma prévia para os botões.
                ReviewGrade.entries.associateWith { Sm2.review(card.sm2State(), it).intervalDays }
            },
            summary = session.summary,
        )
    }
}
