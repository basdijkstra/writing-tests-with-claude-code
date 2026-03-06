# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests
mvn test

# Run a single test class
mvn test -Dtest=YourTestClassName

# Run a single test method
mvn test -Dtest=YourTestClassName#methodName

# Build without running tests
mvn package -DskipTests

# Run the application
mvn spring-boot:run

# Run mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage
```

## Architecture

Spring Boot 3.4.5 REST API using an in-memory H2 database (resets on restart). Java 21.

**Package:** `com.ontestautomation.mutationbank`

**Layers:**
- `controllers/AccountController` — REST endpoints at `/account`
- `services/AccountService` — business logic (deposits, withdrawals, interest)
- `repositories/AccountRepository` — Spring Data JPA
- `models/Account` — JPA entity with `id`, `type` (AccountType enum), `balance`
- `dto/AccountCreateUpdate` — request body for create/update
- `exceptions/` — `ResourceNotFoundException` (404), `BadRequestException` (400), handled by `CustomizedResponseEntityExceptionHandler`

**Key business rules in `AccountService`:**
- Deposits/withdrawals require `amount > 0`
- Withdrawals from `SAVINGS` accounts are blocked if balance < amount; `CHECKING` accounts allow overdraft
- Interest (`/account/{id}/interest`) only applies to `SAVINGS` accounts: 1% if balance < 1000, 2% if < 5000, 3% otherwise

**Test stack:** JUnit 5, REST Assured 6.0, spring-boot-starter-test. PITest configured with `STRONGER` mutators targeting all classes under `com.ontestautomation.mutationbank.*`.

**H2 console** available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:restdb`, user: `sa`, no password).
