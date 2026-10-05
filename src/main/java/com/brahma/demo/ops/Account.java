package com.brahma.demo.ops;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record Account(
        String id, String name, String industry, String country, String plan,
        @JsonProperty("monthly_spend_usd") BigDecimal monthlySpendUsd,
        @JsonProperty("open_tickets") int openTickets,
        @JsonProperty("days_since_last_login") int daysSinceLastLogin,
        @JsonProperty("created_at") LocalDate createdAt,
        @JsonProperty("risk_score") Integer riskScore) {
    public Account {
        if (riskScore != null && (riskScore < 0 || riskScore > 100)) {
            throw new IllegalArgumentException("risk_score must be between 0 and 100");
        }
    }

    public String riskLabel() {
        if (riskScore == null) return "Not scored";
        if (riskScore < 40) return "Low";
        if (riskScore < 70) return "Medium";
        return "High";
    }

    public String riskClass() {
        if (riskScore == null) return "risk-neutral";
        if (riskScore < 40) return "risk-low";
        if (riskScore < 70) return "risk-medium";
        return "risk-high";
    }
}
