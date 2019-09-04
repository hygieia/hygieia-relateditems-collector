package com.capitalone.dashboard.collector;

import com.capitalone.dashboard.client.RelatedItemsClient;
import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.RelatedCollectorItemRepository;
import com.capitalone.dashboard.repository.RelatedItemCollectorRepository;
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
 * @since 09/28/2018
 */
@Component
public class RelatedItemsCollectorTask extends CollectorTask<RelatedItemsCollector> {

    private final Logger LOGGER = LoggerFactory.getLogger(RelatedItemsCollectorTask.class);
    private DashboardRepository dashboardRepository;
    private ComponentRepository componentRepository;
    private CollectorItemRepository collectorItemRepository;
    private RelatedCollectorItemRepository relatedCollectorItemRepository;
    private RelatedItemCollectorRepository relatedItemCollectorRepository;
    private RelatedItemSettings settings;
    private static final String COLLECTOR_NAME = "RelatedItems Collector";
    private RelatedItemsClient relatedItemsClient;

    @Autowired
    public RelatedItemsCollectorTask(TaskScheduler taskScheduler, DashboardRepository dashboardRepository,
                                     ComponentRepository componentRepository,
                                     CollectorItemRepository collectorItemRepository,
                                     RelatedCollectorItemRepository relatedCollectorItemRepository,
                                     RelatedItemCollectorRepository relatedItemCollectorRepository,
                                     RelatedItemsClient relatedItemsClient,
                                     RelatedItemSettings settings) {
        super(taskScheduler, "RelatedItemsCollector");
        this.dashboardRepository = dashboardRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.relatedCollectorItemRepository = relatedCollectorItemRepository;
        this.relatedItemCollectorRepository = relatedItemCollectorRepository;
        this.settings = settings;
        this.relatedItemsClient = relatedItemsClient;
    }

    @Override
    public void collect(RelatedItemsCollector collector) {
        LOGGER.info("RelatedItem Collector");
        logBanner("Collecting related items");
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
     * This property helps to determine AuditStatus Collector execution interval
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
}
