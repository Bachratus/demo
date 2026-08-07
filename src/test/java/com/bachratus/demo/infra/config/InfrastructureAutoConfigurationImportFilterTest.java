package com.bachratus.demo.infra.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class InfrastructureAutoConfigurationImportFilterTest {

    private static final String OTHER_AUTO_CONFIGURATION = "example.OtherAutoConfiguration";

    @Test
    void shouldImportKafkaAutoConfigurationWhenKafkaFlagIsMissing() {
        // given
        InfrastructureAutoConfigurationImportFilter filter = new InfrastructureAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment());

        // when
        boolean[] matches = filter.match(autoConfigurations(), null);

        // then
        assertThat(matches).containsExactly(true, true);
    }

    @Test
    void shouldImportKafkaAutoConfigurationWhenKafkaFlagIsTrue() {
        // given
        InfrastructureAutoConfigurationImportFilter filter = new InfrastructureAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment().withProperty("app.kafka.enabled", "true"));

        // when
        boolean[] matches = filter.match(autoConfigurations(), null);

        // then
        assertThat(matches).containsExactly(true, true);
    }

    @Test
    void shouldExcludeOnlyKafkaAutoConfigurationWhenKafkaFlagIsFalse() {
        // given
        InfrastructureAutoConfigurationImportFilter filter = new InfrastructureAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment().withProperty("app.kafka.enabled", "false"));

        // when
        boolean[] matches = filter.match(autoConfigurations(), null);

        // then
        assertThat(matches).containsExactly(false, true);
    }

    @Test
    void shouldImportEverythingWhenEnvironmentWasNotInjectedYet() {
        // given
        InfrastructureAutoConfigurationImportFilter filter = new InfrastructureAutoConfigurationImportFilter();

        // when
        boolean[] matches = filter.match(autoConfigurations(), null);

        // then
        assertThat(matches).containsExactly(true, true);
    }

    @Test
    void shouldIgnoreNullAutoConfigurationClassNames() {
        // given
        InfrastructureAutoConfigurationImportFilter filter = new InfrastructureAutoConfigurationImportFilter();
        filter.setEnvironment(new MockEnvironment().withProperty("app.kafka.enabled", "false"));

        // when
        boolean[] matches = filter.match(new String[]{null, KafkaAutoConfiguration.class.getName()}, null);

        // then
        assertThat(matches).containsExactly(true, false);
    }

    private String[] autoConfigurations() {
        return new String[]{
                KafkaAutoConfiguration.class.getName(),
                OTHER_AUTO_CONFIGURATION
        };
    }
}
