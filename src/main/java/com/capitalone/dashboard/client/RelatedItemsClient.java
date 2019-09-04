package com.capitalone.dashboard.client;

import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscovery;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Map;

public interface RelatedItemsClient {

    Map<ObjectId,List<AutoDiscoverCollectorItem>> collectAllRelatedCollectorItems(long beginDate, long endDate);

    List<AutoDiscovery> processRelatedCollectorItems(Map<ObjectId,List<AutoDiscoverCollectorItem>> dashboardGroupings);

    List<AutoDiscovery> processAutoDiscoveryBatch(List<AutoDiscovery> autoDiscoveries);

}
