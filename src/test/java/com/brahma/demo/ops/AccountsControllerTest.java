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
            "business", new BigDecimal("1234.50"), 3, 12, LocalDate.of(2024, 1, 2), 70);

    @Test
    void rendersFilteredTable() throws Exception {
        when(client.list("technology", "Cedar", false, 0)).thenReturn(new AccountsPage(List.of(account), 1, 50, 0));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar"))
                .andExpect(status().isOk()).andExpect(view().name("accounts"))
                .andExpect(content().string(containsString("Cedar Labs")))
                .andExpect(content().string(containsString("1,234.50")));
    }

    @Test
    void riskLabelsAndPaginationPreserveServerFilter() throws Exception {
        when(client.list("technology", "Cedar", true, 50))
                .thenReturn(new AccountsPage(List.of(account), 101, 50, 50));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar")
                        .param("high_risk", "true").param("offset", "50"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("risk-high")))
                .andExpect(content().string(containsString("High · 70")))
                .andExpect(content().string(containsString("high_risk=true")))
                .andExpect(content().string(containsString("offset=100")))
                .andExpect(content().string(containsString("offset=0")));
    }

    @Test
    void showsAllRiskBandsIncludingUnknown() throws Exception {
        var low = new Account("low", "Low", "retail", "US", "basic", BigDecimal.ONE, 0, 0, LocalDate.now(), 39);
        var medium = new Account("medium", "Medium", "retail", "US", "basic", BigDecimal.ONE, 0, 0, LocalDate.now(), 40);
        var unknown = new Account("unknown", "Unknown", "retail", "US", "basic", BigDecimal.ONE, 0, 0, LocalDate.now(), null);
        when(client.list("", "", false, 0)).thenReturn(new AccountsPage(List.of(low, medium, account, unknown), 4, 50, 0));
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Low · 39")))
                .andExpect(content().string(containsString("Medium · 40")))
                .andExpect(content().string(containsString("High · 70")))
                .andExpect(content().string(containsString("risk-unknown")));
        when(client.get("unknown")).thenReturn(unknown);
        mvc.perform(get("/accounts/unknown"))
                .andExpect(content().string(containsString("Risk score")))
                .andExpect(content().string(containsString("risk-unknown")));
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
        when(client.list("", "unknown", false, 0)).thenReturn(new AccountsPage(List.of(), 0, 50, 0));
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
}
