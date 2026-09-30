package br.dia23.nodo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta fixa para as matérias. A matéria guarda só o índice (SubjectEntity.colorIndex),
 * então dá para ajustar os tons aqui sem mexer no banco.
 * Tons médios, legíveis tanto no tema claro quanto no escuro.
 */
val SubjectColors = listOf(
    Color(0xFF4F7CAC), // azul
    Color(0xFF5E9E6E), // verde
    Color(0xFFD08C3E), // laranja
    Color(0xFFC0504D), // vermelho
    Color(0xFF8E6BBF), // roxo
    Color(0xFF3E9E9E), // turquesa
    Color(0xFFC46A9A), // rosa
    Color(0xFF8A7F5C), // oliva
)

/** `mod` evita erro se um índice fora da paleta chegar do banco (ex.: sincronizado de outra versão). */
fun subjectColor(index: Int): Color = SubjectColors[index.mod(SubjectColors.size)]
