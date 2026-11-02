# SRM Credit Engine — instruções para o Claude

## Contexto
Teste técnico para vaga de desenvolvedor **pleno**. Enunciado em `/teste-tecnico.md`.
Siga apenas os requisitos de júnior e pleno. Não implemente itens de sênior/staff (optimistic locking, métricas, circuit breaker, CI, C4).

`SPEC.md` (premissas) e `DECISIONS.md` (cortes de escopo) já estão decididos. **Não os altere nem contrarie.** Se encontrar conflito, lacuna ou ambiguidade, pergunte antes de decidir.

## Stack
- Backend em `backend/`: Java 21, Spring Boot, Maven, PostgreSQL, Flyway, Testcontainers, springdoc-openapi.

- Frontend em `frontend/`: React, TypeScript e Vite com estilização shadcn/ui (tailwind css).

## Forma de trabalho
- Trabalhamos em **etapas**. Em cada etapa, primeiro apresente o **plano** (classes, migrations, testes e a divisão em commits) e **espere minha aprovação** antes de escrever código.
- Implemente **uma mudança por vez**, do tamanho de um commit, e pare para eu revisar antes da próxima.

## Git: eu executo, você sugere
- **Nunca execute comandos Git** (branch, add, commit, push, merge, PR). Eu crio as branches, faço os commits e abro os PRs.
- **No início de cada etapa:** sugira o nome da branch no formato `tipo/descricao-curta` (por exemplo, `feat/pricing-engine`).
- **Ao terminar cada mudança:** sugira a mensagem de commit no padrão conventional commits, `tipo(escopo): descrição no imperativo`, em inglês. Cada commit deve ser atômico (uma mudança lógica, com o projeto compilando).
- **Ao terminar a etapa:** sugira a descrição do PR neste formato:

```markdown
## Por quê
O problema ou requisito que motivou a mudança (citando o SPEC/DECISIONS quando for o caso).

## O que mudou
Resumo das mudanças principais.

## Decisões
Escolhas não óbvias e alternativas descartadas.

## Como testar
Comandos ou passos para validar.
```
- Explique decisões não óbvias. Eu preciso conseguir explicar e alterar todo o código numa defesa ao vivo.

## Roteiro de etapas
O backend é construído nesta ordem, uma etapa por branch. Em cada etapa, faça apenas o escopo dela; o que pertence a outra etapa fica para a vez dela.

| # | Branch | Escopo |
|---|---|---|
| 1 | `chore/project-setup` | springdoc-openapi, `docker-compose.yml` com PostgreSQL, `application.yml`, bean de `Clock` e handler global de erros com `ProblemDetail` |
| 2 | `feat/pricing-engine` | `TermCalculator` (meses inteiros, mês incompleto arredondado para cima, prazo <= 0 recusado), Strategy por tipo de recebível com spread fixo, `PricingService` com a fórmula e a conversão para USD, testes dos golden cases (commit antes da implementação) e casos de borda. **Java puro: sem banco e sem HTTP** |
| 3 | `feat/exchange-rates` | Migration, entidade e repositório de cotações; busca da cotação vigente (`valid_from <= agora`); `POST /exchange-rates`, `GET /exchange-rates/current` e `GET /exchange-rates` |
| 4 | `feat/cedentes` | Migration com a tabela e o seed de cedentes; `GET /cedentes` |
| 5 | `feat/receivables` | Migration, entidade e repositório de recebíveis; `POST /receivables` (status `PENDENTE`, sem cálculo), `GET /receivables` (filtro por status) e `GET /receivables/{id}` |
| 6 | `feat/simulation` | `POST /pricing/simulations`, usando o `PricingService` e a cotação vigente; **não persiste nada** |
| 7 | `feat/settlements` | Migration e registro imutável da liquidação; `POST /settlements` com header `Idempotency-Key`, transacional (recalcula, grava e muda o recebível para `LIQUIDADO`); `UNIQUE` em `idempotency_key` e `receivable_id`; respostas 409 e 422; `GET /settlements/{id}`; testes de idempotência com Testcontainers |
| 8 | `feat/settlement-statement` | `GET /settlements` com filtros por período, cedente e moeda e paginação server-side, em SQL nativo (`JdbcTemplate`) |


