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

## Risk scores

The console explicitly maps the API's snake_case `risk_score` to nullable Java
`Integer`. Directory and detail badges show Low (0–39, green), Medium (40–69,
amber), High (70–100, red), or Unknown (null/absent, neutral). Text and accessible
labels carry the band and score, so color is not the only indicator. The console
does not calculate scores or convert upstream failures into Unknown.

The Risk filter sends `high_risk=true` to the API; the API filters scores >=70
before pagination and total calculation. Search, industry and pagination retain
this selection; applying filters resets the offset, and Reset clears all controls.
Merge/roll out **risk-engine → accounts-api → ops-console**. The score field is
additive; old API responses without it render Unknown. The high-risk browser
smoke requires at least one scored high-risk account in the producer artifact.

For the isolated account-risk-score worktree:

```sh
export JAVA_HOME=/workspace/toolchains/java17
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw -B verify
SERVER_PORT=5101 ACCOUNTS_API_URL=http://localhost:8101 java -jar target/ops-console.jar
# Browser smoke target: BASE_URL=http://localhost:5101
```
