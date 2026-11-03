# AI_USAGE

Usei a IA de duas formas:

- **Claude (chat):** para entender o enunciado, discutir as premissas, montar os prompts e revisar planos e PRs.
- **Claude Code (Opus) no VS Code:** para planejar e escrever o código de cada etapa.

## 1. Como eu organizei o trabalho

### Primeiro, entender o problema

Antes do código, usei o chat para entender o negócio. Três pontos mudaram o desenho do sistema:

- **Liquidação é a compra do recebível pelo fundo**, não o pagamento no vencimento. Por isso o prazo é contado a partir do dia da liquidação, e o valor é recalculado nesse momento.
- **Cadastro e liquidação são passos separados**, porque na operação real há um intervalo entre receber o título e comprá-lo.
- **Idempotência e duplicidade são coisas diferentes:** a `Idempotency-Key` protege contra falhas técnicas (retry, duplo clique); o aviso de possível duplicado protege contra erro humano.

### As regras ficam num arquivo

Criei o [CLAUDE.md](CLAUDE.md), que o Claude Code lê em toda sessão. Ele define:

- **Escopo:** só júnior e pleno.
- **Limites:** o `SPEC.md` e o `DECISIONS.md` não podem ser alterados. Em caso de dúvida, a IA pergunta.
- **Regras técnicas:** `BigDecimal` criado a partir de String, `HALF_EVEN` só no resultado final, taxas em fração, `Clock` injetado, `ProblemDetail`, queries parametrizadas e Testcontainers.
- **Roteiro:** 8 etapas de backend e 4 de frontend, uma branch por etapa.
- **Forma de trabalho:** plano antes do código, um commit por vez, e a IA nunca executa Git.

### Um prompt por etapa

Cada prompt trazia o contrato, as regras e os testes esperados, e terminava com *"apresente só o plano desta etapa. Não escreva código até eu aprovar."*

Exemplo da **etapa 2 (motor de precificação)**:

> - Os testes dos golden cases C1, C2 e C3 vêm num commit ANTES da implementação.
> - Casos de borda do TermCalculator, com Clock fixo: mês exato, mês incompleto, fim de mês, vencimento hoje e vencido.

Os golden cases foram commitados antes do código (`8280524` e `c3e24d8`). Assim, o cálculo da IA tinha que bater com o enunciado.

Exemplo da **etapa 7 (liquidação)**:

> - Duas requisições simultâneas com a mesma chave: uma grava, a outra recebe a liquidação já gravada (não pode virar 500).
> - Atenção: depois de uma violação de UNIQUE, a transação atual fica inválida. Explique como o plano trata isso.

Eu já sabia onde estava o risco, então pedi uma explicação, e não só o código.

### Verificar em vez de supor

- **Versão do springdoc:** pedi para a IA não escolher de memória. Ela conferiu no Maven Central e chegou à 3.2.0, compatível com o Spring Boot 4.1.2.
- **Valor como texto:** pedi para conferir se o backend aceitava o valor como string no JSON antes de mudar qualquer coisa.
- **Pacote `cn`:** o template do shadcn trouxe uma dependência de nome genérico. Conferi no site oficial do shadcn que ela é do próprio projeto.
- **Ferramentas oficiais:** o backend veio do Spring Initializr e o frontend, do template do shadcn, e não de configurações escritas pela IA.

## 2. Onde a IA errou

### Valor de face 100 vezes maior

- **Erro:** a máscara do campo ignorava o ponto digitado. Com o ponto do teclado numérico, `100000.50` virava `10.000.050`.
- **Como percebi:** testando a tela à mão. Os testes da IA só usavam vírgula.
- **Correção:** troquei por uma máscara de centavos, como nos apps de banco (`123` → `1,23`). Só os dígitos contam.

### Um teste impossível

- **Erro:** o chat sugeriu, e eu coloquei no prompt, um teste com o valor presente terminando exatamente em meio centavo.
- **Como percebi:** o Claude Code mostrou que isso é matematicamente impossível com o valor de face em centavos. Conferi o raciocínio antes de aceitar.
- **Correção:** o teste de `HALF_EVEN` foi para a conversão para USD, onde o empate pode acontecer (23214,985 → 23214,98).

### Dinheiro comparado como `Double` nos testes

- **Erro:** a IA conferiu valores com `jsonPath`, que lê números como `Double` e perde a escala (`100000.00` vira `100000.0`).
- **Como percebi:** três testes falharam na etapa 5.
- **Correção:** os valores passaram a ser conferidos no JSON bruto. O teste da etapa 3 ainda usa o padrão antigo e está registrado como pendência.

### Resposta do câmbio diferente do que foi gravado

- **Erro:** o `POST` devolvia a data com nanossegundos e a taxa como `5.4321`, mas o banco guardava microssegundos e `5.43210000`.
- **Como percebi:** comparando a resposta do `POST` com a do `GET`.
- **Correção:** a data passou a ser cortada em microssegundos, e a taxa, normalizada para 8 casas.

### SQL concatenado num teste

- **Erro:** um teste tinha `"SELECT count(*) FROM " + table`. Não havia risco real, mas o CLAUDE.md proíbe.
- **Como percebi:** revisando o commit.
- **Correção:** troquei por queries fixas e procurei outras concatenações no projeto.

### Container antigo respondendo

- **Erro:** o build falhou, e o container da etapa anterior continuou respondendo. Parecia um bug no código novo.
- **Como percebi:** o comportamento não batia com o código, e o erro estava no log.
- **Correção:** sempre reconstruo a imagem (`docker compose up -d --build`) antes de testar.

### Arquivo que não existia

- **Erro:** a IA citou um `PLANO-PLENO.md` que eu já tinha apagado.
- **Como percebi:** perguntei de onde vinha. Era de um `git status` antigo.
- **Lição:** o contexto da IA pode estar desatualizado.

### Erros do chat no planejamento

- O primeiro rascunho do `DECISIONS.md` misturava cortes que eu não tinha decidido. Pedi uma versão baseada só no meu `SPEC.md` e reescrevi com as minhas palavras.
- O chat citou "não ter autenticação" como um corte de algo pedido no enunciado. O enunciado não fala de autenticação, e a IA corrigiu.
- O roteiro do `CLAUDE.md` ficou desatualizado (`/cedentes` em vez de `/assignors`). Atualizei antes do frontend.

## 3. O que eu não deleguei

- **As premissas e os cortes de escopo.** Usei a IA para ver as opções, mas cada decisão foi minha. Às vezes a revisão mudou minha ideia: eu tinha definido 4 casas decimais para tudo, e a IA mostrou que isso fazia o golden case C1 errar por R$ 0,81. Separei a precisão: 2 casas para dinheiro, 8 para taxas e câmbio.
- **O Git.** Branches, commits, PRs e merges foram feitos por mim. A IA só sugeria os nomes e as descrições.
- **As decisões de modelagem.** As moedas ficaram numa tabela de referência por escolha minha. Para o tipo de recebível, a IA propôs o mesmo, e eu preferi um `VARCHAR`, com a validação na API.
- **Code review** (`REVIEW.md`). Escrevi a análise inicial com os defeitos que encontrei e ordenei por gravidade, mas pedi ao claude para analisar o que escrevi e verificar se seria válido (SQL injection, taxas 100 vezes maiores, ponto flutuante e idempotência),  também pedi para buscar outras melhorias. Revisei cada item e consigo defender as correções com o código deste projeto.
- **A validação final.** Testei cada etapa à mão antes de commitar. Foi assim que encontrei a máscara 100 vezes maior.