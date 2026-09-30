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
            "business", new BigDecimal("1234.50"), 3, 12, LocalDate.of(2024, 1, 2));

    @Test
    void rendersFilteredTable() throws Exception {
        when(client.list("technology", "Cedar", 0)).thenReturn(new AccountsPage(List.of(account), 1, 50, 0));
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
        when(client.list("", "unknown", 0)).thenReturn(new AccountsPage(List.of(), 0, 50, 0));
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