### Roteiro do frontend
O backend está completo. O frontend segue a mesma forma de trabalho: uma etapa por branch, plano antes do código.

| # | Branch | Escopo |
|---|---|---|
| F1 | `chore/frontend-setup` | Projeto Vite (React + TypeScript) em `frontend/`, cliente HTTP, tratamento de `ProblemDetail`, proxy para a API (Vite em desenvolvimento, Nginx no Docker) e o frontend no `docker-compose.yml` |
| F2 | `feat/operator-panel` | Painel do operador: formulário (cedente, tipo, valor de face, vencimento, moeda de pagamento) com simulação em tempo real, cadastro do recebível e aviso de possível duplicado (consulta `GET /receivables` com cedente, valor e vencimento antes de cadastrar) |
| F3 | `feat/settlement-flow` | Lista de recebíveis pendentes com o botão "Liquidar", gerando a `Idempotency-Key` |
| F4 | `feat/transactions-grid` | Grid de transações com paginação server-side e filtros dinâmicos (período, cedente, moeda), usando `GET /settlements` |

## Regras técnicas obrigatórias
- Dinheiro, taxas e câmbio sempre em `BigDecimal`, criado a partir de String. **Nunca `float`/`double`.**
- Comparação de `BigDecimal` com `compareTo` (nos testes, `isEqualByComparingTo`), nunca `equals`.
- Cálculos intermediários sem arredondamento. `HALF_EVEN` com 2 casas só no resultado final, e de novo na conversão para USD.
- Taxas em fração decimal (1% = `0.01`), nunca em percentual.
- Camadas separadas: `api` (controllers, DTOs, tratamento de erros), `domain` (regras de negócio), `infrastructure` (persistência).
- Data e hora atuais sempre via `Clock` injetado (fuso `America/Sao_Paulo`). Nunca `LocalDate.now()` sem `Clock`.
- Timestamps gravados em UTC (`Instant` / `TIMESTAMPTZ`).
- Schema do banco só via Flyway (`ddl-auto: validate`). Migrations já aplicadas nunca são editadas.
- Erros sempre com status HTTP semântico e `ProblemDetail`. Nunca engolir exceções nem responder erro com `200`.
- Queries sempre parametrizadas. Nunca concatenar SQL.
- Não criar classes, interfaces ou camadas antes de serem necessárias.
- Testes que dependem de banco usam Testcontainers (Postgres real, nunca H2).

## Regras do frontend
- Separar apresentação de lógica (enunciado 4.2.3): componentes só exibem e capturam dados; chamadas à API e regras de tela ficam em hooks e módulos próprios.
- Estado global só se houver justificativa (enunciado 4.2.3). Proponha e justifique antes de adicionar qualquer biblioteca de estado.
- O frontend **nunca calcula valores financeiros**. Ele só envia o que o operador digitou e exibe o que o backend devolveu.
- Valores monetários exibidos com `Intl.NumberFormat` no formato pt-BR.
- Erros da API (`ProblemDetail`) aparecem para o operador: erros de validação no próprio campo, os demais numa mensagem clara.
- Simulação em tempo real com debounce, sem botão (critério de aceite do SPEC).
- A `Idempotency-Key` é gerada uma vez por tentativa de liquidação (`crypto.randomUUID()`), reaproveitada em qualquer reenvio e trocada só depois do sucesso.
- Sem dependências desnecessárias: proponha e justifique cada biblioteca antes de adicioná-la.

- Componentes do shadcn/ui adicionados só pela CLI (`npx shadcn add ...`) e só quando uma etapa precisar deles.
- Sem dependências desnecessárias: proponha e justifique cada biblioteca antes de adicioná-la (inclusive as que alguns componentes do shadcn trazem junto, como as de formulário e validação).