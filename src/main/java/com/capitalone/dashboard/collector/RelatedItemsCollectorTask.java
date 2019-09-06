package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.client.RelatedItemsClient;
import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.AutoDiscoveryRepositoryImpl;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.RelatedItemCollectorRepository;
import org.apache.commons.collections.CollectionUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <h1>RelatedItemsCollectorTask</h1>
 *
 * @since 08/20/2019
 */
@Component
public class RelatedItemsCollectorTask extends CollectorTask<RelatedItemsCollector> {

    private final Logger LOGGER = LoggerFactory.getLogger(RelatedItemsCollectorTask.class);
    private RelatedItemCollectorRepository relatedItemCollectorRepository;
    private AutoDiscoveryRepositoryImpl autoDiscoveryRepositoryImpl;
    private RelatedItemSettings settings;
    private static final String COLLECTOR_NAME = "RelatedItems Collector";
    private RelatedItemsClient relatedItemsClient;

    @Autowired
    public RelatedItemsCollectorTask(TaskScheduler taskScheduler,
                                     RelatedItemCollectorRepository relatedItemCollectorRepository,
                                     AutoDiscoveryRepositoryImpl autoDiscoveryRepositoryImpl,
                                     RelatedItemsClient relatedItemsClient,
                                     RelatedItemSettings settings) {
        super(taskScheduler, "RelatedItemsCollector");
        this.relatedItemCollectorRepository = relatedItemCollectorRepository;
        this.autoDiscoveryRepositoryImpl = autoDiscoveryRepositoryImpl;
        this.settings = settings;
        this.relatedItemsClient = relatedItemsClient;
    }

    @Override
    public void collect(RelatedItemsCollector collector) {
        LOGGER.info("RelatedItem Collector");
        logBanner("Collecting related items");
        refresh();
        long beginTime = getLastUpdated(collector);
        long endTime = System.currentTimeMillis();
        Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardsGrouped = relatedItemsClient.collectAllRelatedCollectorItems(beginTime, endTime);
        log("Fetched dashboards count :", endTime, dashboardsGrouped.size());
        List<AutoDiscovery> autoDiscoveries = relatedItemsClient.processRelatedCollectorItems(dashboardsGrouped);
        log("Fetched auto-discoveries for dashboards :", endTime, autoDiscoveries.size());
        List<AutoDiscovery> updated = relatedItemsClient.processAutoDiscoveryBatch(autoDiscoveries);
        log("RelatedItems Collector executed successfully--- requests sent to subscriber", endTime, updated.size());
    }

    @Override
    public RelatedItemsCollector getCollector() {
        return RelatedItemsCollector.prototype(this.settings.getSubscribers());
    }

    @Override
    public BaseCollectorRepository<RelatedItemsCollector> getCollectorRepository() {
        return relatedItemCollectorRepository;
    }

    /**
     * This property helps to determine RelatedItem Collector execution interval
     */
    @Override
    public String getCron() {
        return this.settings.getCron();
    }

    private long getLastUpdated(Collector collector) {
        if (!Objects.isNull(collector.getLastExecuted()) && collector.getLastExecuted() != 0) {
            return collector.getLastExecuted();
        } else {
            return System.currentTimeMillis() - settings.getOffSet();
        }
    }

    private void refresh(){
        long start = System.currentTimeMillis();
       List<AutoDiscovery> autoDiscoveries=  autoDiscoveryRepositoryImpl.findAllAutoDiscoveriesWithStatusNew();
        if(CollectionUtils.isNotEmpty(autoDiscoveries)){
            List<AutoDiscovery> updated = relatedItemsClient.processAutoDiscoveryBatch(autoDiscoveries);
            log("Refresh -> retried POST on ",start,updated.size());
        }
    }
}
