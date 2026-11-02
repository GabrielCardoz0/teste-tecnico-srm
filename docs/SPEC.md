### [PARTE 1] ambiguidades encontradas e suas premissas adotadas:

1. Qual será a unidade base do prazo na formula? (Valor Presente = Valor de Face / (1 + Taxa Base + Spread)^Prazo) [ambiguidade-informada]
Premissa: o prazo será em meses inteiros. O operador irá informar a data completa de vencimento (dia/mês/ano), isso será salvo no banco. O backend irá calcular o prazo como um número inteiro a partir da diferença entre a data atual e o vencimento informado. Haverá um arredondamento para períodos com meses incompletos para cima, por exemplo, caso a diferença seja de 3 meses e 13 dias, o prazo será de 4 meses. A justificativa para esta escolha é para simplificar o projeto e não precisar trabalhar com a conversão das taxas para taxas equivalentes em dias (entendo que seria utilizado um ano com 360 dias) para calcular o valor presente e manter o expoente inteiro em BigDecimal para cálculos exatos.

2. Vendo a formula, ela tem uma grande dependência do prazo, então como tratar casos onde ele é igual à 0 ou negativo? Isso pode ocorrer durante a operação? [perguntar-negócio]
Premissa: Gostaria de tratar essa situação com o time de negócios para entender essa possível situação.Dito isso, para simplificar o projeto, por hora, valores negativos ou com valor igual á 0 serão recusados.

3. Qual a origem e o valor da taxa base? [ambiguidade-informada]
Premissa: Neste projeto irá iniciar de forma mockada a 1% a.m. (taxa base usada de exemplo no enunciado), mas poderia ser tratada com uma camada de serviço e banco (assim como o câmbio) para fornecer a informação caso ela possa ser alterada ao longo do tempo por n questões de negócio.  

4. Qual taxa de câmbio vale no momento da liquidação (a da data da operação? a mais recente? a da data de vencimento?)? [ambiguidade-informada]
Premissa: Vou trabalhar com a cotação mais recente cadastrada no banco de dados, ou seja, com a data de vigência menor ou igual ao momento em que o método de liquidação foi executado.

5. Qual será a política de arredondamento ? [ambiguidade-informada]
Premissa: Com base em pesquisas feitas, a política de arredondamento utilizada será Half Even, já que evita o viés de se arredondar sempre para cima, o que evita um valor inflado dos resultados.

6. qual a precisão numérica o projeto irá trabalhar? [ambiguidade-informada]
Premissa: Os valores monetários serão salvos no postgres com valores até 17 digitos antes da virgula e 2 casas decimais (NUMERIC(19,2) o que é equivalente a 99 quatrilhões, mais que o suficiente), enquanto que a taxa base e spread serão trabalhadas como fração decimal com até 8 casas decimais (BigDecimal no código, NUMERIC(19,8)), valendo o mesmo para o cambio que será inserido no banco (NUMERIC(19,8)).

7. Em que momento o arredondamento irá ocorrer? [ambiguidade-informada]
Premissa: Irá ocorrer ao final do cálculo buscando levar a precisão dos valores o mais longe possível. Para valores em USD, após a conversão o arredondamento ocorre novamente.

8. O enunciado cita que a SRM Asset trabalha provendo liquidez, e por conta da globalização surge na operação a necessidade de trabalhar com um caixa multimoeda. Essa conversão é unilateral, sempre convertendo BRL para USD, ou será possível trabalhar realizando o processo inverso (USD -> BRL)? [perguntar-negócio]
Premissa: Por uma questão de tempo e esforço, como isso pode ser adicionado futuramente, vou trabalhar imaginando o caminho unilateral (BRL -> USD), logo, considerando que todas as duplicatas serão em reais e apenas o recebimento tendo possibilidade de ser feito em USD.

9. Na parte 4, fase 1, fala sobre a vigência da taxa de cambio, mas não fala sobre seu vencimento. Isso pode se tornar um problema caso a taxa não seja atualizada de forma recorrente, possivelmente gerando problemas de cambio divergente das taxas praticadas no mercado.
Premissa: Vou adotar a inserção de taxas de cambio no banco de dados com uma data de inicio e cadastro, mas sem um prazo de validade. A liquidação  irá usar a taxa mais recente com vigência menor ou igual ao momento atual. Isso gera um risco caso não seja cadastrada uma nova cotação, já que o sistema irá usar uma taxa defasada.

10. Estamos trabalhando com valores em diferentes moedas, e nada sobre fuso horário foi citado.
Premissa: Para evitar conflitos com horários divergentes entre máquinas, vou adotar o timestamp dos dados como UTC, e o frontend pode converter para o horário de brasília. Para o cálculo do prazo, será utilizado a data atual (UTC-3, São paulo) no momento do runtime da aplicação.

11. O texto cita um cedente, entendo que seja quem cede o direito de receber o valor de mercadorias vendidas à SRM, mas em nenhum momento foi citado onde ele é inserido/gerido.
Premissa: Sendo uma informação importante, e seguindo a idea de que um cedente trabalharia de forma recorrente com a SRM e possivelmente passa por uma processo de análise, imagino que deveria haver uma entidade no banco direcionada e endpoints direcionados à gestão de cedentes, mas para simplificar o projeto, vou trabalhar com um script para fazer um seed de cedentes fake, e apenas um endpoint de listagem para consumo no frontend.

12. O que caracteriza a mesma requisição para a idempotência?
Premissa: Para garantir a idempotencia, uma chave idempotency-Key será enviada no header das requests feitas pelo front com um UUID, e no back, caso seja enviado uma chave repetida, será retornado a liquidação já existente, e essa key será salva e única no banco.

---

### [PARTE 2] Perguntas à área de negócios:

1. Qual convenção de prazo o negócio utiliza? Meses inteiros, dias corridos (30/360) ou dias úteis (252)? Se forem meses inteiros, um mês incompleto deve ser arredondado para cima ou para baixo? [ambiguidades.1]

2. Vendo a formula, ela tem uma grande dependência do prazo, então como tratar casos onde ele é igual à 0 ou negativo? Isso pode ocorrer durante a operação? [ambiguidades.2]

3. A taxa base é fixa ou varia ao longo do tempo? Ela está atrelada a algum índice, como o CDI? [ambiguidades.3]

4. O enunciado cita que a SRM Asset trabalha provendo liquidez, e por conta da globalização surge na operação a necessidade de trabalhar com um caixa multimoeda. Essa conversão é unilateral, sempre convertendo BRL para USD, ou será possível trabalhar realizando o processo inverso (USD -> BRL)? [ambiguidades.8]

5. Qual o prazo máximo aceitável que a taxa de cambio de USD pode ficar defasada, e a cada quanto tempo ela precisa ser atualizada? [ambiguidades.9]
---

### [PARTE 3] Critérios de aceite:

Usabilidade:
1. A requisição para simulação deve retornar o valor calculado em até 1 segundo após ser disparada. [backend]
2. Erros de validação aparecem no próprio campo. [frontend]

Segurança:
1. Nenhuma query deve montar SQL concatenando strings. [backend]
2. Toda entrada é validada no backend. [backend]
3. Uma liquidação nunca pode ser alterada via API. [backend]

Desempenho:
1. O retorno do valor calculado da simulação deve retornar em até 1 segundo. [backend]
2. O extrato é paginado via query param server-side. [backend]