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
         "days_since_last_login":12,"created_at":"2024-01-02","risk_score":70}
        """;

    @Test
    void mapsSnakeCaseAndEncodesFilters() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0&industry=technology&q=Cedar%20%26%20Labs"))
                .andRespond(withSuccess("{\"accounts\":[" + ACCOUNT + "],\"total\":1,\"limit\":50,\"offset\":0}", MediaType.APPLICATION_JSON));
        var page = client.list("technology", "Cedar & Labs", false, 0);
        var account = page.accounts().get(0);
        assertThat(account.monthlySpendUsd()).isEqualByComparingTo("1234.50");
        assertThat(account.openTickets()).isEqualTo(3);
        assertThat(account.daysSinceLastLogin()).isEqualTo(12);
        assertThat(account.createdAt()).hasToString("2024-01-02");
        assertThat(account.riskScore()).isEqualTo(70);
        server.verify();
    }

    @Test
    void sendsHighRiskWithOtherFiltersAndOffset() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=50&industry=technology&q=Cedar&high_risk=true"))
                .andRespond(withSuccess("{\"accounts\":[],\"total\":51,\"limit\":50,\"offset\":50}", MediaType.APPLICATION_JSON));
        assertThat(client.list("technology", "Cedar", true, 50).total()).isEqualTo(51);
        server.verify();
    }

    @Test
    void missingScoreStaysUnknown() {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(ACCOUNT.replace(",\"risk_score\":70", ""), MediaType.APPLICATION_JSON));
        assertThat(client.get("acct_0001").riskScore()).isNull();
        server.verify();
    }

    @Test
    void readsDetail() {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(ACCOUNT, MediaType.APPLICATION_JSON));
        assertThat(client.get("acct_0001").name()).isEqualTo("Cedar Labs");
        server.verify();
    }

    @Test
    void propagatesMissingAccount() {
        server.expect(requestTo("http://accounts.test/accounts/missing")).andRespond(withResourceNotFound());
        assertThatThrownBy(() -> client.get("missing")).isInstanceOf(HttpClientErrorException.NotFound.class);
        server.verify();
    }
}
