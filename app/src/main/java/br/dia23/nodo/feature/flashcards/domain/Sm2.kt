package br.dia23.nodo.feature.flashcards.domain

import br.dia23.nodo.feature.flashcards.data.CardEntity
import kotlin.math.roundToInt

/**
 * As 4 respostas do modo estudo e a nota (0 a 5) que cada uma representa no SM-2 original.
 * Notas >= 3 contam como acerto.
 */
enum class ReviewGrade(val quality: Int) {
    AGAIN(1), // Errei
    HARD(3), // Difícil
    GOOD(4), // Bom
    EASY(5), // Fácil
    ;

    val isCorrect: Boolean get() = quality >= 3
}

/** O "estado de memória" de uma carta: os três números que o SM-2 lê e atualiza. */
data class Sm2State(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
)

fun CardEntity.sm2State() = Sm2State(easeFactor, intervalDays, repetitions)

/**
 * Algoritmo SM-2 (SuperMemo 2, Piotr Woźniak, 1987) como função pura:
 * mesma entrada, mesma saída, sem banco, sem relógio, sem Android. Por isso é testável em JVM puro.
 */
object Sm2 {
    const val MIN_EASE_FACTOR = 1.3

    fun review(state: Sm2State, grade: ReviewGrade): Sm2State {
        if (!grade.isCorrect) {
            // Errou: a sequência recomeça e a carta volta amanhã.
            // Como no SM-2 original, o easeFactor NÃO muda ao errar.
            return state.copy(repetitions = 0, intervalDays = 1)
        }

        val interval = when (state.repetitions) {
            0 -> 1 // primeiro acerto: revisa amanhã
            1 -> 6 // segundo acerto: daqui a 6 dias
            else -> (state.intervalDays * state.easeFactor).roundToInt() // depois: cresce pelo fator
        }

        // Fórmula do SM-2: q=5 soma 0,10; q=4 mantém; q=3 subtrai 0,14. Nunca abaixo de 1,3.
        val q = grade.quality
        val newEase = state.easeFactor + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))

        return Sm2State(
            easeFactor = newEase.coerceAtLeast(MIN_EASE_FACTOR),
            intervalDays = interval,
            repetitions = state.repetitions + 1,
        )
    }
}
