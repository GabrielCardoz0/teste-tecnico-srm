### Decisões de escopo: o que foi cortado ou simplificado

1. **Prazo em meses inteiros, arredondado para cima** [spec.1]
Decisão: Não foi usada a convenção de mercado em dias (ano de 360 dias ou 252 dias úteis), que exige converter as taxas para taxas equivalentes diárias.
Motivo: Manter o expoente inteiro em BigDecimal para cálculos exatos e simplificar o projeto.

2. **Prazo igual a 0 ou negativo é recusado** [spec.2]
Decisão: Títulos vencidos ou que vencem no dia serão recusados pela API .
Motivo: A engine foi pensada para valores futuros a receber; o tratamento depende de uma resposta do negócio.

3. **Taxa base mockada em 1% a.m.** [spec.3]
Decisão: Visando simplificar o projeto, não há serviço nem tabela para gerenciar a taxa base, e ela é fixa no código.
Motivo: Não foi definida a origem da taxa base ou alguma referência (como o CDI) ou se a mesma varia ao longo do tempo.

4. **Conversão apenas de BRL para USD** [spec.8]
Decisão: A engine não irá trabalhar com títulos em USD.
Motivo: Economizar tempo e esforço, já que é o único fluxo descrito no enunciado. Mas nada impede a adição do caminho inverso, caso o negócio confirme essa necessidade.

5. **Taxa de câmbio sem prazo de validade** [spec.9]
Decisão: O sistema não verifica se a cotação mais recente está defasada, e não registra data de validade. Com isso, aceito o risco de que caso nenhuma cotação nova seja cadastrada, a liquidação irá usar uma taxa defasada.
Motivo: O prazo máximo aceitável e como tratar casos de vencimento da taxa depende de uma resposta do negócio.

6. **Cedentes via seed, apenas com listagem** [spec.11]
Decisão: Com o objetivo de manter o projeto simples, não haverá endpoints de gestão de cedentes, mesmo imaginando que seria algo natural ao projeto.
Motivo: Simplificar o projeto, e o enunciado não pede gestão de cedentes. A listagem basta para o painel do operador e para o filtro do extrato.


7. **Sem testes automatizados no frontend**
Decisão: o frontend será verificado manualmente, os testes automatizados já cobrem o backend.
Motivo: o teste técnico exige apenas testes do cálculo, das strategies e da idempotência, todos no backend, nada sobre testes no frontend, já que ele não calcula valores financeiros, só exibe o que a API devolve.

8. **Valores monetários exibidos a partir do número JSON**
Decisão: o frontend exibe os valores que a API devolve como número JSON, convertidos em `number` pelo `JSON.parse` e formatados com `Intl.NumberFormat` (pt-BR, 2 casas). Na entrada, o valor digitado é convertido como texto ("100.000,50" → "100000.50") e enviado como string, sem passar por `Number`.
Motivo: o frontend nunca calcula, só exibe. O `number` é exato até ~15 dígitos significativos (cerca de R$ 9 trilhões com centavos), muito acima de qualquer recebível. Precisão total exigiria ler o texto bruto do JSON (`context.source` do reviver), recurso recente dos navegadores, com ganho prático nulo.
