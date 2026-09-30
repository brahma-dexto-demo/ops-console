package com.brahma.demo.ops;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Account(
        String id, String name, String industry, String country, String plan,
        @JsonProperty("monthly_spend_usd") BigDecimal monthlySpendUsd,
        @JsonProperty("open_tickets") int openTickets,
        @JsonProperty("days_since_last_login") int daysSinceLastLogin,
        @JsonProperty("created_at") LocalDate createdAt) {
}
