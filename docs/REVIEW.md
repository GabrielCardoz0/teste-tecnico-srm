# REVIEW (Anexo A)

## Pontos que considero críticos

1. SQL injection nas consultas: Linha 211 (também l225 e l229) uma consulta é feita direto com template string passando um valor recebido direto do body para dentro do SQL. Isso deve ser corrigido, deve ser usado a parametrização da consulta. (considerando que o código está em JS, a lib responsável com certeza aceita um array de parâmetros no acesso ao banco). Caso continue assim, uma pessoa má intencionada consegue ler e alterar dados do banco apenas manipulando o body da request. Idealmente também seria adicionar uma validação mínima dessas chaves do JSON enviado.

2. O cálculo para valor presente está incorreto: Está usando a base (e também o spread) como 1.0, ou seja, 1+1.0 = 2, logo, não está convertendo a taxa para valores de fração nominal (0.01). Isso gera um número que trabalha com taxas como 100% e 150%, ao contrário de 1% e 1,5%.

3. Não há tratamento de erro para insert e update e status sempre igual a 200: Dentro do try-catch onde ocorre o INSERT e o UPDATE da liquidação e do recebível respectivamente, caso ocorra um erro no SQL de UPDATE, no banco haverá um recebível que irá continuar com status pendente, o que pode gerar um erro de diversas liquidações para o mesmo recebível. Isso se agrava pela falta de validação da existência de uma liquidação já cadastrada pré inserção de uma liquidação nova e pelo fato do endpoint sempre retornar status 200 independente do que aconteça. Isso precisa de um tratamento de erro retornando status 400 ou 500 na request e a adição de transação para que caso uma das consultas falhe, nenhuma delas deve rodar.

4. Um ponto que foi levantado durante a descrição do teste técnico é o fato de ser necessário se trabalhar com BigDecimal para evitar problemas de ponto flutuante durante os cálculos, e na linha 214 os valores das taxas foram declarados como numbers. Eles precisam ser trabalhados como BigDecimal (JAVA) ou alguma lib (JS/TS).


## Pontos que considero de médio risco

5. Registro da taxa de cambio utilizada: Vi que na linha 220 usa-se um serviço para buscar a taxa de cambio, mas o valor da taxa utilizada no cálculo para a liquidação não está sendo gravada no banco, é importante que essa informação seja registrada para se ter auditoria dos dados.

6. Assim como o ponto 5, é importante que outras informações sejam salvas na tabela de liquidações para se ter uma auditoria, como a taxa base e o spread já que estão mockados no código, assim seria possível saber se a taxa base da operação foi 1% e o spread de 1,5% ou se foi utilizado 0,5% + 2%.

7. Entradas do body e resultado da busca não validados: `receivableId` e `currency` são usados direto, sem nenhuma validação, e o retorno da consulta do recebível não é verificado antes do uso. Caso o recebível não exista, `receivable.type` lança um TypeError que cai no catch sem explicação (e ainda retorna 200, como citado no ponto 3). Já na moeda, qualquer valor diferente de "USD" (ex: "EUR") é tratado silenciosamente como BRL e gravado com a moeda errada. O ideal é validar o body retornando 400, restringir a moeda a valores conhecidos e retornar 404 caso o recebível não seja encontrado.

8. Spread padrão para tipo de recebível desconhecido: A verificação `receivable.type === "DUPLICATA" ? 1.5 : 2.5` aplica o spread de cheque para qualquer tipo que não seja duplicata. Com isso, um tipo novo ou um valor errado no banco seria precificado silenciosamente com uma taxa que não é a dele. Cada tipo deveria ter seu spread definido explicitamente (um map ou padrão Strategy, por exemplo), e um tipo não mapeado deve gerar erro, nunca cair em um valor padrão.

9. Regra de negócio e consultas ao banco dentro do controller: Taxa base, spread, fórmula do valor presente e consultas estão todos no controller. Isso dificulta a manutenção (mudar uma taxa exige caçar onde mais ela é usada), impede reaproveitar o cálculo em outro endpoint (ex: uma simulação) sem duplicar código e impede testar o cálculo isoladamente. Sugiro separar em camadas: o controller cuidando apenas do HTTP, um service com a regra de negócio (taxas, spreads e cálculo) e um repository para o acesso ao banco. Caso as taxas precisem mudar sem deploy, vale levar ao time de negócio a ideia de movê-las para configuração ou para o banco.


## Pontos que considero de baixo risco

10. Strings soltas no código: "DUPLICATA", "USD" e "SETTLED" aparecem como texto direto, sem tipagem. Um erro de digitação passa despercebido e o mesmo valor se repete em vários lugares. Deveriam virar enums ou constantes.

11. Resposta do endpoint: O retorno é `200 { ok: true, amount }`, sem o id da liquidação criada. Para uma criação, o ideal é retornar 201 com o registro criado (ou ao menos o id), o que facilita para o front e para a rastreabilidade.
