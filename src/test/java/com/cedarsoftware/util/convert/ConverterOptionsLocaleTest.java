package com.cedarsoftware.util.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class ConverterOptionsLocaleTest {

    @Test
    void defaultLocaleMatchesSystemLocale() {
        ConverterOptions options = new ConverterOptions() { };
        assertThat(options.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void customLocaleReturnedWhenOverridden() {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public Locale getLocale() {
                return Locale.CANADA_FRENCH;
            }
        };
        assertThat(options.getLocale()).isEqualTo(Locale.CANADA_FRENCH);
    }
}
