package br.dia23.nodo.feature.flashcards.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes unitários "puros": rodam na JVM do computador, sem emulador, em milissegundos.
 * Só é possível porque o Sm2 não depende de Android.
 */
class Sm2Test {

    private val newCard = Sm2State(easeFactor = 2.5, intervalDays = 0, repetitions = 0)

    @Test
    fun primeiroAcertoAgendaParaAmanha() {
        val next = Sm2.review(newCard, ReviewGrade.GOOD)

        assertEquals(1, next.intervalDays)
        assertEquals(1, next.repetitions)
    }

    @Test
    fun segundoAcertoAgendaParaSeisDias() {
        val next = Sm2.review(Sm2.review(newCard, ReviewGrade.GOOD), ReviewGrade.GOOD)

        assertEquals(6, next.intervalDays)
        assertEquals(2, next.repetitions)
    }

    @Test
    fun terceiroAcertoMultiplicaOIntervaloPeloFator() {
        val state = Sm2State(easeFactor = 2.5, intervalDays = 6, repetitions = 2)

        assertEquals(15, Sm2.review(state, ReviewGrade.GOOD).intervalDays) // 6 * 2,5
    }

    @Test
    fun fatorSobeComFacilMantemComBomECaiComDificil() {
        assertEquals(2.6, Sm2.review(newCard, ReviewGrade.EASY).easeFactor, 1e-9)
        assertEquals(2.5, Sm2.review(newCard, ReviewGrade.GOOD).easeFactor, 1e-9)
        assertEquals(2.36, Sm2.review(newCard, ReviewGrade.HARD).easeFactor, 1e-9)
    }

    @Test
    fun dificilAindaContaComoAcertoEAumentaOIntervalo() {
        val state = Sm2State(easeFactor = 2.5, intervalDays = 6, repetitions = 2)

        val next = Sm2.review(state, ReviewGrade.HARD)

        assertEquals(15, next.intervalDays) // usa o fator de antes da resposta
        assertEquals(3, next.repetitions)
    }

    @Test
    fun fatorNuncaFicaAbaixoDoMinimo() {
        val state = Sm2State(easeFactor = 1.35, intervalDays = 6, repetitions = 2)

        assertEquals(Sm2.MIN_EASE_FACTOR, Sm2.review(state, ReviewGrade.HARD).easeFactor, 1e-9)
    }

    @Test
    fun errarZeraASequenciaVoltaAmanhaENaoMudaOFator() {
        val state = Sm2State(easeFactor = 2.2, intervalDays = 40, repetitions = 5)

        val next = Sm2.review(state, ReviewGrade.AGAIN)

        assertEquals(0, next.repetitions)
        assertEquals(1, next.intervalDays)
        assertEquals(2.2, next.easeFactor, 1e-9)
    }
}
