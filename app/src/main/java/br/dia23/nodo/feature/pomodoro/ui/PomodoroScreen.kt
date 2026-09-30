package br.dia23.nodo.feature.pomodoro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.core.ui.ColorDot
import br.dia23.nodo.core.ui.TopLevelScreenInsets
import br.dia23.nodo.core.ui.formatDuration
import br.dia23.nodo.feature.pomodoro.data.SubjectEntity
import br.dia23.nodo.feature.pomodoro.domain.PomodoroPhase
import br.dia23.nodo.feature.pomodoro.domain.PomodoroSettings
import br.dia23.nodo.ui.theme.NodoTheme
import java.util.Locale

@Composable
fun PomodoroRoute(viewModel: PomodoroViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Android 13+: pede a permissão de notificação no primeiro "Iniciar". Aceitando ou não, o timer começa.
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onStartOrResume()
    }
    val onStart = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onStartOrResume()
        }
    }

    PomodoroScreen(
        uiState = uiState,
        onStart = onStart,
        onPause = viewModel::onPause,
        onStop = viewModel::onStop,
        onSkipBreak = viewModel::onSkipBreak,
        onSelectSubject = viewModel::onSelectSubject,
        onCreateSubject = viewModel::onCreateSubject,
        onSaveSettings = viewModel::onSaveSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    uiState: PomodoroUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkipBreak: () -> Unit,
    onSelectSubject: (String?) -> Unit,
    onCreateSubject: (name: String, colorIndex: Int) -> Unit,
    onSaveSettings: (PomodoroSettings) -> Unit,
) {
    // Diálogos abertos: estado só de interface (um Boolean), então fica na tela com rememberSaveable.
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showNewSubject by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pomodoro_title)) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.pomodoro_settings),
                        )
                    }
                },
            )
        },
        contentWindowInsets = TopLevelScreenInsets,
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SubjectSelector(
                selected = uiState.selectedSubject,
                subjects = uiState.subjects,
                // Matéria só muda antes de começar (o tempo já corrido pertence à matéria escolhida).
                enabled = !uiState.isStarted,
                onSelect = onSelectSubject,
                onNewSubject = { showNewSubject = true },
            )

            val phaseColor = phaseColor(uiState.phase)
            Text(
                stringResource(uiState.phase.labelRes()),
                style = MaterialTheme.typography.titleLarge,
                color = phaseColor,
            )

            TimerRing(
                remainingFraction = uiState.remainingFraction,
                color = phaseColor,
                modifier = Modifier.size(260.dp),
            ) {
                Text(formatClock(uiState.remainingMs), style = MaterialTheme.typography.displayMedium)
            }

            CycleIndicator(
                completed = uiState.completedFocuses,
                total = uiState.settings.focusesBeforeLongBreak,
                color = MaterialTheme.colorScheme.primary,
            )

            Controls(uiState, onStart, onPause, onStop, onSkipBreak)

            Text(
                stringResource(R.string.pomodoro_today, formatDuration(uiState.focusedTodayMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showSettings) {
        PomodoroSettingsDialog(
            settings = uiState.settings,
            onDismiss = { showSettings = false },
            onSave = {
                onSaveSettings(it)
                showSettings = false
            },
        )
    }
    if (showNewSubject) {
        NewSubjectDialog(
            onDismiss = { showNewSubject = false },
            onCreate = { name, color ->
                onCreateSubject(name, color)
                showNewSubject = false
            },
        )
    }
}

/**
 * Anel desenhado à mão no Canvas: um círculo de fundo ("trilho") e um arco por cima
 * que vai diminuindo conforme o tempo passa. O conteúdo (os números) fica no centro.
 */
@Composable
private fun TimerRing(
    remainingFraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // O valor muda de segundo em segundo; animar em 1 s com velocidade constante deixa o arco contínuo.
    val animatedFraction by animateFloatAsState(
        targetValue = remainingFraction,
        animationSpec = tween(durationMillis = 1_000, easing = LinearEasing),
        label = "ring",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            // O traço é centrado na borda do arco: recuamos meia espessura para ele não ser cortado.
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)

            drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(strokeWidth))
            drawArc(
                color = color,
                startAngle = -90f, // 0° no Canvas é "3 horas"; -90° começa no topo, como um relógio
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }
        content()
    }
}

