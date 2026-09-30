# Operations Console

Internal operations console. Renders customer account tables and details from accounts-api, with name search, industry filtering and pagination.

This is a demo repository backed by synthetic, fictional accounts.

## Local development

```sh
./mvnw -B verify
ACCOUNTS_API_URL=http://localhost:8000 ./mvnw spring-boot:run
./mvnw -B package
```

Requires Java 17 or newer; Maven is downloaded by the checked-in wrapper.
The compiler targets Java 17, using Spring Boot 3.3. The executable artifact is
`target/ops-console.jar`. Start accounts-api first, then open http://localhost:5000.
`SERVER_PORT` defaults to 5000; `ACCOUNTS_API_URL` defaults to http://localhost:8000.
`/healthz` provides a process health endpoint. Java DTOs use camelCase with explicit
`@JsonProperty` mappings for today's snake_case wire fields, without a global
Jackson naming strategy.

Run the live browser smoke against a running console and API:

```sh
cd e2e
npm ci
npx playwright install chromium
BASE_URL=http://localhost:5000 npm test
```

See [DEPLOY.md](DEPLOY.md) for the AWS API staging flow.
