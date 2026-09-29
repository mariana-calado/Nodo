# Nodo

App Android de estudo: flashcards com repetição espaçada (SM-2), timer Pomodoro, planner, quiz e resumo com IA.

> Projeto em desenvolvimento — feito para aprendizado e portfólio.

## Stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Room · Navigation Compose · WorkManager · Retrofit · Firebase (Auth + Firestore)

## Roadmap

- [ ] Fase 1 — Flashcards (decks, cartas, modo estudo, SM-2)
  - [x] Modelagem Room: `Deck` e `Card`
  - [x] Tela de decks: criar, listar, editar e excluir (MVVM + Hilt + Navigation)
- [ ] Fase 2 — Pomodoro + estatísticas
- [ ] Fase 3 — Planner e lembretes
- [ ] Fase 4 — Modo quiz
- [ ] Fase 5 — Resumo com IA (Cloud Functions)
- [ ] Fase 6 — Login com Google e sincronização

## Decisões de arquitetura

- Entidades do Room usam `id` UUID (String) e `updatedAt`, para sincronizar com o Firestore sem conflito de chaves.
- Exclusão lógica (`isDeleted`) em vez de `DELETE`, para que a exclusão também seja sincronizada.
- Um pacote por funcionalidade (`feature/flashcards`, ...) e código compartilhado em `core/`.
- MVVM com fluxo unidirecional: a tela desenha um `UiState` (StateFlow) e só envia eventos ao ViewModel.
- Injeção de dependências com Hilt; o Repository é a única porta de entrada para os dados.
