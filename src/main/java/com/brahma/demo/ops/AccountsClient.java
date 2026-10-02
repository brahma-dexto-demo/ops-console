package com.brahma.demo.ops;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AccountsClient {
    private final RestClient client;

    public AccountsClient(RestClient.Builder builder,
                          @Value("${accounts.api.url}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public AccountsPage list(String industry, String q, boolean highRisk, int offset) {
        return client.get().uri(builder -> {
            builder.path("/accounts").queryParam("limit", 50).queryParam("offset", offset);
            if (industry != null && !industry.isBlank()) builder.queryParam("industry", "{industry}");
            if (q != null && !q.isBlank()) builder.queryParam("q", "{q}");
            if (highRisk) builder.queryParam("high_risk", "true");
            var variables = new java.util.HashMap<String, String>();
            variables.put("industry", industry == null ? "" : industry);
            variables.put("q", q == null ? "" : q);
            return builder.build(variables);
        }).retrieve().body(AccountsPage.class);
    }

    public Account get(String id) {
        return client.get().uri("/accounts/{id}", id).retrieve().body(Account.class);
    }
}
