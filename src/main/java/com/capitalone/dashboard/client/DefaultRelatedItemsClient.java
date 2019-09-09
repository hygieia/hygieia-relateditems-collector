package com.capitalone.dashboard.client;

import com.capitalone.dashboard.collector.RelatedItemSettings;
import com.capitalone.dashboard.misc.HygieiaException;
import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.AutoDiscoveryMetaData;
import com.capitalone.dashboard.model.AutoDiscoveryRemoteRequest;
import com.capitalone.dashboard.model.AutoDiscoveryStatusType;
import com.capitalone.dashboard.model.AutoDiscoverySubscriberRemoteRequest;
import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorItem;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.Dashboard;
import com.capitalone.dashboard.model.relation.RelatedCollectorItem;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.RelatedCollectorItemRepository;
import com.capitalone.dashboard.util.Supplier;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultRelatedItemsClient implements RelatedItemsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRelatedItemsClient.class);
    private final RestOperations restOperations;
    private RelatedItemSettings relatedItemSettings;
    private String bearerToken;
    private RelatedCollectorItemRepository relatedCollectorItemRepository;
    private DashboardRepository dashboardRepository;
    private ComponentRepository componentRepository;
    private CollectorItemRepository collectorItemRepository;
    private CollectorRepository collectorRepository;
    private AutoDiscoveryRepository autoDiscoveryRepository;
    private static final String BEARER = "Bearer ";
    private static final String HTTP_CODE_200 = "200";


    @Autowired
    public DefaultRelatedItemsClient(RelatedItemSettings relatedItemSettings, Supplier<RestOperations> restOperationsSupplier,
                                     RelatedCollectorItemRepository relatedCollectorItemRepository,
                                     DashboardRepository dashboardRepository,
                                     ComponentRepository componentRepository,
                                     CollectorItemRepository collectorItemRepository,
                                     CollectorRepository collectorRepository,
                                     AutoDiscoveryRepository autoDiscoveryRepository) {
        this.relatedItemSettings = relatedItemSettings;
        this.restOperations = restOperationsSupplier.get();
        this.relatedCollectorItemRepository = relatedCollectorItemRepository;
        this.dashboardRepository = dashboardRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.collectorRepository = collectorRepository;
        this.autoDiscoveryRepository = autoDiscoveryRepository;

    }


    @Override
    public Map<ObjectId, List<AutoDiscoverCollectorItem>> collectAllRelatedCollectorItems(long beginDate, long endDate) {
        List<RelatedCollectorItem> relatedCollectorItems = relatedCollectorItemRepository.findAllByCreationTimeIsBetweenOrderByCreationTimeDesc(beginDate - 1, endDate + 1);
        Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping = new HashMap<>();
        relatedCollectorItems.forEach(rci -> {
            CollectorItem leftCollectorItem = collectorItemRepository.findOne(rci.getLeft());
            CollectorItem rightCollectorItem = collectorItemRepository.findOne(rci.getRight());
            // find components
            List<com.capitalone.dashboard.model.Component> components = new ArrayList<>();
            components = getComponentsAndProcessExclusions(leftCollectorItem, rightCollectorItem, components);
            Collection<com.capitalone.dashboard.model.Component> c = components.stream().collect(Collectors.toConcurrentMap(com.capitalone.dashboard.model.Component::getId, Function.identity(), (p, q) -> p)).values();
            List<com.capitalone.dashboard.model.Component> distinctComponents = new ArrayList<>(c);
            distinctComponents.forEach(component -> {
                List<Dashboard> dashboards = dashboardRepository.findByApplicationComponentIdsIn(Arrays.asList(component.getId()));
                dashboards.forEach(dashboard -> {
                    // if (dashboard.getTitle().equalsIgnoreCase("CI383423")){
                    List<AutoDiscovery> autoDiscoveries = autoDiscoveryRepository.findByMetaDataTitle(dashboard.getTitle());
                    //  if there is no entry for dashboard in auto_discovery collection, add one
                    if (CollectionUtils.isEmpty(autoDiscoveries)) {
                        initialCall(dashboardGrouping, leftCollectorItem, rightCollectorItem, dashboard);
                    } else {
                        autoDiscoveries.forEach(ad -> {
                            AutoDiscoverCollectorItem l = getAutoDiscoveryCollectorItemForCollectorTypes(leftCollectorItem, ad);
                            AutoDiscoverCollectorItem r = getAutoDiscoveryCollectorItemForCollectorTypes(rightCollectorItem, ad);
                            List<AutoDiscoverCollectorItem> refs = new ArrayList<>();
                            if (Objects.isNull(l)) {
                                convertToAutoDiscoverCollectorItem(leftCollectorItem, refs);
                            }
                            if (Objects.isNull(r)) {
                                convertToAutoDiscoverCollectorItem(rightCollectorItem, refs);
                            }
                            if (dashboardGrouping.containsKey(dashboard.getId())) {
                                List<AutoDiscoverCollectorItem> items = dashboardGrouping.get(dashboard.getId());
                                refs.addAll(items);
                                if (CollectionUtils.isNotEmpty(refs)) {
                                    addAllDistinctCollectorItemsToMap(dashboardGrouping, dashboard, refs);
                                }
                            } else {
                                if (CollectionUtils.isNotEmpty(refs)) {
                                    addAllDistinctCollectorItemsToMap(dashboardGrouping, dashboard, refs);
                                }

                            }
                        });
                    }
                });
            });
        });
        LOGGER.info("Dashboards found auto discovered : " + dashboardGrouping.size());
        return dashboardGrouping;
    }

    @Override
    public List<AutoDiscovery> processRelatedCollectorItems(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGroupings) {
        List<Dashboard> dashboards = new ArrayList<>();
        List<AutoDiscovery> autoDiscoveries = new ArrayList<>();
        dashboardGroupings.keySet().forEach(id -> {
            List<AutoDiscoverCollectorItem> adcis = dashboardGroupings.get(id);
            AutoDiscovery autoDiscovery = new AutoDiscovery();
            Dashboard dashboard = dashboardRepository.findOne(id);
            if (Objects.nonNull(dashboard)) {
                dashboards.add(dashboard);
                setAutoDiscoverMetaData(autoDiscovery, dashboard);
                adcis.forEach(adci -> {
                    Collector collector = collectorRepository.findOne(adci.getCollectorId());
                    CollectorType collectorType = collector.getCollectorType();
                    setEntries(autoDiscovery, adci, collectorType);
                });
                List<AutoDiscovery> ads = autoDiscoveryRepository.findByMetaDataTitle(autoDiscovery.getMetaData().getTitle());
                if (CollectionUtils.isNotEmpty(ads)) {
                    AutoDiscovery ad = ads.stream().findAny().get();
                    AutoDiscovery adConsolidate = new AutoDiscovery();
                    updateExistingEntriesInDashboard(autoDiscovery, ad, adConsolidate);
                    autoDiscoveryRepository.save(adConsolidate);
                    autoDiscoveries.add(adConsolidate);
                } else {
                    autoDiscoveryRepository.save(autoDiscovery);
                    autoDiscovery.setCreatedTimestamp(System.currentTimeMillis());
                    autoDiscoveries.add(autoDiscovery);
                }
                LOGGER.info("Auto discovered entries for dashboard : " + dashboard.getTitle());
            }
        });
        return autoDiscoveries;
    }


    @Override
    public List<AutoDiscovery> processAutoDiscoveryBatch(List<AutoDiscovery> autoDiscoveries) {
        OAuth2AccessToken accessToken = restTemplate().getAccessToken();
        List<AutoDiscovery> updated = new ArrayList<>();
        String token = BEARER + accessToken.getValue();
        setBearerToken(token);
        if (CollectionUtils.isNotEmpty(autoDiscoveries)) {
            autoDiscoveries.forEach(autoDiscovery -> {
                AutoDiscoverySubscriberRemoteRequest autoDiscoverySubscriberRemoteRequest = new AutoDiscoverySubscriberRemoteRequest();
                autoDiscoverySubscriberRemoteRequest.setSystemsToRequest(Arrays.asList(relatedItemSettings.getSubscriberName()));
                autoDiscoverySubscriberRemoteRequest.setAutoDiscoveryRequest(createRequest(autoDiscovery));
                String json = new Gson().toJson(autoDiscoverySubscriberRemoteRequest);
                ResponseEntity<String> response = makeRestCall(json, token);
                if (Objects.nonNull(response)) {
                    String response_Code = response.getStatusCode().toString();
                    if (response_Code.equalsIgnoreCase(HTTP_CODE_200)) {
                        autoDiscovery.getAllEntries().stream().forEach(entry -> {
                            if (entry.getStatus().equals(AutoDiscoveryStatusType.NEW)) {
                                entry.setStatus(AutoDiscoveryStatusType.AWAITING_USER_RESPONSE);
                            }
                        });
                        autoDiscoveryRepository.save(autoDiscovery);
                        updated.add(autoDiscovery);
                    }
                }
            });
        }
        return updated;
    }

    private ResponseEntity<String> makeRestCall(String payload, String token) {
        ResponseEntity<String> response = null;
        try {
            response = restOperations.exchange(getSubscriberUrl(), HttpMethod.POST, new HttpEntity<>(payload, createHeaders(token, relatedItemSettings.getAccept())), String.class);
        } catch (RestClientException re) {
            LOGGER.error("Error with REST url: " + getSubscriberUrl());
            LOGGER.error(re.getMessage());
        }
        return response;
    }

    private AutoDiscoverCollectorItem getAutoDiscoveryCollectorItemForCollectorTypes(CollectorItem leftCollectorItem, AutoDiscovery ad) {
        Collector collector = collectorRepository.findOne(leftCollectorItem.getCollectorId());
        AutoDiscoveredEntry adEntry;
        switch (collector.getCollectorType()) {
            case Build:
                adEntry = ad.getBuildEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case SCM:
                adEntry = ad.getCodeRepoEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case CodeQuality:
                adEntry = ad.getStaticCodeEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case LibraryPolicy:
                adEntry = ad.getLibraryScanEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case StaticSecurityScan:
                adEntry = ad.getSecurityScanEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case Deployment:
                adEntry = ad.getDeploymentEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case Artifact:
                adEntry = ad.getArtifactEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case AgileTool:
                adEntry = ad.getFeatureEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);

            case Test:
                adEntry = ad.getFunctionalTestEntries().stream().filter(entry -> matchOptions(entry.getOptions(), leftCollectorItem.getOptions(), collector)).findAny().orElse(null);
                return getAutoDiscoverCollectorItem(leftCollectorItem, collector, adEntry);
        }
        return null;
    }

    private AutoDiscoverCollectorItem getAutoDiscoverCollectorItem(CollectorItem collectorItem, Collector collector, AutoDiscoveredEntry adEntry) {
        AutoDiscoverCollectorItem leftAdci = null;
        if (Objects.nonNull(adEntry)) {
            try {
                leftAdci = adEntry.toAutoDiscoverCollectorItem(collector);
                leftAdci.setId(collectorItem.getId());
            } catch (HygieiaException e) {
                LOGGER.info("Exception occured in DefaultRelatedItemsClient.getAutoDiscoverCollectorItem()- invalid fields for collector type found.", e.getMessage());
            }
        }
        return leftAdci;
    }

    private AutoDiscoverCollectorItem toAutoDiscoverCollectorItem(CollectorItem collectorItem) {
        AutoDiscoverCollectorItem autoDiscoverCollectorItem = new AutoDiscoverCollectorItem();
        if (Objects.nonNull(collectorItem)) {
            autoDiscoverCollectorItem.setId(collectorItem.getId());
            autoDiscoverCollectorItem.setAutoDiscoverStatus(AutoDiscoveryStatusType.NEW);
            autoDiscoverCollectorItem.setCollector(collectorItem.getCollector());
            autoDiscoverCollectorItem.setOptions(collectorItem.getOptions());
            autoDiscoverCollectorItem.setCollectorId(collectorItem.getCollectorId());
            autoDiscoverCollectorItem.setEnabled(collectorItem.isEnabled());
            autoDiscoverCollectorItem.setPushed(collectorItem.isPushed());
            autoDiscoverCollectorItem.setDescription(collectorItem.getDescription());
            autoDiscoverCollectorItem.setLastUpdated(collectorItem.getLastUpdated());
            autoDiscoverCollectorItem.setNiceName(collectorItem.getNiceName());
        }
        return autoDiscoverCollectorItem;
    }


    private boolean matchOptions(Map<String, Object> existingOptions, Map<String, Object> incomingOptions, Collector collector) {
        boolean optionsMatched = true;
        if (Objects.nonNull(collector)) {
            Map<String, Object> uniqueFields = collector.getUniqueFields();
            for (String field : uniqueFields.keySet()) {
                try {
                    if (!((String) existingOptions.get(field)).equalsIgnoreCase((String) incomingOptions.get(field))) {
                        optionsMatched = false;
                    }
                } catch (Exception e) {
                    LOGGER.info("Caught exception in DefaultRelatedItemsClient.matchOptions()-- invalid options for collectorItem." + e.getMessage());
                }
            }
        }
        return optionsMatched;
    }

    private List<AutoDiscoveredEntry> consolidate(List<AutoDiscoveredEntry> a, List<AutoDiscoveredEntry> b) {
        List<AutoDiscoveredEntry> c = new ArrayList<>();
        c.addAll(a);
        c.addAll(b);
        return c;
    }

    private AutoDiscoveryRemoteRequest createRequest(AutoDiscovery autoDiscovery) {
        AutoDiscoveryRemoteRequest autoDiscoveryRemoteRequest = new AutoDiscoveryRemoteRequest();
        autoDiscoveryRemoteRequest.setMetaData(autoDiscovery.getMetaData());
        autoDiscoveryRemoteRequest.setCodeRepoEntries(filterNew(autoDiscovery.getCodeRepoEntries()));
        autoDiscoveryRemoteRequest.setBuildEntries(filterNew(autoDiscovery.getBuildEntries()));
        autoDiscoveryRemoteRequest.setArtifactEntries(filterNew(autoDiscovery.getArtifactEntries()));
        autoDiscoveryRemoteRequest.setLibraryScanEntries(filterNew(autoDiscovery.getLibraryScanEntries()));
        autoDiscoveryRemoteRequest.setStaticCodeEntries(filterNew(autoDiscovery.getStaticCodeEntries()));
        autoDiscoveryRemoteRequest.setSecurityScanEntries(filterNew(autoDiscovery.getSecurityScanEntries()));
        autoDiscoveryRemoteRequest.setDeploymentEntries(filterNew(autoDiscovery.getDeploymentEntries()));
        autoDiscoveryRemoteRequest.setFunctionalTestEntries(filterNew(autoDiscovery.getFunctionalTestEntries()));
        autoDiscoveryRemoteRequest.setFeatureEntries(filterNew(autoDiscovery.getFeatureEntries()));
        autoDiscoveryRemoteRequest.setAutoDiscoveryId(autoDiscovery.getId().toString());
        return autoDiscoveryRemoteRequest;
    }

    private List<AutoDiscoveredEntry> filterNew(List<AutoDiscoveredEntry> entries) {
        if (CollectionUtils.isNotEmpty(entries)) {
            return entries.stream().filter(entry -> entry.getStatus().equals(AutoDiscoveryStatusType.NEW)).collect(Collectors.toList());
        } else return new ArrayList<>();

    }

    private HttpHeaders createHeaders(final String token, final String accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token);
        headers.set("accept", accept);
        return headers;
    }


    private OAuth2RestOperations restTemplate() {
        AccessTokenRequest atr = new DefaultAccessTokenRequest();
        return new OAuth2RestTemplate(resource(), new DefaultOAuth2ClientContext(atr));
    }

    private OAuth2ProtectedResourceDetails resource() {
        ClientCredentialsResourceDetails resource = new ClientCredentialsResourceDetails();
        resource.setAccessTokenUri(relatedItemSettings.getTokenUrl());
        resource.setClientId(relatedItemSettings.getClientId());
        resource.setClientSecret(relatedItemSettings.getClientSecret());
        resource.setGrantType("client_credentials");
        resource.setScope(new ArrayList<>());
        return resource;
    }

    private String getSubscriberUrl() {
        return relatedItemSettings.getSubscribers().get(0);
    }

    private void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }


    private CollectorType findCollectorType(ObjectId collectorId) {
        Collector collector = collectorRepository.findOne(collectorId);
        return collector.getCollectorType();
    }

    private List<com.capitalone.dashboard.model.Component> getComponentsForCollectorItemId(CollectorItem collectorItem) {
        if (Objects.isNull(collectorItem)) return null;
        CollectorType collectorType = findCollectorType(collectorItem.getCollectorId());
        List<com.capitalone.dashboard.model.Component> components = new ArrayList<>();
        switch (collectorType) {
            case Build:
                return componentRepository.findByBuildCollectorItemId(collectorItem.getId());
            case SCM:
                return componentRepository.findBySCMCollectorItemId(collectorItem.getId());
            case Artifact:
                return componentRepository.findByArtifactCollectorItems(collectorItem.getId());
            case LibraryPolicy:
                return componentRepository.findByLibraryPolicyCollectorItems(collectorItem.getId());
            case CodeQuality:
                return componentRepository.findByCodeQualityCollectorItems(collectorItem.getId());
            case StaticSecurityScan:
                return componentRepository.findByStaticSecurityScanCollectorItems(collectorItem.getId());
            case Test:
                return componentRepository.findByTestCollectorItems(collectorItem.getId());
        }
        return components;
    }

    private void addAllDistinctCollectorItemsToMap(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping, Dashboard dashboard, List<AutoDiscoverCollectorItem> refs) {
        Iterables.removeIf(refs, Predicates.isNull());
        Collection<AutoDiscoverCollectorItem> rs = refs.stream().collect(Collectors.toConcurrentMap(AutoDiscoverCollectorItem::getId, Function.identity(), (p, q) -> p)).values();
        List<AutoDiscoverCollectorItem> distinctItems = new ArrayList<>(rs);
        dashboardGrouping.put(dashboard.getId(), distinctItems);
    }

    private void convertToAutoDiscoverCollectorItem(CollectorItem leftCollectorItem, List<AutoDiscoverCollectorItem> refs) {
        AutoDiscoverCollectorItem lf = toAutoDiscoverCollectorItem(leftCollectorItem);
        refs.add(lf);
    }

    private void initialCall(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping, CollectorItem leftCollectorItem, CollectorItem rightCollectorItem, Dashboard dashboard) {
        AutoDiscoverCollectorItem left;
        AutoDiscoverCollectorItem right;
        left = toAutoDiscoverCollectorItem(leftCollectorItem);
        right = toAutoDiscoverCollectorItem(rightCollectorItem);
        if (dashboardGrouping.containsKey(dashboard.getId())) {
            List<AutoDiscoverCollectorItem> items = dashboardGrouping.get(dashboard.getId());
            List<AutoDiscoverCollectorItem> refs = new ArrayList<>();
            refs.addAll(Arrays.asList(left, right));
            refs.addAll(items);
            Iterables.removeIf(refs, Predicates.isNull());
            if (CollectionUtils.isNotEmpty(refs)) {
                Collection<AutoDiscoverCollectorItem> rs = refs.stream().collect(Collectors.toConcurrentMap(AutoDiscoverCollectorItem::getId, Function.identity(), (p, q) -> p)).values();
                List<AutoDiscoverCollectorItem> distinctItems = new ArrayList<>(rs);
                dashboardGrouping.put(dashboard.getId(), distinctItems);
            }
        } else {
            dashboardGrouping.put(dashboard.getId(), Arrays.asList(left, right));
        }
    }

    private List<com.capitalone.dashboard.model.Component> getComponentsAndProcessExclusions(CollectorItem leftCollectorItem, CollectorItem rightCollectorItem, List<com.capitalone.dashboard.model.Component> components) {
        List<String> exclusions = relatedItemSettings.getExclusions();
        if (CollectionUtils.isNotEmpty(exclusions)) {
            for (String exclusion : exclusions) {
                String[] ex = StringUtils.split(exclusion, "|");
                if (Objects.isNull(leftCollectorItem.getOptions().get(ex[0])) || !leftCollectorItem.getOptions().get(ex[0]).equals(ex[1])) {
                    components = getComponentsForCollectorItemId(leftCollectorItem);
                }
                if (Objects.isNull(rightCollectorItem.getOptions().get(ex[0])) || !rightCollectorItem.getOptions().get(ex[0]).equals(ex[1])) {
                    components.addAll(getComponentsForCollectorItemId(rightCollectorItem));
                }
            }
        } else {
            components = getComponentsForCollectorItemId(leftCollectorItem);
            components.addAll(getComponentsForCollectorItemId(rightCollectorItem));
        }
        return components;
    }


    private void setAutoDiscoverMetaData(AutoDiscovery autoDiscovery, Dashboard dashboard) {
        AutoDiscoveryMetaData autoDiscoveryMetaData = new AutoDiscoveryMetaData();
        autoDiscoveryMetaData.setTitle(dashboard.getTitle());
        autoDiscoveryMetaData.setTemplate(dashboard.getTemplate());
        autoDiscoveryMetaData.setType(dashboard.getType().toString());
        autoDiscoveryMetaData.setApplicationName(dashboard.getApplication().getName());
        autoDiscoveryMetaData.setBusinessApplication(dashboard.getConfigurationItemBusAppName());
        autoDiscoveryMetaData.setBusinessService(dashboard.getConfigurationItemBusServName());
        autoDiscoveryMetaData.setComponentName(dashboard.getApplication().getComponents().get(0).getName());
        autoDiscovery.setMetaData(autoDiscoveryMetaData);
    }

    private void updateExistingEntriesInDashboard(AutoDiscovery autoDiscovery, AutoDiscovery ad, AutoDiscovery adConsolidate) {
        adConsolidate.setId(ad.getId());
        adConsolidate.setBuildEntries(consolidate(ad.getBuildEntries(), autoDiscovery.getBuildEntries()));
        adConsolidate.setCodeRepoEntries(consolidate(ad.getCodeRepoEntries(), autoDiscovery.getCodeRepoEntries()));
        adConsolidate.setArtifactEntries(consolidate(ad.getArtifactEntries(), autoDiscovery.getArtifactEntries()));
        adConsolidate.setStaticCodeEntries(consolidate(ad.getStaticCodeEntries(), autoDiscovery.getStaticCodeEntries()));
        adConsolidate.setLibraryScanEntries(consolidate(ad.getLibraryScanEntries(), autoDiscovery.getLibraryScanEntries()));
        adConsolidate.setSecurityScanEntries(consolidate(ad.getSecurityScanEntries(), autoDiscovery.getSecurityScanEntries()));
        adConsolidate.setDeploymentEntries(consolidate(ad.getDeploymentEntries(), autoDiscovery.getDeploymentEntries()));
        adConsolidate.setFunctionalTestEntries(consolidate(ad.getFunctionalTestEntries(), autoDiscovery.getFunctionalTestEntries()));
        adConsolidate.setFeatureEntries(consolidate(ad.getFeatureEntries(), autoDiscovery.getFeatureEntries()));
        adConsolidate.setMetaData(ad.getMetaData());
    }

    private void setEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        RelatedItemsUtils.setBuildEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setCodeRepoEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setArtifactEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setLibraryPolicyEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setStaticCodeEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setStaticSecurityEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setDeployEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setFunctionalTestEntries(autoDiscovery, adci, collectorType);
        RelatedItemsUtils.setFeatureEntries(autoDiscovery, adci, collectorType);
    }

}
