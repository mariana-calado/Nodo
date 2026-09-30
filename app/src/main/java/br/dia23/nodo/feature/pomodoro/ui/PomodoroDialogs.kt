package br.dia23.nodo.feature.pomodoro.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.dia23.nodo.R
import br.dia23.nodo.feature.pomodoro.domain.PomodoroSettings
import kotlin.math.roundToInt

@Composable
fun PomodoroSettingsDialog(
    settings: PomodoroSettings,
    onDismiss: () -> Unit,
    onSave: (PomodoroSettings) -> Unit,
) {
    // Cópia local enquanto a pessoa mexe nos sliders; só vira configuração ao tocar em Salvar.
    var focus by rememberSaveable { mutableIntStateOf(settings.focusMinutes) }
    var shortBreak by rememberSaveable { mutableIntStateOf(settings.shortBreakMinutes) }
    var longBreak by rememberSaveable { mutableIntStateOf(settings.longBreakMinutes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pomodoro_settings)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MinutesSlider(stringResource(R.string.pomodoro_settings_focus, focus), focus, 5..60, step = 5) {
                    focus = it
                }
                MinutesSlider(stringResource(R.string.pomodoro_settings_short_break, shortBreak), shortBreak, 1..15) {
                    shortBreak = it
                }
                MinutesSlider(stringResource(R.string.pomodoro_settings_long_break, longBreak), longBreak, 5..30, step = 5) {
                    longBreak = it
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(settings.copy(focusMinutes = focus, shortBreakMinutes = shortBreak, longBreakMinutes = longBreak))
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun MinutesSlider(
    label: String,
    value: Int,
    range: IntRange,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    Slider(
        value = value.toFloat(),
        onValueChange = { onValueChange(((it / step).roundToInt() * step).coerceIn(range)) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        // `steps` = quantidade de paradas ENTRE as pontas (ex.: 5..60 de 5 em 5 tem 10 paradas no meio).
        steps = (range.last - range.first) / step - 1,
    )
}
