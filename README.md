# AgendaFácil

Plataforma de agendamento de serviços locais (barbearias, salões, clínicas, estúdios).
O documento de definição do problema está em [`docs/`](docs/).

**Stack:** Java 17, Spring Boot 3, Thymeleaf, Bootstrap 5, Spring Security, Spring Data JPA, H2.
Tudo em Java: as páginas HTML são geradas pelo servidor, não existe front end separado.

## Como rodar

Pré-requisitos: **Java 17 ou superior** e **Maven** (ou o IntelliJ, que já traz o Maven).

**No IntelliJ:** `File > Open`, selecione a pasta do projeto (onde está o `pom.xml`), espere baixar as
dependências e rode a classe `AgendaFacilApplication`.

**No terminal:**

```bash
mvn spring-boot:run
```

Acesse **http://localhost:8080**. Contas de teste, todas com senha `123456`:

| Perfil       | E-mail                         |
|--------------|--------------------------------|
| Cliente      | cliente@agendafacil.com        |
| Profissional | profissional@agendafacil.com   |
| Admin        | admin@agendafacil.com          |

Para ver o banco: **http://localhost:8080/h2-console** (JDBC URL `jdbc:h2:file:./data/agendafacil`,
usuário `sa`, senha vazia). Para zerar o banco, pare o sistema e apague a pasta `data/`.

Para rodar os testes: `mvn test`.

## Estrutura

```
src/main/java/br/com/agendafacil/
├── model/         Entidades (tabelas). NÃO altere sem combinar com o grupo.
├── repository/    Consultas ao banco
├── service/       Regras de negócio (AgendamentoService é o coração do sistema)
├── controller/    Recebem as requisições e escolhem a página
├── config/        Segurança e dados de exemplo
└── exception/     RegraNegocioException (mensagens de erro para o usuário)

src/main/resources/
├── templates/     Páginas HTML (Thymeleaf). fragments/layout.html tem o cabeçalho e o menu
├── static/css/    estilo.css (cores e fonte do sistema)
└── application.properties
```

## Regra principal: validação de conflitos

Todo agendamento passa por `AgendamentoService`, que só aceita o horário se:

1. estiver no futuro;
2. couber inteiro dentro de um período de trabalho do profissional (almoço = intervalo entre dois períodos);
3. não cruzar nenhum bloqueio (limpeza, pausa, compromisso);
4. não cruzar outro agendamento do profissional (cancelados não contam);
5. não cruzar outro agendamento do próprio cliente.

Dois horários se sobrepõem quando `inicioA < fimB` **e** `fimA > inicioB`. Por isso um atendimento
das 10:00 às 10:30 e outro das 10:30 às 11:00 **não** conflitam.

> **Nunca** salve um `Agendamento` direto pelo repository num controller. Use sempre
> `agendamentoService.agendar(...)`, `remarcar(...)` ou `cancelar(...)`.

Cancelamento e remarcação exigem antecedência mínima, configurada em
`agendafacil.cancelamento.horas-antecedencia` (padrão: 24 horas).

## Divisão por duplas

| Dupla | Módulo | O que construir | Ponto de partida |
|-------|--------|-----------------|------------------|
| 1 | Login e cadastro | Tela `/cadastro` de cliente, edição de perfil, cadastro de profissionais pelo admin, ajustes no layout | `SecurityConfig`, `UsuarioRepository`, `fragments/layout.html` |
| 2 | Serviços, horários e bloqueios | CRUD de serviços (`/servicos`), horários de trabalho (`/horarios`) e bloqueios (`/bloqueios`) | `ServicoRepository`, `HorarioTrabalhoRepository`, `BloqueioRepository` |
| 3 | Agendamento | Tela `/agendamentos/novo` (escolher serviço, profissional, dia e horário livre), `/agendamentos` (meus agendamentos, cancelar, remarcar), `/agenda` do profissional | `AgendamentoService.horariosDisponiveis` e `agendar` |
| 4 | Histórico e relatórios | Histórico do cliente com anotações do profissional, marcar atendimento como concluído/faltou, `/relatorios` de faturamento por período, serviço e profissional | `findByClienteIdOrderByInicioDesc`, `faturamentoNoPeriodo` |
| 5 | Lembretes e qualidade | Envio de e-mail na marcação e lembrete automático (`@Scheduled` + `@EnableScheduling`), revisão de PRs, testes, README e apresentação | `findByStatusAndLembreteEnviadoFalseAndInicioBetween`, `spring.mail.*` |

O menu já tem links para essas telas; eles dão erro 404 até cada dupla criar o controller correspondente.
Use o `InicioController` e o `index.html` como modelo.

## Como trabalhar com o Git

1. Antes de começar: `git checkout main` e `git pull`.
2. Crie uma branch para a tarefa: `git checkout -b feature/tela-servicos`.
3. Faça commits pequenos: `git add .` e `git commit -m "Cria listagem de serviços"`.
4. Envie: `git push -u origin feature/tela-servicos` e abra um **Pull Request** no GitHub.
5. Outra pessoa revisa e aprova. Só então o PR é mesclado na `main`.
6. Deu conflito? Rode `git pull origin main` na sua branch, resolva os arquivos marcados, faça commit e push.

Regras do grupo:

- Ninguém faz push direto na `main` (ative a proteção em *Settings > Branches*).
- Pelo menos um Pull Request por dupla por dia, para não acumular conflitos.
- Mudanças no pacote `model/` só depois de avisar o grupo.
- Senhas e chaves ficam em variáveis de ambiente, nunca no código.

## Cronograma

| Dia | Meta |
|-----|------|
| 1 | Todos rodando o projeto, treino de Git, cada dupla com sua branch criada |
| 2 | Primeiras telas de cada módulo funcionando |
| 3 | Fluxo completo: cliente entra, escolhe serviço, vê horários livres e agenda |
| 4 | Integração, e-mails, relatórios e ajustes visuais |
| 5 | Até o meio-dia só correções. À tarde, revisão do README e ensaio da apresentação |