/** Bolinhas do ciclo: quantos focos já foram feitos antes da pausa longa. */
@Composable
private fun CycleIndicator(completed: Int, total: Int, color: Color) {
    val description = stringResource(R.string.pomodoro_cycle, (completed + 1).coerceAtMost(total), total)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (index < completed) color else MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(description, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun Controls(
    uiState: PomodoroUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onSkipBreak: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                uiState.isRunning -> FilledTonalButton(onClick = onPause) {
                    ButtonContent(R.drawable.ic_pause, R.string.pomodoro_pause)
                }
                else -> Button(onClick = onStart) {
                    ButtonContent(
                        R.drawable.ic_play_arrow,
                        if (uiState.isStarted) R.string.pomodoro_resume else R.string.pomodoro_start,
                    )
                }
            }
            if (uiState.isStarted) {
                OutlinedButton(onClick = onStop) { ButtonContent(R.drawable.ic_stop, R.string.pomodoro_stop) }
            }
        }
        if (uiState.phase != PomodoroPhase.FOCUS) {
            TextButton(onClick = onSkipBreak) { ButtonContent(R.drawable.ic_skip_next, R.string.pomodoro_skip_break) }
        }
    }
}

@Composable
private fun ButtonContent(icon: Int, label: Int) {
    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(stringResource(label))
}

@Composable
private fun SubjectSelector(
    selected: SubjectEntity?,
    subjects: List<SubjectEntity>,
    enabled: Boolean,
    onSelect: (String?) -> Unit,
    onNewSubject: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // O Box é a "âncora": o DropdownMenu abre logo abaixo do chip.
    Box {
        AssistChip(
            onClick = { expanded = true },
            enabled = enabled,
            label = { Text(selected?.name ?: stringResource(R.string.pomodoro_no_subject)) },
            leadingIcon = { ColorDot(selected?.colorIndex) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pomodoro_no_subject)) },
                leadingIcon = { ColorDot(null) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name) },
                    leadingIcon = { ColorDot(subject.colorIndex) },
                    onClick = {
                        expanded = false
                        onSelect(subject.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.pomodoro_new_subject)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                onClick = {
                    expanded = false
                    onNewSubject()
                },
            )
        }
    }
}

@Composable
private fun phaseColor(phase: PomodoroPhase): Color =
    if (phase == PomodoroPhase.FOCUS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

private fun PomodoroPhase.labelRes(): Int = when (this) {
    PomodoroPhase.FOCUS -> R.string.pomodoro_phase_focus
    PomodoroPhase.SHORT_BREAK -> R.string.pomodoro_phase_short_break
    PomodoroPhase.LONG_BREAK -> R.string.pomodoro_phase_long_break
}

/** "24:59". Arredonda para cima: com 0,4 s restante ainda mostra 00:01, não 00:00. */
private fun formatClock(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999) / 1_000
    return String.format(Locale.ROOT, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun PomodoroScreenPreview() {
    NodoTheme {
        PomodoroScreen(
            uiState = PomodoroUiState(
                isLoading = false,
                remainingMs = 14 * 60_000L + 32_000,
                durationMs = 25 * 60_000L,
                isStarted = true,
                isRunning = true,
                completedFocuses = 2,
                selectedSubject = SubjectEntity(name = "Cálculo", colorIndex = 2),
                focusedTodayMs = 75 * 60_000L,
            ),
            onStart = {}, onPause = {}, onStop = {}, onSkipBreak = {},
            onSelectSubject = {}, onCreateSubject = { _, _ -> }, onSaveSettings = {},
        )
    }
}
