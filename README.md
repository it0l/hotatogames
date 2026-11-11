# 🥔 Batata Quente Matemática

**Um jogo de matemática, velocidade e estratégia para Android.**

Batata Quente Matemática é um minijogo desenvolvido em **Kotlin** com **Jetpack Compose**, no qual os participantes precisam resolver operações matemáticas e passar a batata antes que o tempo acabe. O projeto foi criado como trabalho final da disciplina de Desenvolvimento Android, aplicando conceitos de **MVVM, Navigation, Room, CRUD, Flow e integração externa**.

**Equipe:** Lucas, Rafael e Pedro  
**Plataforma:** Android  
**Modo de jogo:** presencial, em um único dispositivo  
**Estado do projeto:** versão local jogável; integração com Firebase ainda pendente

---

## 🎮 Como funciona o jogo

1. Na tela inicial, selecione **Jogar**.
2. Cadastre os participantes e selecione de **2 a 6 jogadores**.
3. Configure a dificuldade (**Fácil, Médio ou Difícil**), o tempo de cada rodada (**20 a 60 segundos**) e a quantidade de vidas (**1 a 3**).
4. Ao começar, o jogador da vez recebe uma operação matemática.
5. **Acertou:** ganha um acerto e passa a batata para o próximo participante ativo. Uma nova pergunta é exibida.
6. **Errou:** permanece com a batata e precisa tentar novamente. O tempo continua correndo.
7. Quando o cronômetro chega a zero, quem estiver com a batata **perde uma vida**.
8. Novas rodadas acontecem até restar apenas **um vencedor**.
9. Ao concluir a partida, os resultados são registrados no ranking local.

**Pontuação:** cada resposta correta vale **10 pontos** na classificação acumulada. O ranking também registra partidas disputadas e vitórias.

### Perguntas sem repetição

O jogo combina questões cadastradas localmente com operações geradas automaticamente. O sistema mantém o histórico das questões apresentadas durante a partida para evitar repetições, incluindo operações equivalentes de adição e multiplicação (por exemplo, `7 × 8` e `8 × 7`). Em uma nova partida, o histórico é reiniciado.

---

## 📱 Telas do aplicativo

| Tela | Função |
| --- | --- |
| **Início** | Escolher entre o modo Jogador e o modo Administrador. |
| **Jogadores** | Cadastrar, selecionar, editar e excluir participantes. |
| **Configurações** | Definir dificuldade, duração da rodada e número de vidas. |
| **Jogo** | Mostrar a conta, receber respostas, controlar a batata e exibir o tempo e as vidas. |
| **Resultado** | Apresentar o vencedor, o número de rodadas e a pontuação da partida. |
| **Ranking** | Exibir os pontos, as vitórias e as partidas registradas. |
| **Login Admin** | Acessar a área administrativa em modo de demonstração. |
| **Painel Admin** | Cadastrar, consultar, editar, excluir e filtrar questões matemáticas locais. |

As telas principais do jogador utilizam **Bottom Navigation** para acessar Jogadores, Configurações e Ranking. Durante o jogo, a navegação inferior não é exibida.

---

## 🛠️ Tecnologias utilizadas

| Tecnologia | Aplicação no projeto |
| --- | --- |
| **Kotlin** | Linguagem de programação e regras da partida. |
| **Jetpack Compose / Material 3** | Construção das telas e dos componentes visuais. |
| **Navigation Compose** | Navegação entre as oito telas. |
| **MVVM** | Separação entre interface, estado e lógica de negócio. |
| **ViewModel e StateFlow** | Gerenciamento e atualização dos estados das telas. |
| **ViewModelFactory** | Criação dos ViewModels com suas dependências. |
| **Room e DAO** | Banco de dados local e operações CRUD. |
| **Kotlin Flow** | Observação reativa das listas armazenadas no Room. |
| **Coroutines** | Execução de operações de dados e do cronômetro sem bloquear a interface. |

**Firebase Firestore** foi escolhido para a etapa de integração externa, **mas ainda não está implementado nesta versão**. Atualmente, o gerenciamento de questões acontece no Room e o jogo é local.

