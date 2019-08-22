package com.capitalone.dashboard.collector.config;
import com.capitalone.dashboard.collector.RelatedItemSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.Order;
import java.util.Arrays;

/**
 * Spring context configuration for Testing purposes
 */
@Order(1)
@Configuration
@ComponentScan(basePackages ={"com.capitalone.dashboard.model"},
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value= RelatedItemSettings.class))
public class TestConfig {

    @Bean
    public RelatedItemSettings settings() {
        // Test Config Settings
        RelatedItemSettings settings = new RelatedItemSettings();
        settings.setCron("*/2 * * * *");
        settings.setSubscribers(Arrays.asList("http://localhost:8081/"));
        return settings;
    }

}
