package br.dia23.nodo.feature.flashcards.ui.cardedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import br.dia23.nodo.R
import br.dia23.nodo.ui.theme.NodoTheme

@Composable
fun CardEditRoute(
    onNavigateUp: () -> Unit,
    viewModel: CardEditViewModel = hiltViewModel(),
) {
    // LaunchedEffect roda o bloco quando a chave muda. Quando isSaved vira true, voltamos uma tela.
    LaunchedEffect(viewModel.isSaved) {
        if (viewModel.isSaved) onNavigateUp()
    }

    CardEditScreen(
        isNewCard = viewModel.isNewCard,
        isLoading = viewModel.isLoading,
        front = viewModel.front,
        back = viewModel.back,
        canSave = viewModel.canSave,
        onFrontChange = viewModel::onFrontChange,
        onBackChange = viewModel::onBackChange,
        onSave = viewModel::onSave,
        onSaveAndNew = viewModel::onSaveAndNew,
        onNavigateUp = onNavigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditScreen(
    isNewCard: Boolean,
    isLoading: Boolean,
    front: String,
    back: String,
    canSave: Boolean,
    onFrontChange: (String) -> Unit,
    onBackChange: (String) -> Unit,
    onSave: () -> Unit,
    onSaveAndNew: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    // FocusRequester = "controle remoto" do foco de um campo: permite pedir o foco por código.
    val frontFocus = remember { FocusRequester() }

    // Carta nova: já abre com o cursor na "Frente" (e o teclado aparece).
    LaunchedEffect(Unit) {
        if (isNewCard) frontFocus.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isNewCard) R.string.card_edit_title_create else R.string.card_edit_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_navigate_up),
                        )
                    }
                },
                // "Salvar" fica na barra do topo (padrão do Material para formulários em tela cheia):
                // sempre visível, nunca coberto pelo teclado.
                actions = {
                    TextButton(onClick = onSave, enabled = canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                // O Scaffold já descontou as barras do sistema; consumeWindowInsets evita descontar de novo,
                // e imePadding sobe o conteúdo quando o teclado abre (o app é edge-to-edge).
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)

            OutlinedTextField(
                value = front,
                onValueChange = onFrontChange,
                label = { Text(stringResource(R.string.card_front)) },
                minLines = 3,
                keyboardOptions = keyboardOptions,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(frontFocus),
            )
            OutlinedTextField(
                value = back,
                onValueChange = onBackChange,
                label = { Text(stringResource(R.string.card_back)) },
                minLines = 3,
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )

            if (isNewCard) {
                OutlinedButton(
                    onClick = {
                        onSaveAndNew()
                        frontFocus.requestFocus() // volta o cursor para a "Frente" da próxima carta
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.card_save_and_new)) }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardEditScreenPreview() {
    NodoTheme {
        CardEditScreen(
            isNewCard = true, isLoading = false, front = "cat", back = "gato", canSave = true,
            onFrontChange = {}, onBackChange = {}, onSave = {}, onSaveAndNew = {}, onNavigateUp = {},
        )
    }
}
