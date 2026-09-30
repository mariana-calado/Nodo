package br.dia23.nodo.core.subjects

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Matérias ficam em `core` porque várias funcionalidades usam: decks, Pomodoro, Planner e estatísticas. */
@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
) {
    fun observeSubjects(): Flow<List<SubjectEntity>> = subjectDao.observeActive()

    suspend fun getSubject(id: String): SubjectEntity? = subjectDao.getById(id)

    /** Devolve a matéria criada para a tela já selecioná-la. */
    suspend fun createSubject(name: String, colorIndex: Int): SubjectEntity {
        val subject = SubjectEntity(name = name.trim(), colorIndex = colorIndex)
        subjectDao.upsert(subject)
        return subject
    }
}
