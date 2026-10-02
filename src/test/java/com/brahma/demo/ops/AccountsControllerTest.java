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
            "business", new BigDecimal("1234.50"), 3, 12, LocalDate.of(2024, 1, 2), 73);

    @Test
    void rendersFilteredTable() throws Exception {
        when(client.list("technology", "Cedar", 0, false)).thenReturn(new AccountsPage(List.of(account), 1, 50, 0));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar"))
                .andExpect(status().isOk()).andExpect(view().name("accounts"))
                .andExpect(content().string(containsString("Cedar Labs")))
                .andExpect(content().string(containsString("1,234.50")))
                .andExpect(content().string(containsString("High · 73/100")));
    }

    @Test
    void rendersDetail() throws Exception {
        when(client.get("acct_0001")).thenReturn(account);
        mvc.perform(get("/accounts/acct_0001")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Days since last login")))
                .andExpect(content().string(containsString("2024-01-02")))
                .andExpect(content().string(containsString("aria-label=\"Risk: High · 73/100\"")));
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
    void highRiskPersistsWithSearchIndustryAndBothPaginationLinks() throws Exception {
        when(client.list("technology", "Cedar & Labs", 50, true))
                .thenReturn(new AccountsPage(List.of(account), 120, 50, 50));
        mvc.perform(get("/").param("industry", "technology").param("q", "Cedar & Labs")
                .param("offset", "50").param("high_risk", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"true\" selected=\"selected\"")))
                .andExpect(content().string(containsString("value=\"Cedar &amp; Labs\"")))
                .andExpect(content().string(containsString("value=\"technology\" selected=\"selected\"")))
                .andExpect(content().string(containsString(
                        "/?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=0")))
                .andExpect(content().string(containsString(
                        "/?industry=technology&amp;q=Cedar%20%26%20Labs&amp;high_risk=true&amp;offset=100")));
    }

    @Test
    void rendersUnknownInDirectoryAndDetail() throws Exception {
        var unknown = new Account("legacy", "Legacy", "retail", "US", "basic",
                BigDecimal.ZERO, 0, 0, LocalDate.of(2024, 1, 2), null);
        when(client.list("", "", 0, false)).thenReturn(new AccountsPage(List.of(unknown), 1, 50, 0));
        when(client.get("legacy")).thenReturn(unknown);
        for (String path : List.of("/", "/accounts/legacy")) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("Risk: Unknown · Not scored")));
        }
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/healthz")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }
}
