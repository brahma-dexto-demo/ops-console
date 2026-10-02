package com.brahma.demo.ops;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AccountsClientTest {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final AccountsClient client = new AccountsClient(builder, "http://accounts.test");
    private static final String ACCOUNT = """
        {"id":"acct_0001","name":"Cedar Labs","industry":"technology","country":"US",
         "plan":"business","monthly_spend_usd":1234.50,"open_tickets":3,
         "days_since_last_login":12,"created_at":"2024-01-02","risk_score":73}
        """;

    @Test
    void mapsSnakeCaseAndEncodesFilters() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0&industry=technology&q=Cedar%20%26%20Labs"))
                .andRespond(withSuccess("{\"accounts\":[" + ACCOUNT + "],\"total\":1,\"limit\":50,\"offset\":0}", MediaType.APPLICATION_JSON));
        var page = client.list("technology", "Cedar & Labs", 0, false);
        var account = page.accounts().get(0);
        assertThat(account.monthlySpendUsd()).isEqualByComparingTo("1234.50");
        assertThat(account.riskScore()).isEqualTo(73);
        assertThat(account.openTickets()).isEqualTo(3);
        assertThat(account.daysSinceLastLogin()).isEqualTo(12);
        assertThat(account.createdAt()).hasToString("2024-01-02");
        server.verify();
    }

    @Test
    void readsDetail() {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(ACCOUNT, MediaType.APPLICATION_JSON));
        var account = client.get("acct_0001");
        assertThat(account.name()).isEqualTo("Cedar Labs");
        assertThat(account.riskScore()).isEqualTo(73);
        server.verify();
    }

    @Test
    void passesHighRiskWithFiltersAndPagination() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=50&high_risk=true&industry=technology&q=Cedar%20%26%20Labs"))
                .andRespond(withSuccess("{\"accounts\":[],\"total\":60,\"limit\":50,\"offset\":50}", MediaType.APPLICATION_JSON));
        assertThat(client.list("technology", "Cedar & Labs", 50, true).total()).isEqualTo(60);
        server.verify();
    }

    @Test
    void absentAndNullScoresRemainUnknown() {
        server.expect(requestTo("http://accounts.test/accounts/legacy"))
                .andRespond(withSuccess(ACCOUNT.replace(",\"risk_score\":73", ""), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://accounts.test/accounts/unscored"))
                .andRespond(withSuccess(ACCOUNT.replace("\"risk_score\":73", "\"risk_score\":null"), MediaType.APPLICATION_JSON));
        assertThat(client.get("legacy").riskScore()).isNull();
        assertThat(client.get("unscored").riskScore()).isNull();
        server.verify();
    }

    @Test
    void scoreServiceFailureIsNotAnUnknownAccount() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.list("", "", 0, false))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.ServiceUnavailable.class);
        server.verify();
    }

    @Test
    void propagatesMissingAccount() {
        server.expect(requestTo("http://accounts.test/accounts/missing")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.get("missing")).isInstanceOf(HttpClientErrorException.NotFound.class);
        server.verify();
    }
}
