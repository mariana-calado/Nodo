package br.dia23.nodo.feature.flashcards.domain

import br.dia23.nodo.feature.flashcards.data.CardEntity

/**
 * Fila de uma sessão de estudo. Kotlin puro: não salva nada, só decide a ordem das cartas.
 *
 * Regras:
 * - Só a PRIMEIRA resposta de cada carta na sessão conta para o agendamento (SM-2) e para o resumo.
 * - Carta respondida com "Errei" volta para o fim da fila, até ser acertada (fixação, como no Anki).
 */
class StudySession(cards: List<CardEntity>) {

    private val queue = ArrayDeque(cards)
    private val answeredIds = mutableSetOf<String>()
    private val firstAnswers = mutableMapOf<ReviewGrade, Int>()

    /** Quantas cartas diferentes a sessão tem. */
    val total: Int = cards.size

    /** Carta na frente da fila, ou null quando a sessão acabou. */
    val current: CardEntity? get() = queue.firstOrNull()

    val isFinished: Boolean get() = queue.isEmpty()

    /** Cartas que já saíram da fila de vez (cada carta aparece no máximo uma vez na fila). */
    val completed: Int get() = total - queue.size

    /** true se a carta atual já foi respondida antes nesta sessão (voltou por "Errei"). */
    val isCurrentRepeat: Boolean get() = current?.id in answeredIds

    /** Contagem das primeiras respostas, para o resumo final. */
    val summary: Map<ReviewGrade, Int> get() = firstAnswers.toMap()

    /**
     * Registra a resposta para a carta atual e avança a fila.
     * @return true se foi a primeira resposta desta carta, ou seja, se o agendamento deve ser salvo.
     */
    fun answer(grade: ReviewGrade): Boolean {
        val card = queue.removeFirstOrNull() ?: return false
        val isFirstAnswer = answeredIds.add(card.id) // add() devolve false se o id já estava no set
        if (isFirstAnswer) firstAnswers[grade] = (firstAnswers[grade] ?: 0) + 1
        if (grade == ReviewGrade.AGAIN) queue.addLast(card)
        return isFirstAnswer
    }
}
