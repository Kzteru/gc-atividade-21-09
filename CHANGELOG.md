# Changelog

Todas as mudanças relevantes do AgendaFácil são registradas neste arquivo.

O formato segue o [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/)
e o projeto usa [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [Não publicado]

## [1.2.0] - 2026-10-24



### Adicionado
- Tela de faturamento por período, serviço e profissional em `/relatorios` (#32, PR #33)
- Workflow de release que gera o `.jar` e publica a GitHub Release ao enviar uma tag (#32, PR #33)
- Este `CHANGELOG.md` (#27, PR #28)

### Corrigido
- Profissional não consegue mais alterar anotações ou o resultado de atendimentos de outro profissional (#27, PR #28)

### Alterado
- Versão do `pom.xml` alinhada com a tag do Git (#33, PR #32)

## [1.1.0] - 2026-10-23

### Adicionado
- Workflow de CI (`ci.yml`) que compila e roda os testes a cada push e Pull Request (#NN, PR #NN)
- Workflow que exige o vínculo de uma issue em todo Pull Request (#25, PR #26)

### Alterado
- Branch `main` protegida, exigindo o check do CI para o merge (#03)

## [1.0.0] - 2026-10-06

### Adicionado
- Login, cadastro de clientes, edição de perfil e cadastro de profissionais pelo admin
- CRUD de serviços, horários de trabalho e bloqueios
- Agendamento com validação de conflitos, cancelamento e remarcação com antecedência mínima
- Agenda do profissional, com confirmação e cancelamento de atendimentos
- Histórico do cliente com anotações e registro de concluído/faltou

[Não publicado]: https://github.com/Kzteru/gc-atividade-21-09/compare/v1.2.0...HEAD
[1.2.0]: https://github.com/Kzteru/gc-atividade-21-09/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/Kzteru/gc-atividade-21-09/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/Kzteru/gc-atividade-21-09/releases/tag/v1.0.0