package br.dia23.nodo.feature.stats.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.dia23.nodo.feature.stats.data.StatsRepository
import br.dia23.nodo.feature.stats.domain.StudyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class StatsPeriod(val days: Int) { WEEK(7), MONTH(30) }

data class StatsUiState(
    val period: StatsPeriod = StatsPeriod.WEEK,
    /** null enquanto carrega. */
    val stats: StudyStats? = null,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: StatsRepository,
) : ViewModel() {

    private val period = MutableStateFlow(StatsPeriod.WEEK)

    /**
     * flatMapLatest: quando o período muda, "desliga" a consulta antiga e liga uma nova.
     * (Um `map` comum não serve: cada período é um Flow diferente vindo do banco.)
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = period
        .flatMapLatest { selected -> repository.observeStats(selected.days).map { StatsUiState(selected, it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun onPeriodChange(newPeriod: StatsPeriod) {
        period.value = newPeriod
    }
}
