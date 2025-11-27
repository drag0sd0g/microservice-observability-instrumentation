package com.observability.inventory.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilsTest {

    @Test
    void sanitizeForLogReturnsNullStringForNullInput() {
        var result = LogUtils.sanitizeForLog(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void sanitizeForLogReplacesNewlines() {
        var result = LogUtils.sanitizeForLog("line1\nline2");
        assertThat(result).isEqualTo("line1_line2");
    }

    @Test
    void sanitizeForLogReplacesCarriageReturns() {
        var result = LogUtils.sanitizeForLog("line1\rline2");
        assertThat(result).isEqualTo("line1_line2");
    }

    @Test
    void sanitizeForLogReplacesTabs() {
        var result = LogUtils.sanitizeForLog("col1\tcol2");
        assertThat(result).isEqualTo("col1_col2");
    }

    @Test
    void sanitizeForLogReplacesMultipleSpecialChars() {
        var result = LogUtils.sanitizeForLog("a\nb\rc\td");
        assertThat(result).isEqualTo("a_b_c_d");
    }

    @Test
    void sanitizeForLogReturnsUnmodifiedStringWithoutSpecialChars() {
        var input = "normal string";
        var result = LogUtils.sanitizeForLog(input);
        assertThat(result).isEqualTo(input);
    }
}
