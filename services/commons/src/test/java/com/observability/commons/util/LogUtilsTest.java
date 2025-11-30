package com.observability.commons.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogUtils}.
 */
class LogUtilsTest {

    @Test
    void sanitizeForLogReturnsNullStringForNullInput() {
        final var result = LogUtils.sanitizeForLog(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void sanitizeForLogReplacesNewlines() {
        final var result = LogUtils.sanitizeForLog("line1\nline2");
        assertThat(result).isEqualTo("line1_line2");
    }

    @Test
    void sanitizeForLogReplacesCarriageReturns() {
        final var result = LogUtils.sanitizeForLog("line1\rline2");
        assertThat(result).isEqualTo("line1_line2");
    }

    @Test
    void sanitizeForLogReplacesTabs() {
        final var result = LogUtils.sanitizeForLog("col1\tcol2");
        assertThat(result).isEqualTo("col1_col2");
    }

    @Test
    void sanitizeForLogReplacesMultipleSpecialChars() {
        final var result = LogUtils.sanitizeForLog("a\nb\rc\td");
        assertThat(result).isEqualTo("a_b_c_d");
    }

    @Test
    void sanitizeForLogReturnsUnmodifiedStringWithoutSpecialChars() {
        final var input = "normal string";
        final var result = LogUtils.sanitizeForLog(input);
        assertThat(result).isEqualTo(input);
    }
}
