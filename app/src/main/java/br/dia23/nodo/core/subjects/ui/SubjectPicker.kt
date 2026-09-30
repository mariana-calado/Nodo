package br.dia23.nodo.core.subjects.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.dia23.nodo.R
import br.dia23.nodo.core.subjects.SubjectEntity
import br.dia23.nodo.core.ui.ColorDot
import br.dia23.nodo.ui.theme.SubjectColors

/**
 * Chip que mostra a matéria escolhida e abre um menu com as matérias, "Sem matéria" e "Nova matéria".
 * Usado no Pomodoro, no diálogo de deck e no Planner: uma peça de interface, vários lugares.
 */
@Composable
fun SubjectPicker(
    selectedId: String?,
    subjects: List<SubjectEntity>,
    onSelect: (String?) -> Unit,
    onNewSubject: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    noneLabel: String = stringResource(R.string.pomodoro_no_subject),
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = subjects.find { it.id == selectedId }

    // O Box é a "âncora": o DropdownMenu abre logo abaixo do chip.
    Box(modifier) {
        AssistChip(
            onClick = { expanded = true },
            enabled = enabled,
            label = { Text(selected?.name ?: noneLabel) },
            leadingIcon = { ColorDot(selected?.colorIndex) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noneLabel) },
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
fun NewSubjectDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, colorIndex: Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var colorIndex by rememberSaveable { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pomodoro_new_subject)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_dialog_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.subject_dialog_color), style = MaterialTheme.typography.labelLarge)
                // 8 cores em 2 fileiras de 4.
                SubjectColors.indices.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { index ->
                            ColorOption(
                                index = index,
                                selected = index == colorIndex,
                                onClick = { colorIndex = index },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, colorIndex) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ColorOption(index: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.subject_color_option, index + 1)
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(SubjectColors[index])
            // Anel ao redor da cor escolhida.
            .then(
                if (selected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                },
            )
            // selectable + Role.RadioButton: o leitor de tela anuncia "Cor 3, selecionado", como um botão de opção.
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = description }
            .padding(4.dp),
    )
}
