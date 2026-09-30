package br.dia23.nodo.feature.flashcards.ui.decks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.dia23.nodo.R
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.subjects.ui.NewSubjectDialog
import br.dia23.nodo.core.subjects.ui.SubjectPicker
import br.dia23.nodo.feature.flashcards.data.DeckEntity

/**
 * Diálogo único para criar (deck = null) e editar (deck != null).
 *
 * O texto digitado fica AQUI (estado local da UI), não no ViewModel: só vira "dado do app"
 * quando o usuário toca em Salvar. `rememberSaveable` mantém o texto ao girar a tela.
 */
@Composable
fun DeckEditDialog(
    deck: DeckEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, subjectId: String?) -> Unit,
    onCreateSubject: (name: String, colorIndex: Int, onCreated: (String) -> Unit) -> Unit,
) {
    // A chave (deck?.id) reinicia os campos se o diálogo for reaberto para outro deck.
    var name by rememberSaveable(deck?.id) { mutableStateOf(deck?.name.orEmpty()) }
    var description by rememberSaveable(deck?.id) { mutableStateOf(deck?.description.orEmpty()) }
    var subjectId by rememberSaveable(deck?.id) { mutableStateOf(deck?.subjectId) }
    var showNewSubject by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (deck == null) R.string.deck_dialog_title_create else R.string.deck_dialog_title_edit,
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.deck_dialog_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.deck_dialog_description)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
                Text(
                    stringResource(R.string.deck_dialog_subject),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
                SubjectPicker(
                    selectedId = subjectId,
                    subjects = subjects,
                    onSelect = { subjectId = it },
                    onNewSubject = { showNewSubject = true },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, description, subjectId) },
                // Nome em branco não é permitido: o botão fica desabilitado.
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )

    // Um diálogo pode abrir outro por cima: o de nova matéria aparece sobre o do deck.
    if (showNewSubject) {
        NewSubjectDialog(
            onDismiss = { showNewSubject = false },
            onCreate = { newName, colorIndex ->
                showNewSubject = false
                onCreateSubject(newName, colorIndex) { newId -> subjectId = newId }
            },
        )
    }
}
