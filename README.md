# Nodo

App Android de estudo: flashcards com repetição espaçada (SM-2), timer Pomodoro, planner, quiz e resumo com IA.

> Projeto em desenvolvimento — feito para aprendizado e portfólio.

## Stack

Kotlin · Jetpack Compose · Material 3 · MVVM · Hilt · Room · DataStore · Navigation Compose · WorkManager · Retrofit · Firebase (Auth + Firestore)

## Roadmap

- [x] Fase 1 — Flashcards (decks, cartas, modo estudo, SM-2)
  - [x] Modelagem Room: `Deck` e `Card`
  - [x] Tela de decks: criar, listar, editar e excluir (MVVM + Hilt + Navigation)
  - [x] Cartas do deck: criar, editar e excluir com "Desfazer"
  - [x] Modo estudo com repetição espaçada (SM-2) e histórico de revisões
- [x] Fase 2 — Pomodoro + estatísticas
  - [x] Timer Pomodoro por matéria, com notificação e alarme de fim de fase
  - [x] Estatísticas de 7/30 dias: tempo de foco, acertos, gráficos por dia, por matéria e por deck
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
- Regras de negócio em Kotlin puro (`domain/`: algoritmo SM-2 e fila da sessão de estudo), cobertas por testes unitários JVM que rodam sem emulador.
- Migrações do banco versionadas: o schema de cada versão fica em `app/schemas/` e um teste instrumentado garante que a atualização preserva os dados.
- Timer do Pomodoro baseado em horários (não em contagem de segundos), persistido no DataStore: o tempo fica certo mesmo com o app fechado. Um controlador com escopo de aplicação finaliza as fases, e um alarme do sistema cobre o caso de o processo ter sido encerrado.
- Gráficos desenhados com o Canvas do Compose, sem biblioteca externa, com descrição para leitores de tela.
- Navegação por abas com barra inferior; cada funcionalidade registra o próprio grafo de navegação.

## Créditos

- Fonte [Montserrat](https://github.com/JulietaUla/Montserrat) — SIL Open Font License 1.1 (ver `licenses/Montserrat-OFL.txt`).
- Ícones [Material Symbols](https://fonts.google.com/icons) do Google — Apache License 2.0.
