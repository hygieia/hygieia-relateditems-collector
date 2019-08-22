package com.capitalone.dashboard.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Bean to hold settings specific to the RelatedItems Collector.
 */
@Component
@ConfigurationProperties(prefix = "relatedItem")
public class RelatedItemSettings {

    private String cron;
    private List<String> subscribers;

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public List<String> getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(List<String> subscribers) {
        this.subscribers = subscribers;
    }

}


