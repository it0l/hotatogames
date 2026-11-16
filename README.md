# 🥔 Batata Quente Matemática

Jogo Android em **Kotlin e Jetpack Compose**, desenvolvido como trabalho final da disciplina de Android. De 2 a 6 pessoas jogam presencialmente no mesmo celular, resolvendo contas antes que a batata exploda.

**Integrantes:** Lucas, Rafael e Pedro.

## Como jogar

1. Na tela inicial, escolha **Jogar**.
2. Cadastre os jogadores e selecione de **2 a 6** participantes.
3. Configure a dificuldade, o tempo por rodada (20–60 segundos) e as vidas (1–3).
4. O jogador da vez responde à conta. Se acertar, recebe um acerto e passa a batata; se errar, continua com ela.
5. Quando o tempo acabar, quem estiver com a batata perde uma vida.
6. A partida termina quando restar um jogador com vidas. A pontuação é estilo fliperama: cada acerto vale 100/200/300 pontos (Fácil/Médio/Difícil), responder em até 3 s dobra o valor, 3 acertos seguidos dão combo x2 e 5 seguidos dão x3. O vencedor ganha +500 e +100 por vida restante. As partidas vão para o ranking online no Firebase (login anônimo, sem cadastro: o jogador é identificado pelo nome).

As operações incluem soma, subtração, multiplicação e divisão exata. O sorteio mistura questões cadastradas com questões geradas e **não repete a mesma operação na partida** (por exemplo, `7 × 8` e `8 × 7` são equivalentes).

## Telas

| Tela | Função |
|---|---|
| Inicial | Escolher jogar ou administrar |
| Jogadores | Cadastrar, selecionar, editar e excluir participantes |
| Configurações | Dificuldade, tempo e vidas |
| Jogo | Questão, resposta, cronômetro, batata e vidas |
| Resultado | Vencedor, rodadas e pontos |
| Ranking | Pontos, vitórias e partidas salvos localmente |
| Login Admin | Login de demonstração ou login Firebase |
| Painel Admin | CRUD de questões e sincronização com Firebase |

Há uma **Bottom Navigation Bar** para Jogadores, Configurações e Ranking. As listagens usam `LazyColumn`; as demais telas usam `Column` com `verticalScroll` para permitir rolagem no modo retrato e paisagem.

## Tecnologias e arquitetura

- Kotlin + Jetpack Compose / Material 3
- Navigation Compose
- **MVVM:** `UiState` / `StateFlow`, três ViewModels e três ViewModelFactories
- **Room:** duas entidades, dois DAOs e CRUD completo
- `Flow<List<...>>` para listas atualizadas automaticamente
- Filtros e ordenação com `filter`, `map` e `sortedByDescending`
- **Firebase Authentication + Firestore** para questões remotas
- `try-catch` nos Repositories para falhas de banco e de integração externa

Estrutura principal:

```text
app/src/main/java/com/example/batataquente/
├── MainActivity.kt
├── navigation/AppNavigation.kt
├── ui/
│   ├── telas/          (8 telas Compose + Moldura)
│   ├── jogo/           (JogoViewModel, JogoState e Factory)
│   ├── jogadores/      (JogadoresViewModel, JogadoresState e Factory)
│   └── admin/          (AdminViewModel, AdminState e Factory)
├── data/
│   ├── local/          (AppDatabase, Jogador/Questao Entity e DAO)
│   ├── repository/     (JogadorRepository e QuestaoRepository)
│   └── remote/         (FirebaseQuestaoDataSource)
└── logic/              (RegrasJogo e BaralhoQuestoes)
```

Fluxo: **Tela → ViewModel → Repository → DAO/Room ou Firebase → UiState → Tela**.

### Banco e CRUD

O Room utiliza `JogadorEntity` (`nome`, `pontos`, `vitorias`, `partidas`) e `QuestaoEntity` (`pergunta`, `resposta`, `dificuldade`, `origem`, `firebaseId`). Ambas possuem métodos de criar, consultar, atualizar e excluir. As consultas de listas são expostas por `Flow`.

