package br.dia23.nodo.feature.flashcards.ui.study

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.feature.flashcards.domain.ReviewGrade
import br.dia23.nodo.ui.theme.NodoTheme
import kotlin.math.roundToInt

@Composable
fun StudyRoute(
    onNavigateUp: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudyScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onFlip = viewModel::onFlip,
        onAnswer = viewModel::onAnswer,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    uiState: StudyUiState,
    onNavigateUp: () -> Unit,
    onFlip: () -> Unit,
    onAnswer: (ReviewGrade) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deckName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_navigate_up),
                        )
                    }
                },
                actions = {
                    if (uiState.card != null) {
                        Text(
                            stringResource(R.string.study_progress, uiState.completed, uiState.total),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.total > 0) {
                // Anima a barra entre um valor e outro, em vez de "pular".
                val progress by animateFloatAsState(
                    targetValue = uiState.completed.toFloat() / uiState.total,
                    label = "progress",
                )
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }

            val card = uiState.card
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                card == null -> SessionSummary(uiState.total, uiState.summary, onDone = onNavigateUp)
                else -> StudyContent(card, uiState, onFlip, onAnswer)
            }
        }
    }
}

@Composable
private fun StudyContent(
    card: CardEntity,
    uiState: StudyUiState,
    onFlip: () -> Unit,
    onAnswer: (ReviewGrade) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // key(turn): a cada resposta o FlipCard é recriado do zero, já na frente e SEM animar de volta.
        // Sem isso, a animação de "desvirar" mostraria por um instante o verso da próxima carta.
        key(uiState.turn) {
            FlipCard(
                front = card.front,
                back = card.back,
                isFlipped = uiState.isFlipped,
                onClick = onFlip,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }

        // Altura fixa: o botão único e a fileira de 4 ocupam o mesmo espaço, então a carta não muda de tamanho.
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            if (uiState.isFlipped) {
                GradeButtons(uiState.nextIntervals, onAnswer)
            } else {
                Button(onClick = onFlip, modifier = Modifier.fillMaxSize()) {
                    Text(stringResource(R.string.study_show_answer))
                }
            }
        }
    }
}

/**
 * Carta que gira no eixo Y (efeito 3D) entre frente e verso.
 * Na primeira metade do giro vemos a frente; depois dos 90° desenhamos o verso.
 */
@Composable
private fun FlipCard(
    front: String,
    back: String,
    isFlipped: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip",
    )
    val showingBack = rotation > 90f

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            // Afasta a "câmera": sem isso a perspectiva distorce demais a carta durante o giro.
            cameraDistance = 12f * density
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                // Depois de girar 180°, o conteúdo ficaria espelhado; girar o verso mais 180° o desvira.
                .graphicsLayer { rotationY = if (showingBack) 180f else 0f },
            contentAlignment = Alignment.Center,
        ) {
            if (showingBack) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        front,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    HorizontalDivider(Modifier.width(48.dp))
                    Text(back, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                }
            } else {
                Text(front, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun GradeButtons(nextIntervals: Map<ReviewGrade, Int>, onAnswer: (ReviewGrade) -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReviewGrade.entries.forEach { grade ->
            val (container, content) = gradeColors(grade)
            Button(
                onClick = { onAnswer(grade) },
                colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(grade.labelRes()), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    nextIntervals[grade]?.let { days ->
                        Text(formatInterval(days), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummary(total: Int, summary: Map<ReviewGrade, Int>, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (total == 0) {
            Text(stringResource(R.string.study_nothing_due), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.study_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Text(stringResource(R.string.study_finished_title), style = MaterialTheme.typography.headlineSmall)
            Text(
                pluralStringResource(R.plurals.study_reviewed_count, total, total),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            ReviewGrade.entries.forEach { grade ->
                Row(
                    modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(grade.labelRes()), style = MaterialTheme.typography.bodyLarge)
                    Text("${summary[grade] ?: 0}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Button(onClick = onDone, modifier = Modifier.padding(top = 32.dp)) {
            Text(stringResource(R.string.study_back_to_deck))
        }
    }
}

@StringRes
private fun ReviewGrade.labelRes(): Int = when (this) {
    ReviewGrade.AGAIN -> R.string.grade_again
    ReviewGrade.HARD -> R.string.grade_hard
    ReviewGrade.GOOD -> R.string.grade_good
    ReviewGrade.EASY -> R.string.grade_easy
}

/** Cores do tema (claro/escuro) para cada resposta; "Bom", a mais comum, recebe a cor principal. */
@Composable
private fun gradeColors(grade: ReviewGrade): Pair<Color, Color> {
    val colors = MaterialTheme.colorScheme
    return when (grade) {
        ReviewGrade.AGAIN -> colors.errorContainer to colors.onErrorContainer
        ReviewGrade.HARD -> colors.tertiaryContainer to colors.onTertiaryContainer
        ReviewGrade.GOOD -> colors.primary to colors.onPrimary
        ReviewGrade.EASY -> colors.secondaryContainer to colors.onSecondaryContainer
    }
}

/** "6 d" até 29 dias; a partir de 30, em meses ("2 m"). */
@Composable
private fun formatInterval(days: Int): String =
    if (days < 30) {
        stringResource(R.string.interval_days, days)
    } else {
        stringResource(R.string.interval_months, (days / 30.0).roundToInt())
    }

// --- Previews ---

private val previewCard = CardEntity(deckId = "1", front = "to look forward to", back = "estar ansioso por algo")

@Preview(showBackground = true)
@Composable
private fun StudyFrontPreview() {
    NodoTheme {
        StudyScreen(
            uiState = StudyUiState(isLoading = false, deckName = "Inglês", card = previewCard, completed = 2, total = 10),
            onNavigateUp = {}, onFlip = {}, onAnswer = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyBackPreview() {
    NodoTheme {
        StudyScreen(
            uiState = StudyUiState(
                isLoading = false, deckName = "Inglês", card = previewCard, isFlipped = true,
                completed = 2, total = 10,
                nextIntervals = mapOf(
                    ReviewGrade.AGAIN to 1, ReviewGrade.HARD to 15, ReviewGrade.GOOD to 15, ReviewGrade.EASY to 16,
                ),
            ),
            onNavigateUp = {}, onFlip = {}, onAnswer = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StudySummaryPreview() {
    NodoTheme {
        StudyScreen(
            uiState = StudyUiState(
                isLoading = false, deckName = "Inglês", total = 10, completed = 10,
                summary = mapOf(ReviewGrade.AGAIN to 2, ReviewGrade.GOOD to 7, ReviewGrade.EASY to 1),
            ),
            onNavigateUp = {}, onFlip = {}, onAnswer = {},
        )
    }
}
