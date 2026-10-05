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
         "days_since_last_login":12,"created_at":"2024-01-02"}
        """;

    @Test
    void mapsSnakeCaseAndEncodesFilters() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0&industry=technology&q=Cedar%20%26%20Labs"))
                .andRespond(withSuccess("{\"accounts\":[" + ACCOUNT + "],\"total\":1,\"limit\":50,\"offset\":0}", MediaType.APPLICATION_JSON));
        var page = client.list("technology", "Cedar & Labs", 0, false);
        var account = page.accounts().get(0);
        assertThat(account.monthlySpendUsd()).isEqualByComparingTo("1234.50");
        assertThat(account.openTickets()).isEqualTo(3);
        assertThat(account.daysSinceLastLogin()).isEqualTo(12);
        assertThat(account.createdAt()).hasToString("2024-01-02");
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
    @Test
    void forwardsHighRiskAndPagination() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=50&high_risk=true&industry=finance&q=A%20%26%20B"))
                .andRespond(withSuccess("{\"accounts\":[],\"total\":0,\"limit\":50,\"offset\":50}", MediaType.APPLICATION_JSON));
        client.list("finance", "A & B", 50, true);
        server.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(ints = {0, 39, 40, 69, 70, 100})
    void mapsRiskScoreOnListAndDetail(int score) {
        String scored = ACCOUNT.replace("\"created_at\":", "\"risk_score\":" + score + ",\"created_at\":");
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0"))
                .andRespond(withSuccess("{\"accounts\":[" + scored + "],\"total\":1,\"limit\":50,\"offset\":0}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(scored, MediaType.APPLICATION_JSON));
        assertThat(client.list("", "", 0, false).accounts().get(0).riskScore()).isEqualTo(score);
        assertThat(client.get("acct_0001").riskScore()).isEqualTo(score);
        server.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"", "\"risk_score\":null,"})
    void keepsAbsentOrNullScoreUnscored(String field) {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(ACCOUNT.replace("\"created_at\":", field + "\"created_at\":"), MediaType.APPLICATION_JSON));
        assertThat(client.get("acct_0001").riskScore()).isNull();
        server.verify();
    }

    @Test
    void doesNotHideUpstreamFailure() {
        server.expect(requestTo("http://accounts.test/accounts?limit=50&offset=0")).andRespond(withServerError());
        assertThatThrownBy(() -> client.list("", "", 0, false))
                .isInstanceOf(org.springframework.web.client.HttpServerErrorException.class);
        server.verify();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"-1", "101", "\"corrupt\""})
    void rejectsInvalidRiskRatherThanDisplayingUnscored(String score) {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withSuccess(ACCOUNT.replace("\"created_at\":", "\"risk_score\":" + score + ",\"created_at\":"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.get("acct_0001"))
                .isInstanceOf(org.springframework.web.client.RestClientException.class);
        server.verify();
    }

    @Test
    void propagatesNetworkFailure() {
        server.expect(requestTo("http://accounts.test/accounts/acct_0001"))
                .andRespond(withException(new java.io.IOException("connection failed")));
        assertThatThrownBy(() -> client.get("acct_0001"))
                .isInstanceOf(org.springframework.web.client.ResourceAccessException.class);
        server.verify();
    }

}
