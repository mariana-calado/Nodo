package br.dia23.nodo.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.dia23.nodo.R
import br.dia23.nodo.ui.theme.subjectColor

/**
 * Margens do sistema para as telas principais (as que têm a barra inferior).
 * A barra inferior já ocupa o espaço de baixo, inclusive o da barra de gestos do Android;
 * se a tela também reservasse esse espaço, sobraria um vão vazio.
 */
val TopLevelScreenInsets: WindowInsets
    @Composable get() = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

/** "45 min" ou "2 h 15 min". */
@Composable
fun formatDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

/** Bolinha com a cor da matéria; cinza quando não há matéria (colorIndex null). */
@Composable
fun ColorDot(colorIndex: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(colorIndex?.let(::subjectColor) ?: MaterialTheme.colorScheme.outline),
    )
}
