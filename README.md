# Operations Console

Internal operations console. Renders customer account tables and details from accounts-api, with name search, industry and High-risk filtering, pagination, and risk badges.

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

## Account risk scores

The API supplies `risk_score` as an integer 0–100 or `null`. The console maps it
with `@JsonProperty("risk_score") Integer riskScore`; it does not calculate scores
or use zero for missing scores. A missing field (older API) or null displays
**Not scored** in neutral gray. Zero is a scored **Low** account. Directory and
detail use the same labelled badge: **Low** 0–39 green, **Medium** 40–69 amber,
**High** 70–100 red, with the numeric score shown alongside the label.
Out-of-range scores and upstream errors fail rather than display Not scored.

The **High-risk** checkbox forwards `high_risk=true` to GET `/accounts`, along
with name search, industry, limit and offset. Unchecked omits that parameter
(API default false). The API selects non-null scores >=70 **before pagination**;
the console never filters a single loaded page. Applying changed filters resets
to the first page. Previous/Next, directory-to-detail links and **All accounts**
preserve industry, search, High-risk selection and page offset. Reset intentionally
clears all filters. Merge/roll out **risk-engine → accounts-api → ops-console**.
The engine owns `floor(clamp(probability,0,1)*100 + 0.5)`; API scoring corruption,
permissions and network errors must not be converted into absent scores.

### Coordinated local integration

After the parent starts accounts-api on port 8102 with scoring data:

```sh
./mvnw -B verify
./mvnw -B package
SERVER_PORT=5103 ACCOUNTS_API_URL=http://localhost:8102 java -jar target/ops-console.jar
# Browser QA is run separately against http://localhost:5103
# In e2e/: BASE_URL=http://localhost:5103 npm test
```

Java tests cover snake_case mapping, null/absent scores, 0/39/40/69/70/100 badge
boundaries on directory/detail, query encoding/forwarding, upstream failure,
combined filters, pagination links and detail return navigation. The live e2e
suite covers the High-risk checkbox, >=70 badges, combined filters and return
navigation; seed scored accounts before running it to exercise non-empty results.
