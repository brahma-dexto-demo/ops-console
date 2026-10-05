package com.brahma.demo.ops;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AccountsController.class, HealthController.class})
class AccountsControllerTest {
    @Autowired MockMvc mvc;
    @MockBean AccountsClient client;
    private final Account account = new Account("acct_0001", "Cedar Labs", "technology", "US",
            "business", new BigDecimal("1234.50"), 3, 12, LocalDate.of(2024, 1, 2), null);

    @Test
    void rendersFilteredTable() throws Exception {
        when(client.list("technology", "Cedar", 0, false)).thenReturn(new AccountsPage(List.of(account), 1, 50, 0));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar"))
                .andExpect(status().isOk()).andExpect(view().name("accounts"))
                .andExpect(content().string(containsString("Cedar Labs")))
                .andExpect(content().string(containsString("1,234.50")));
    }

    @Test
    void rendersDetail() throws Exception {
        when(client.get("acct_0001")).thenReturn(account);
        mvc.perform(get("/accounts/acct_0001")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Days since last login")))
                .andExpect(content().string(containsString("2024-01-02")));
    }

    @Test
    void rendersEmptyState() throws Exception {
        when(client.list("", "unknown", 0, false)).thenReturn(new AccountsPage(List.of(), 0, 50, 0));
        mvc.perform(get("/").param("q", "unknown")).andExpect(status().isOk())
                .andExpect(content().string(containsString("No accounts match")));
    }

    @Test
    void missingAccountAndInvalidOffset() throws Exception {
        when(client.get("missing")).thenThrow(HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, "missing", HttpHeaders.EMPTY, new byte[0], null));
        mvc.perform(get("/accounts/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/").param("offset", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"0,Low,risk-low", "39,Low,risk-low", "40,Medium,risk-medium", "69,Medium,risk-medium", "70,High,risk-high", "100,High,risk-high"})
    void rendersRiskBoundaries(int score, String label, String css) throws Exception {
        Account scored = new Account(account.id(), account.name(), account.industry(), account.country(),
                account.plan(), account.monthlySpendUsd(), account.openTickets(), account.daysSinceLastLogin(), account.createdAt(), score);
        when(client.list("", "", 0, false)).thenReturn(new AccountsPage(List.of(scored), 1, 50, 0));
        when(client.get(account.id())).thenReturn(scored);
        for (String path : List.of("/", "/accounts/" + account.id())) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString(label + " · " + score)))
                    .andExpect(content().string(containsString(css)));
        }
    }

    @Test
    void rendersNeutralNull() throws Exception {
        when(client.list("", "", 0, false)).thenReturn(new AccountsPage(List.of(account), 1, 50, 0));
        when(client.get(account.id())).thenReturn(account);
        for (String path : List.of("/", "/accounts/" + account.id())) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Not scored")))
                    .andExpect(content().string(containsString("risk-neutral")));
        }
    }

    @Test
    void preservesCombinedFiltersAndNavigation() throws Exception {
        when(client.list("technology", "Cedar & Labs", 50, true))
                .thenReturn(new AccountsPage(List.of(account), 151, 50, 50));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar & Labs")
                        .param("offset", "50").param("high_risk", "true"))
                .andExpect(status().isOk()).andExpect(model().attribute("highRisk", true))
                .andExpect(content().string(containsString("checked=\"checked\"")))
                .andExpect(content().string(containsString("/accounts/acct_0001?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=50")))
                .andExpect(content().string(containsString("/?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=0")))
                .andExpect(content().string(containsString("/?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=100")));
        when(client.get(account.id())).thenReturn(account);
        mvc.perform(get("/accounts/" + account.id()).param("industry", "technology").param("q", "Cedar & Labs")
                        .param("offset", "50").param("high_risk", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=50")));
        org.mockito.Mockito.verify(client).list("technology", "Cedar & Labs", 50, true);
    }

}
