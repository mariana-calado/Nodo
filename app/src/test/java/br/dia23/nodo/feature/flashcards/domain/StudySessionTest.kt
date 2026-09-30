package br.dia23.nodo.feature.flashcards.domain

import br.dia23.nodo.feature.flashcards.data.CardEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionTest {

    private fun card(front: String) = CardEntity(id = front, deckId = "d", front = front, back = "-")

    @Test
    fun comecaPelaPrimeiraCartaEContaOTotal() {
        val session = StudySession(listOf(card("a"), card("b")))

        assertEquals("a", session.current?.id)
        assertEquals(2, session.total)
        assertEquals(0, session.completed)
        assertFalse(session.isFinished)
    }

    @Test
    fun acertoTiraACartaDaFila() {
        val session = StudySession(listOf(card("a"), card("b")))

        assertTrue(session.answer(ReviewGrade.GOOD))

        assertEquals("b", session.current?.id)
        assertEquals(1, session.completed)
    }

    @Test
    fun errarMandaACartaParaOFimDaFila() {
        val session = StudySession(listOf(card("a"), card("b")))

        session.answer(ReviewGrade.AGAIN)

        assertEquals("b", session.current?.id)
        assertEquals(0, session.completed) // "a" ainda não saiu de vez
        session.answer(ReviewGrade.GOOD)
        assertEquals("a", session.current?.id)
        assertTrue(session.isCurrentRepeat)
    }

    @Test
    fun soAPrimeiraRespostaContaParaAgendamentoEResumo() {
        val session = StudySession(listOf(card("a")))

        assertTrue(session.answer(ReviewGrade.AGAIN)) // primeira: salva o agendamento
        assertFalse(session.answer(ReviewGrade.AGAIN)) // repetição: não salva
        assertFalse(session.answer(ReviewGrade.GOOD))

        assertTrue(session.isFinished)
        assertNull(session.current)
        assertEquals(mapOf(ReviewGrade.AGAIN to 1), session.summary)
    }

    @Test
    fun sessaoVaziaJaComecaTerminada() {
        val session = StudySession(emptyList())

        assertTrue(session.isFinished)
        assertFalse(session.answer(ReviewGrade.GOOD))
    }
}
