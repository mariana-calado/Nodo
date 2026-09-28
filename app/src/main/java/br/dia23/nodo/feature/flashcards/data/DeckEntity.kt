package br.dia23.nodo.feature.flashcards.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Um baralho (deck) de flashcards.
 *
 * `@Entity` diz ao Room: "esta classe é uma tabela". Cada propriedade vira uma coluna.
 * (Analogia com Python: é como um dataclass que também descreve a tabela SQL.)
 */
@Entity(tableName = "decks")
data class DeckEntity(
    // UUID em String (e não Int autoincrement): dois aparelhos offline nunca geram o mesmo id,
    // então dá para sincronizar com o Firestore depois sem conflito de chaves.
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    // Toda alteração deve atualizar este campo. Na sincronização, "vence" quem tiver o updatedAt maior.
    val updatedAt: Long = createdAt,
    // Exclusão lógica: em vez de apagar a linha, marcamos como excluída.
    // Assim a exclusão também pode ser sincronizada com a nuvem.
    val isDeleted: Boolean = false,
)
