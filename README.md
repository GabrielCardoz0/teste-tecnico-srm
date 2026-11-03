# SRM Credit Engine

Uma pequena plataforma para gerenciar recebíveis.

---

# Sobre o projeto

O operador cadastra um recebível (duplicata mercantil ou cheque pré-datado), vê na hora quanto ele vale hoje e faz a liquidação em real (BRL) ou em dólar (USD). Depois, consulta tudo no extrato.

## O que dá para fazer

- **Simular**: ao preencher o formulário, o sistema calcula o valor presente e o deságio na hora.
- **Cadastrar**: o recebível é salvo como `PENDENTE`. O sistema avisa se já existir um parecido.
- **Liquidar**: o recebível pendente é pago e passa a `LIQUIDADO`. Se o mesmo pedido for enviado duas vezes, ele não é pago em dobro.
- **Extrato**: lista das liquidações, com filtros por período, cedente e moeda, e paginação.

## Como o cálculo funciona

```
Valor presente = Valor de face / (1 + taxa base + spread) ^ prazo
```

- A taxa base é de 1% ao mês.
- O spread é de 1,5% para duplicata mercantil e de 2,5% para cheque pré-datado.
- O prazo é contado em meses inteiros até o vencimento, e um mês incompleto conta como um mês a mais.

As premissas e os cortes de escopo estão em [docs/SPEC.md](docs/SPEC.md) e em [docs/DECISIONS.md](docs/DECISIONS.md).

## Tecnologias

- **Backend:** Java 21, Spring Boot, PostgreSQL, Flyway, Testcontainers.
- **Frontend:** React, TypeScript, Vite, Tailwind e shadcn/ui.

## Estrutura

```
backend/    API em Spring Boot (api, domain, infrastructure)
frontend/   Interface em React
docs/       Premissas (SPEC) e decisões de escopo (DECISIONS)
```

---

# Como rodar

## 1. Subir com Docker

O único requisito é ter o Docker e o Docker Compose instalados:

- **Windows e macOS:** instale o [Docker Desktop](https://docs.docker.com/desktop/). Ele já vem com o Docker Compose.
- **Linux:** instale o [Docker Engine](https://docs.docker.com/engine/install/) e depois o [plugin do Docker Compose](https://docs.docker.com/compose/install/linux/).

Para conferir se está tudo certo, rode `docker --version` e `docker compose version`.

Na raiz do projeto, rode:

```bash
docker compose up -d --build
```

Depois, abra no navegador:

| O quê | Endereço |
|---|---|
| Aplicação | http://localhost:3000 |
| API | http://localhost:8080 |
| Documentação da API (Swagger) | http://localhost:8080/swagger-ui.html |

Para desligar tudo, rode `docker compose down`. Para apagar também os dados do banco, use `docker compose down -v`.

## 2. Acessar o banco de dados

O banco é criado sozinho, já com alguns cedentes de exemplo. Para conectar com um cliente (DBeaver, pgAdmin, `psql`), use:

| Campo | Valor |
|---|---|
| Host | `localhost` |
| Porta | `5433` |
| Banco | `credit_engine` |
| Usuário | `credit_engine` |
| Senha | `credit_engine` |

## 3. Cadastrar uma cotação do dólar

O sistema começa sem nenhuma cotação. Para simular ou liquidar em USD, cadastre uma antes:

```bash
curl -X POST http://localhost:8080/exchange-rates \
  -H "Content-Type: application/json" \
  -d '{"sourceCurrency": "USD", "targetCurrency": "BRL", "rate": "5.50"}'
```

Sem cotação, as operações em BRL funcionam normalmente, mas as operações em USD são recusadas.

## Desenvolvimento local (sem Docker para a app)

Para isso, você precisa do Java 21 e do Node 22.

1. Suba só o banco:
   ```bash
   docker compose up -d postgres
   ```
2. Inicie o backend, que fica na porta 8080:
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
3. Em outro terminal, inicie o frontend, que fica em http://localhost:5173:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

O frontend chama a API pelo caminho `/api`, que é redirecionado para o backend: em desenvolvimento quem faz isso é o Vite, e no Docker é o Nginx.

## Testes

Os testes do backend usam um PostgreSQL de verdade, criado pelo Testcontainers. Por isso, o Docker precisa estar rodando.

```bash
cd backend
./mvnw test
```

Para verificar o frontend:

```bash
cd frontend
npm run lint
npm run typecheck
```