---

## 🧩 Arquitetura MVVM

A organização do aplicativo separa as responsabilidades em quatro áreas principais:

- **`ui/`**: telas Compose, estados e ViewModels.
- **`data/local/`**: entidades, DAOs e configuração do banco Room.
- **`data/repository/`**: acesso aos dados e tratamento de falhas nas operações.
- **`logic/`**: regras do jogo, passagem da batata e geração das questões.

Fluxo simplificado:

```text
Tela (Jetpack Compose)
       ↓ ações
ViewModel + UiState (StateFlow)
       ↓ solicitações
Repository
       ↓
Room / DAO
       ↓ Flow
ViewModel → atualização da tela
```

A lógica da partida também utiliza `RegrasJogo` e `BaralhoQuestoes`, mantendo as regras matemáticas separadas da interface.

### ViewModels

| ViewModel | Responsabilidade |
| --- | --- |
| `JogoViewModel` | Cronômetro, questões, respostas, acertos, vidas, rodadas e vencedor. |
| `JogadoresViewModel` | Cadastro, seleção de participantes e ranking. |
| `AdminViewModel` | Acesso administrativo de demonstração e gerenciamento de questões. |

Cada ViewModel possui seu próprio **State** e uma **ViewModelFactory**. Nesta versão, as classes de estado e as factories ficam nos respectivos arquivos de ViewModel.

---

## 🗂️ Estrutura de pastas

```text
app/src/main/java/com/example/batataquente/
├── MainActivity.kt
├── navigation/
│   └── AppNavigation.kt
├── logic/
│   ├── BaralhoQuestoes.kt
│   └── RegrasJogo.kt
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── JogadorEntity.kt
│   │   ├── JogadorDao.kt
│   │   ├── QuestaoEntity.kt
│   │   └── QuestaoDao.kt
│   └── repository/
│       ├── JogadorRepository.kt
│       └── QuestaoRepository.kt
└── ui/
    ├── jogo/
    │   └── JogoViewModel.kt
    ├── jogadores/
    │   └── JogadoresViewModel.kt
    ├── admin/
    │   └── AdminViewModel.kt
    └── telas/
        ├── Moldura.kt
        ├── TelaInicial.kt
        ├── TelaJogadores.kt
        ├── TelaConfiguracoes.kt
        ├── TelaJogo.kt
        ├── TelaResultado.kt
        ├── TelaRanking.kt
        ├── TelaLoginAdmin.kt
        └── TelaAdmin.kt
```

---

## 💾 Persistência com Room e CRUD

O banco de dados local contém duas entidades:

**`JogadorEntity` — tabela `jogadores`**

| Campo | Função |
| --- | --- |
| `id` | Identificador do jogador. |
| `nome` | Nome do participante. |
| `pontos` | Pontos acumulados. |
| `vitorias` | Quantidade de vitórias. |
| `partidas` | Quantidade de partidas concluídas. |

**`QuestaoEntity` — tabela `questoes`**

| Campo | Função |
| --- | --- |
| `id` | Identificador da questão. |
| `pergunta` | Expressão matemática exibida. |
| `resposta` | Resultado numérico correto. |
| `dificuldade` | Fácil, Médio ou Difícil. |
| `origem` | Identificação da origem da questão. |

Os DAOs implementam o **CRUD completo** nas duas tabelas:

- **Create:** cadastrar jogadores e questões.
- **Read:** consultar listas reativas usando `Flow<List<...>>`.
- **Update:** editar jogadores/questões e atualizar estatísticas dos jogadores.
- **Delete:** excluir jogadores e questões.

Os ViewModels coletam os dados observados e atualizam as telas. A classificação utiliza filtros e ordenação em Kotlin (`filter` e `sortedByDescending`); o painel administrativo também permite filtrar questões por dificuldade.

---

## ▶️ Como executar

### Requisitos

