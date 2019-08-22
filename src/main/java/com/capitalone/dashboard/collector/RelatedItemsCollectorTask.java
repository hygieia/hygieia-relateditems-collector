package com.capitalone.dashboard.collector;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.DashboardType;

import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.BaseCollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.RelatedCollectorItemRepository;
import com.capitalone.dashboard.repository.RelatedItemCollectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

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

    @Autowired
    public RelatedItemsCollectorTask(TaskScheduler taskScheduler, DashboardRepository dashboardRepository,
                                        ComponentRepository componentRepository,
                                        CollectorItemRepository collectorItemRepository,
                                        RelatedCollectorItemRepository relatedCollectorItemRepository,
                                     RelatedItemCollectorRepository relatedItemCollectorRepository) {
        super(taskScheduler, COLLECTOR_NAME);
        this.dashboardRepository = dashboardRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.relatedCollectorItemRepository = relatedCollectorItemRepository;
        this.relatedItemCollectorRepository = relatedItemCollectorRepository;
        this.settings = settings;
    }

    @Override
    public void collect(RelatedItemsCollector collector) {
        LOGGER.info("RelatedItem Collector");
        Iterable<Dashboard> dashboards = dashboardRepository.findAllByType(DashboardType.Team);

        LOGGER.info("RelatedItems Collector executed successfully");
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

}
