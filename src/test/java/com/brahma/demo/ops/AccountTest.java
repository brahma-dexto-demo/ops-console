package com.brahma.demo.ops;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class AccountTest {
    @ParameterizedTest
    @CsvSource(value = {"0,Low", "39,Low", "40,Medium", "69,Medium", "70,High", "100,High", "null,Unknown"}, nullValues = "null")
    void riskBandsUseExactBoundaries(Integer score, String band) {
        var account = new Account("id", "name", "industry", "US", "plan", null, 0, 0, null, score);
        assertThat(account.riskScore()).isEqualTo(score);
        assertThat(account.riskBand()).isEqualTo(band);
        assertThat(account.riskClass()).isEqualTo("risk-" + band.toLowerCase(java.util.Locale.ROOT));
        assertThat(account.riskLabel()).isEqualTo(score == null ? "Unknown · Not scored" : band + " · " + score + "/100");
    }
}
