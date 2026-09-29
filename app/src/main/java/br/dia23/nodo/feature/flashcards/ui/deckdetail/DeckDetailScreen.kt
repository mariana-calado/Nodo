package br.dia23.nodo.feature.flashcards.ui.deckdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.feature.flashcards.data.CardEntity
import br.dia23.nodo.ui.theme.NodoTheme
import kotlinx.coroutines.launch

@Composable
fun DeckDetailRoute(
    onNavigateUp: () -> Unit,
    onAddCard: () -> Unit,
    onCardClick: (cardId: String) -> Unit,
    viewModel: DeckDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeckDetailScreen(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onAddCard = onAddCard,
        onCardClick = onCardClick,
        onDeleteCard = viewModel::onDeleteCard,
        onUndoDelete = viewModel::onUndoDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    uiState: DeckDetailUiState,
    onNavigateUp: () -> Unit,
    onAddCard: () -> Unit,
    onCardClick: (String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onUndoDelete: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // Escopo de coroutine ligado a esta tela: o Snackbar é "suspend" (espera o usuário agir ou o tempo acabar).
    val scope = rememberCoroutineScope()
    // stringResource só pode ser chamado durante a composição, então lemos os textos aqui fora do onClick.
    val deletedMessage = stringResource(R.string.card_deleted)
    val undoLabel = stringResource(R.string.action_undo)

    val deleteWithUndo: (String) -> Unit = { cardId ->
        onDeleteCard(cardId)
        scope.launch {
            // Se já havia um aviso na tela (exclusões seguidas), troca pelo novo em vez de enfileirar.
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = deletedMessage,
                actionLabel = undoLabel,
                // Long (~10 s): 4 s do Short é pouco para a pessoa perceber e tocar em "Desfazer".
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) onUndoDelete(cardId)
        }
    }

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
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.cards_add))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.cards.isEmpty() -> EmptyState(Modifier.align(Alignment.Center))
                else -> CardList(uiState.cards, onCardClick, deleteWithUndo)
            }
        }
    }
}

@Composable
private fun CardList(
    cards: List<CardEntity>,
    onCardClick: (String) -> Unit,
    onDeleteCard: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(cards, key = { it.id }) { card ->
            CardItem(card, onClick = { onCardClick(card.id) }, onDelete = { onDeleteCard(card.id) })
        }
    }
}

@Composable
private fun CardItem(card: CardEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    card.front,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    card.back,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            // Sem diálogo de confirmação: o Snackbar com "Desfazer" é mais rápido e igualmente seguro.
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.card_delete))
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.cards_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.cards_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeckDetailScreenPreview() {
    NodoTheme {
        DeckDetailScreen(
            uiState = DeckDetailUiState(
                deckName = "Inglês",
                cards = listOf(
                    CardEntity(deckId = "1", front = "cat", back = "gato"),
                    CardEntity(deckId = "1", front = "to look forward to", back = "estar ansioso por algo (bom)"),
                ),
                isLoading = false,
            ),
            onNavigateUp = {}, onAddCard = {}, onCardClick = {}, onDeleteCard = {}, onUndoDelete = {},
        )
    }
}
