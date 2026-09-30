package br.dia23.nodo.feature.flashcards.ui.decks

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.dia23.nodo.R
import br.dia23.nodo.feature.flashcards.data.DeckEntity
import br.dia23.nodo.feature.flashcards.data.DeckWithStats
import br.dia23.nodo.ui.theme.NodoTheme

/**
 * Versão "com estado" da tela: conversa com o ViewModel e repassa tudo para a versão "sem estado".
 * Esse par (Route + Screen) é o padrão recomendado: a Screen fica fácil de testar e de pré-visualizar.
 */
@Composable
fun DeckListRoute(
    onDeckClick: (deckId: String) -> Unit,
    viewModel: DeckListViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle: só coleta enquanto a tela está visível (poupa bateria/CPU).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DeckListScreen(
        uiState = uiState,
        onDeckClick = onDeckClick,
        onAddClick = viewModel::onAddClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDialog = viewModel::onDismissDialog,
        onSaveDeck = viewModel::onSaveDeck,
        onConfirmDelete = viewModel::onConfirmDelete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckListScreen(
    uiState: DeckListUiState,
    onDeckClick: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditClick: (DeckEntity) -> Unit,
    onDeleteClick: (DeckEntity) -> Unit,
    onDismissDialog: () -> Unit,
    onSaveDeck: (name: String, description: String) -> Unit,
    onConfirmDelete: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.decks_title)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                // Ícones vêm de res/drawable (vetores XML do Material). A antiga lib material-icons
                // não vem mais junto do material3 e o Google recomenda usar os drawables.
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.decks_add))
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.decks.isEmpty() -> EmptyState(Modifier.align(Alignment.Center))
                else -> DeckList(uiState.decks, onDeckClick, onEditClick, onDeleteClick)
            }
        }
    }

    // Diálogos: aparecem por cima conforme o estado (a tela nunca "abre" nada por conta própria).
    when (val dialog = uiState.dialog) {
        DeckDialog.None -> Unit
        DeckDialog.Create -> DeckEditDialog(deck = null, onDismiss = onDismissDialog, onSave = onSaveDeck)
        is DeckDialog.Edit -> DeckEditDialog(deck = dialog.deck, onDismiss = onDismissDialog, onSave = onSaveDeck)
        is DeckDialog.ConfirmDelete -> ConfirmDeleteDialog(
            deckName = dialog.deck.name,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDialog,
        )
    }
}

@Composable
private fun DeckList(
    decks: List<DeckWithStats>,
    onDeckClick: (String) -> Unit,
    onEditClick: (DeckEntity) -> Unit,
    onDeleteClick: (DeckEntity) -> Unit,
) {
    LazyColumn(
        // Espaço no fim para o botão "+" não cobrir o último item.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // `key` deixa o Compose identificar cada item pelo id (animações e estado corretos ao reordenar).
        items(decks, key = { it.deck.id }) { item ->
            DeckItem(item, onDeckClick, onEditClick, onDeleteClick)
        }
    }
}

@Composable
private fun DeckItem(
    item: DeckWithStats,
    onDeckClick: (String) -> Unit,
    onEditClick: (DeckEntity) -> Unit,
    onDeleteClick: (DeckEntity) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(onClick = { onDeckClick(item.deck.id) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.deck.name, style = MaterialTheme.typography.titleMedium)
                if (item.deck.description.isNotBlank()) {
                    Text(
                        item.deck.description,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // 1º argumento escolhe singular/plural; o 2º preenche o %d.
                val cards = pluralStringResource(R.plurals.deck_card_count, item.cardCount, item.cardCount)
                val summary = if (item.dueCount > 0) {
                    "$cards · ${stringResource(R.string.deck_due, item.dueCount)}"
                } else {
                    cards
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.deck_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.deck_rename)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                        onClick = { menuOpen = false; onEditClick(item.deck) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.deck_delete)) },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                        onClick = { menuOpen = false; onDeleteClick(item.deck) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.decks_empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.decks_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ConfirmDeleteDialog(deckName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.deck_delete_title)) },
        text = { Text(stringResource(R.string.deck_delete_message, deckName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.deck_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

// --- Previews: só existem para o Android Studio desenhar a tela sem rodar o app ---

private fun previewDeck(name: String, description: String, cards: Int, due: Int) =
    DeckWithStats(DeckEntity(name = name, description = description), cardCount = cards, dueCount = due)

@Preview(showBackground = true)
@Composable
private fun DeckListScreenPreview() {
    NodoTheme {
        DeckListScreen(
            uiState = DeckListUiState(
                decks = listOf(
                    previewDeck("Inglês", "Vocabulário do dia a dia", cards = 24, due = 5),
                    previewDeck("Kotlin", "", cards = 1, due = 0),
                ),
                isLoading = false,
            ),
            onDeckClick = {}, onAddClick = {}, onEditClick = {}, onDeleteClick = {},
            onDismissDialog = {}, onSaveDeck = { _, _ -> }, onConfirmDelete = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeckListScreenEmptyPreview() {
    NodoTheme {
        DeckListScreen(
            uiState = DeckListUiState(isLoading = false),
            onDeckClick = {}, onAddClick = {}, onEditClick = {}, onDeleteClick = {},
            onDismissDialog = {}, onSaveDeck = { _, _ -> }, onConfirmDelete = {},
        )
    }
}
