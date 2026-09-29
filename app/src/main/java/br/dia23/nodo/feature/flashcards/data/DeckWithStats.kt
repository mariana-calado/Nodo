package br.dia23.nodo.feature.flashcards.data

import androidx.room.Embedded

/**
 * Deck + números calculados pelo banco, para a lista de decks.
 * Não é uma tabela: é só o formato do resultado de uma query (ver DeckDao.observeDecksWithStats).
 */
data class DeckWithStats(
    // @Embedded "achata" as colunas do deck dentro deste objeto.
    @Embedded val deck: DeckEntity,
    /** Total de cartas ativas do deck. */
    val cardCount: Int,
    /** Cartas ativas que já venceram e estão prontas para revisão. */
    val dueCount: Int,
)
