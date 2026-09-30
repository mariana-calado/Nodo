package br.dia23.nodo.feature.pomodoro.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroTimerTest {

    private val settings = PomodoroSettings(focusMinutes = 25, shortBreakMinutes = 5, longBreakMinutes = 15)
    private val minute = 60_000L
    private val t0 = 1_000_000L // um horário qualquer; só as diferenças importam

    private fun newFocus(completed: Int = 0) =
        PomodoroTimer.idle(PomodoroPhase.FOCUS, settings, subjectId = "mat", completedFocuses = completed)

    @Test
    fun iniciarComecaAContarAPartirDeAgora() {
        val running = PomodoroTimer.start(newFocus(), t0)

        assertTrue(running.isRunning)
        assertEquals(25 * minute, running.remainingMs(t0))
        assertEquals(15 * minute, running.remainingMs(t0 + 10 * minute))
        assertEquals(t0 + 25 * minute, running.endsAt())
    }

    @Test
    fun pausaCongelaOTempoERetomarContinuaDeOndeParou() {
        val paused = PomodoroTimer.pause(PomodoroTimer.start(newFocus(), t0), t0 + 10 * minute)

        // Uma hora pausado não consome tempo.
        assertFalse(paused.isRunning)
        assertEquals(15 * minute, paused.remainingMs(t0 + 70 * minute))

        val resumed = PomodoroTimer.start(paused, t0 + 70 * minute)
        assertEquals(t0 + 70 * minute + 15 * minute, resumed.endsAt())
        assertEquals(t0, resumed.startedAt) // o início da fase não muda ao retomar
    }

    @Test
    fun terminaQuandoOTempoAcaba() {
        val running = PomodoroTimer.start(newFocus(), t0)

        assertFalse(running.isFinished(t0 + 25 * minute - 1))
        assertTrue(running.isFinished(t0 + 25 * minute))
        assertEquals(0, running.remainingMs(t0 + 40 * minute)) // nunca fica negativo
    }

    @Test
    fun fimDoFocoSalvaSessaoCompletaEPreparaPausaCurta() {
        val running = PomodoroTimer.start(newFocus(), t0)

        // O app só percebeu o fim 3 minutos depois: a sessão termina no horário teórico mesmo assim.
        val (next, record) = PomodoroTimer.finish(running, settings, now = t0 + 28 * minute)

        assertEquals(PomodoroPhase.SHORT_BREAK, next.phase)
        assertFalse(next.isStarted)
        assertEquals(5 * minute, next.durationMs)
        assertEquals(1, next.completedFocuses)
        assertEquals("mat", next.subjectId)
        assertEquals(FocusRecord("mat", t0, t0 + 25 * minute, 25 * minute, 25 * minute, completed = true), record)
    }

    @Test
    fun quartoFocoLevaAPausaLongaQueReiniciaOCiclo() {
        val (longBreak, _) = PomodoroTimer.finish(PomodoroTimer.start(newFocus(completed = 3), t0), settings, t0)
        assertEquals(PomodoroPhase.LONG_BREAK, longBreak.phase)
        assertEquals(15 * minute, longBreak.durationMs)

        val (focus, record) = PomodoroTimer.finish(PomodoroTimer.start(longBreak, t0), settings, t0)
        assertEquals(PomodoroPhase.FOCUS, focus.phase)
        assertEquals(0, focus.completedFocuses)
        assertNull(record) // pausas não viram sessão
    }

    @Test
    fun pararFocoDepoisDeUmMinutoSalvaSessaoIncompleta() {
        val running = PomodoroTimer.start(newFocus(), t0)

        val (next, record) = PomodoroTimer.stop(running, settings, t0 + 12 * minute)

        assertFalse(next.isStarted)
        assertEquals(PomodoroPhase.FOCUS, next.phase)
        assertEquals(12 * minute, record?.focusedMs)
        assertEquals(false, record?.completed)
    }

    @Test
    fun pararFocoComMenosDeUmMinutoNaoSalvaNada() {
        val (_, record) = PomodoroTimer.stop(PomodoroTimer.start(newFocus(), t0), settings, t0 + 30_000)

        assertNull(record)
    }

    @Test
    fun pularPausaVoltaAoFoco() {
        val shortBreak = PomodoroTimer.idle(PomodoroPhase.SHORT_BREAK, settings, "mat", completedFocuses = 2)

        val next = PomodoroTimer.skipBreak(shortBreak, settings)

        assertEquals(PomodoroPhase.FOCUS, next.phase)
        assertEquals(2, next.completedFocuses)
    }

    @Test
    fun materiaEConfiguracoesSoMudamAntesDeComecar() {
        val running = PomodoroTimer.start(newFocus(), t0)
        val longerFocus = settings.copy(focusMinutes = 50)

        assertEquals("mat", PomodoroTimer.selectSubject(running, "outra").subjectId)
        assertEquals(25 * minute, PomodoroTimer.applySettings(running, longerFocus).durationMs)

        assertEquals("outra", PomodoroTimer.selectSubject(newFocus(), "outra").subjectId)
        assertEquals(50 * minute, PomodoroTimer.applySettings(newFocus(), longerFocus).durationMs)
    }
}
