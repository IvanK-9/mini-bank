# Mini Bank

A small banking REST API: accounts, deposits, withdrawals, transfers and transaction history.
Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit 5 + Mockito.

## Run

```bash
docker compose up -d      # PostgreSQL on :5432
mvn spring-boot:run
mvn test
```

## Endpoints

| Method | Path | Description |
|---|---|---|
| POST | `/api/accounts` | Create account `{"ownerName": "Ivan"}` |
| GET | `/api/accounts/{id}` | Balance |
| POST | `/api/accounts/{id}/deposit` | `{"amount": 100.00}` |
| POST | `/api/accounts/{id}/withdraw` | `{"amount": 20.00}`, 422 if not enough money |
| GET | `/api/accounts/{id}/transactions` | History, newest first |
| POST | `/api/transfers` | Header `Idempotency-Key`, body `{"fromAccountId":1,"toAccountId":2,"amount":10.00}` |

```bash
curl -X POST localhost:8080/api/transfers \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"fromAccountId":1,"toAccountId":2,"amount":10.00}'
```

## Design decisions

- **Money is `BigDecimal`** (scale 2), never `double`.
- **Business rules live in the `Account` entity** (no overdraft, amount > 0), so they hold no matter who calls them.
- **Transfers are atomic**: one `@Transactional` method changes both accounts and writes both ledger entries, or nothing.
- **Optimistic locking** (`@Version`) on accounts: two concurrent updates of the same account cannot silently overwrite each other; the loser gets `409` and retries.
- **Idempotency**: a repeated `Idempotency-Key` returns the original transfer (200) instead of moving money twice. A unique constraint covers parallel duplicates.
- **Ledger is append-only**: balance changes are always traceable to entries.

## Known limitations / next steps

- [ ] Concurrency test against a real PostgreSQL (Testcontainers): two parallel withdrawals from one account
- [ ] Replay check should also compare the request body with the original transfer
- [ ] Flyway migrations instead of `ddl-auto: update`
- [ ] Retry on `409` inside the service
- [ ] Authentication (Spring Security), currencies, pagination of history
- [ ] Dockerfile and GitHub Actions