A migração da versão 1 para a versão 2 do Room adiciona o campo `firebaseId` sem apagar o banco anterior.

## Rotação, rolagem e persistência

- **Rolagem:** `Moldura.kt` utiliza `verticalScroll` para formulários e `LazyColumn` para listas. O teclado é considerado com `imePadding()`.
- **Girar a tela:** `JogoViewModel` mantém o cronômetro, a questão atual, o jogador e as vidas durante a mudança de orientação. A seleção de jogadores também permanece no `JogadoresViewModel`.
- **Campos de texto e configurações:** usam `rememberSaveable` para restaurar os valores ao girar a tela.
- **Dados permanentes:** Room armazena os jogadores, questões locais e pontuações, inclusive após fechar e abrir o aplicativo.

**Limite intencional:** a partida em andamento é mantida durante rotação, mas não é prometida recuperação caso o Android encerre completamente o processo do aplicativo. Isso evita serialização manual de JSON e mantém o código no padrão MVVM aprendido em aula.

## Firebase

O projeto já contém o código da integração, mas **Firestore, Authentication, regras e usuário administrador devem ser configurados no Firebase Console**, e a conexão real ainda precisa ser testada no Android Studio.

1. No [Firebase Console](https://console.firebase.google.com/), abra o projeto `batataquente-85eeb`.
2. Em **Authentication**, ative o provedor **E-mail/senha** e crie uma conta de administrador.
3. Em **Firestore Database**, crie o banco, se ainda não existir.
4. Crie uma coleção `admins` e um documento cujo ID seja o **UID** do usuário autorizado.
5. Publique as regras presentes em [`firestore.rules`](firestore.rules). Elas permitem leitura de questões e exigem um administrador autorizado para escrita.
6. Baixe o **`google-services.json` original** do console e substitua o arquivo em `app/google-services.json` (a versão do pacote foi digitada a partir de uma imagem).
7. Abra o aplicativo, entre em Administrador com a conta Firebase e teste salvar uma questão e sincronizá-la.

**Modo local:** o login de demonstração (`1234`) modifica somente as questões do Room. Não concede acesso de escrita ao Firebase. Quando estiver sem internet, o jogo continua usando o Room e a geração local de questões.

## Abrir e executar

1. Abra a pasta `BatataQuenteMatematica` no Android Studio.
2. Mantenha a configuração de JDK compatível com Gradle 9.1 (o projeto usa JDK 25 no Android Studio e compila o aplicativo para Java 17).
3. Confirme o Gradle Wrapper completo: `gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar`. Caso faltem, preserve-os de uma cópia funcional do projeto Android Studio.
4. Substitua `app/google-services.json` pelo arquivo original do Firebase Console.
5. Faça **Sync Project with Gradle Files → Build → Run** em emulador ou celular Android (API 26 ou superior).

## Testes e entrega

Confira `COMO_USAR_O_ZIP.md` para atualizar a cópia do GitHub e `TESTES_MANUAIS.md` para testar cadastro, CRUD, jogo, contas não repetidas, rolagem, rotação e Firebase. O projeto deve ser enviado **com todos os arquivos necessários para execução** em um repositório GitHub.

**Imagens e vídeo de demonstração:** ainda serão adicionados antes da entrega, conforme as instruções do professor. O trabalho também exige defesa individual e participação dos integrantes nos commits.

Confira também `VERIFICACAO_DESTA_VERSAO.md` para ver o que foi efetivamente verificado.

Consulte `GUIA_DEFESA.md` para uma explicação simples do código que cada integrante deve entender.

## Organização da apresentação

- **Lucas:** jogo, regras, cronômetro, perguntas e navegação.
- **Rafael:** entidades, DAOs, Room, CRUD, Flow e ranking.
- **Pedro:** tela administrativa, questões, Repository e Firebase.

Essa divisão identifica os assuntos de estudo e demonstração, não atribui a autoria de código ou commits a alguém que não tenha contribuído.
