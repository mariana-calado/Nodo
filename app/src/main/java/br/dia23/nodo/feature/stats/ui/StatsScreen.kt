package br.dia23.nodo.feature.stats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.core.ui.ColorDot
import br.dia23.nodo.core.ui.TopLevelScreenInsets
import br.dia23.nodo.core.ui.formatDuration
import br.dia23.nodo.feature.stats.domain.DayStats
import br.dia23.nodo.feature.stats.domain.DeckStats
import br.dia23.nodo.feature.stats.domain.StudyStats
import br.dia23.nodo.feature.stats.domain.SubjectStats
import br.dia23.nodo.ui.theme.NodoTheme
import br.dia23.nodo.ui.theme.subjectColor
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun StatsRoute(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(uiState = uiState, onPeriodChange = viewModel::onPeriodChange)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(uiState: StatsUiState, onPeriodChange: (StatsPeriod) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.stats_title)) }) },
        contentWindowInsets = TopLevelScreenInsets,
    ) { innerPadding ->
        val stats = uiState.stats
        if (stats == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PeriodSelector(uiState.period, onPeriodChange) }

            if (stats.isEmpty) {
                item { EmptyState() }
                return@LazyColumn
            }

            item { SummaryGrid(stats) }
            item {
                SectionCard(stringResource(R.string.stats_focus_per_day)) {
                    FocusChart(stats.days)
                }
            }
            item {
                SectionCard(stringResource(R.string.stats_reviews_per_day)) {
                    ReviewsChart(stats.days)
                }
            }
            if (stats.bySubject.isNotEmpty()) {
                item { SectionCard(stringResource(R.string.stats_by_subject)) { SubjectBreakdown(stats.bySubject) } }
            }
            if (stats.byDeck.isNotEmpty()) {
                item { SectionCard(stringResource(R.string.stats_by_deck)) { DeckBreakdown(stats.byDeck) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(selected: StatsPeriod, onPeriodChange: (StatsPeriod) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        StatsPeriod.entries.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onPeriodChange(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsPeriod.entries.size),
            ) {
                Text(
                    stringResource(
                        if (period == StatsPeriod.WEEK) R.string.stats_period_week else R.string.stats_period_month,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SummaryGrid(stats: StudyStats) {
    val accuracy = stats.accuracy?.let { stringResource(R.string.percent, (it * 100).roundToInt()) }
        ?: stringResource(R.string.stats_no_value)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.stats_focus_time), formatDuration(stats.totalFocusedMs), Modifier.weight(1f))
            StatTile(stringResource(R.string.stats_sessions), stats.sessionCount.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.stats_reviews), stats.totalReviews.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.stats_accuracy), accuracy, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    OutlinedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun FocusChart(days: List<DayStats>) {
    val color = MaterialTheme.colorScheme.primary
    BarChart(
        bars = days.map { day ->
            Bar(dayLabel(day.date, days.size), listOf(BarSegment(day.focusedMs / 60_000f, color)))
        },
        contentDescription = stringResource(R.string.stats_chart_focus_description),
        maxValueLabel = { "${it.roundToInt()}" },
    )
}

@Composable
private fun ReviewsChart(days: List<DayStats>) {
    val correctColor = MaterialTheme.colorScheme.primary
    val wrongColor = MaterialTheme.colorScheme.error
    BarChart(
        bars = days.map { day ->
            Bar(
                dayLabel(day.date, days.size),
                listOf(
                    BarSegment(day.correctReviews.toFloat(), correctColor),
                    BarSegment((day.reviews - day.correctReviews).toFloat(), wrongColor),
                ),
            )
        },
        contentDescription = stringResource(R.string.stats_chart_reviews_description),
        maxValueLabel = { "${it.roundToInt()}" },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(correctColor, stringResource(R.string.stats_legend_correct))
        LegendItem(wrongColor, stringResource(R.string.stats_legend_wrong))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SubjectBreakdown(subjects: List<SubjectStats>) {
    val max = subjects.maxOf { it.focusedMs }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        subjects.forEach { subject ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(subject.colorIndex)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subject.name ?: stringResource(R.string.pomodoro_no_subject),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatDuration(subject.focusedMs), style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { subject.focusedMs.toFloat() / max },
                    color = subject.colorIndex?.let(::subjectColor) ?: MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DeckBreakdown(decks: List<DeckStats>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        decks.forEach { deck ->
            val percent = (deck.accuracy * 100).roundToInt()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    deck.name ?: stringResource(R.string.stats_deleted_deck),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    pluralStringResource(R.plurals.stats_deck_reviews, deck.reviews, deck.reviews, percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(progress = { deck.accuracy }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.stats_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.stats_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 7 dias: "seg", "ter"...; 30 dias: o número do dia ("28"). */
private fun dayLabel(date: LocalDate, periodDays: Int): String =
    if (periodDays <= 7) {
        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).removeSuffix(".")
    } else {
        date.dayOfMonth.toString()
    }

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun StatsScreenPreview() {
    val today = LocalDate.of(2026, 9, 29)
    val minute = 60_000L
    val focus = listOf(50, 25, 0, 75, 100, 25, 50)
    val reviews = listOf(10, 4, 0, 12, 20, 6, 15)
    NodoTheme {
        StatsScreen(
            uiState = StatsUiState(
                stats = StudyStats(
                    days = (0 until 7).map { i ->
                        DayStats(today.minusDays(6L - i), focus[i] * minute, reviews[i], (reviews[i] * 0.8).toInt())
                    },
                    sessionCount = 13,
                    bySubject = listOf(
                        SubjectStats("1", "Cálculo", 0, 200 * minute),
                        SubjectStats("2", "Inglês", 4, 100 * minute),
                        SubjectStats(null, null, null, 25 * minute),
                    ),
                    byDeck = listOf(DeckStats("d", "Inglês", 50, 41), DeckStats("e", "Kotlin", 17, 12)),
                ),
            ),
            onPeriodChange = {},
        )
    }
}
