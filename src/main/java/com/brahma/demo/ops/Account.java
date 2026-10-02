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

    public String riskBand() {
        if (riskScore == null) return "Unknown";
        if (riskScore < 40) return "Low";
        if (riskScore < 70) return "Medium";
        return "High";
    }

    public String riskClass() {
        return "risk-" + riskBand().toLowerCase(java.util.Locale.ROOT);
    }

    public String riskLabel() {
        return riskScore == null ? "Unknown · Not scored" : riskBand() + " · " + riskScore + "/100";
    }
}
