package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorType;

import java.util.ArrayList;
import java.util.List;

public class RelatedItemsCollector extends Collector {

    private List<String> subscribers = new ArrayList<>();

    /**
    * RelatedItems Collector Instance built with required config settings
    */
    public static RelatedItemsCollector prototype(List<String> subscribers) {
        RelatedItemsCollector protoType = new RelatedItemsCollector();
        protoType.setName("RelatedItemsCollector");
        protoType.setCollectorType(CollectorType.AutoDiscover);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        protoType.subscribers.addAll(subscribers);
        return protoType;
    }
    public List<String> getSubscribers() {
        return subscribers;
    }
}