- **Android Studio** com Android SDK instalado.
- **Android SDK 36** para compilar o projeto (`compileSdk = 36`).
- **Emulador ou dispositivo com Android 8.0 / API 26 ou superior**.
- Projeto configurado com **Gradle 9.1.0**, **Android Gradle Plugin 9.0.1** e **Gradle JDK 25**.

### Passo a passo

1. Clone o repositório ou baixe os arquivos do projeto.
2. Abra a pasta raiz do projeto no **Android Studio**.
3. Em **Settings → Build, Execution, Deployment → Build Tools → Gradle**, selecione o JDK compatível (a configuração utilizada no desenvolvimento foi o **JDK 25**).
4. Aguarde o **Gradle Sync** e a instalação das dependências necessárias.
5. Selecione o módulo **`app`** e um emulador ou celular.
6. Clique em **Run ▶** para iniciar o aplicativo.

> **Atenção ao Gradle Wrapper:** para que um clone seja executável sem ajustes manuais, o repositório também deve conter os arquivos `gradlew`, `gradlew.bat` e `gradle/wrapper/gradle-wrapper.jar`, além de `gradle-wrapper.properties`. Esses arquivos devem ser mantidos no GitHub a partir do projeto Android Studio utilizado pela equipe.

### Teste rápido

1. Cadastre três jogadores na tela **Jogadores** e selecione os três.
2. Configure **Fácil**, **20 segundos** e **1 vida**.
3. Inicie a partida e responda às contas até o cronômetro zerar.
4. Continue as rodadas até que apareça o vencedor.
5. Abra **Ranking** e confira o registro da partida.
6. Volte ao início, abra **Administrador** e teste o cadastro, edição e exclusão de questões.

A senha do **modo administrador de demonstração** é `1234`. Essa autenticação é apenas didática, não representa um sistema seguro de login.

---

## 👥 Integrantes e divisão de responsabilidades

| Integrante | Área de responsabilidade | Principais componentes |
| --- | --- | --- |
| **Lucas** | Jogo, lógica das rodadas e navegação. | `JogoViewModel`, `RegrasJogo`, `BaralhoQuestoes`, `AppNavigation` e telas de início, jogo e resultado. |
| **Rafael** | Banco local, cadastro e classificação. | `AppDatabase`, entidades, DAOs, `JogadorRepository`, `JogadoresViewModel` e telas de jogadores, configurações e ranking. |
| **Pedro** | Administração e questões matemáticas. | `AdminViewModel`, `QuestaoRepository`, telas administrativas e futura integração com Firebase. |

A tabela registra **responsabilidades previstas para o grupo**. As contribuições individuais devem ser comprovadas pelas alterações realizadas e pelos commits de cada integrante no histórico do repositório.

---

## 📌 Situação atual e próximos passos

**Implementado na versão local:**

- [x] Oito telas e navegação entre os fluxos do jogador e do administrador.
- [x] Mecânica de batata quente, cronômetro, vidas e resultado.
- [x] Questões matemáticas com controle de repetição por partida.
- [x] Três ViewModels com estados e factories.
- [x] Duas entidades no Room e operações CRUD locais.
- [x] Listas reativas, filtros, ordenação e ranking local.
- [x] Controle de acesso administrativo de demonstração.

**Pendente para a entrega acadêmica:**

- [ ] Integrar e testar o **Firebase Firestore** como serviço externo.
- [ ] Revisar os testes finais de persistência, navegação e fluxo completo em dispositivo.
- [ ] Registrar e revisar as contribuições individuais dos três integrantes.
- [ ] Adicionar capturas de tela e um vídeo curto da aplicação funcionando.

---

## 🎓 Objetivo acadêmico

Demonstrar, em um aplicativo Android jogável, o uso de interfaces declarativas com Compose, navegação entre telas, organização MVVM, estado reativo, persistência local com Room, operações CRUD, lógica de negócio em Kotlin e integração com uma fonte de dados externa.

Este repositório corresponde ao projeto **Batata Quente Matemática**, desenvolvido por **Lucas, Rafael e Pedro** para a disciplina de Desenvolvimento Android.
