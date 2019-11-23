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
import com.capitalone.dashboard.model.FeatureFlag;
import com.capitalone.dashboard.model.relation.RelatedCollectorItem;
import com.capitalone.dashboard.repository.AutoDiscoveryRepository;
import com.capitalone.dashboard.repository.CollectorItemRepository;
import com.capitalone.dashboard.repository.CollectorRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.DashboardRepository;
import com.capitalone.dashboard.repository.FeatureFlagRepository;
import com.capitalone.dashboard.repository.RelatedCollectorItemRepository;
import com.capitalone.dashboard.util.FeatureFlagsEnum;
import com.capitalone.dashboard.util.HygieiaUtils;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DefaultRelatedItemsClient implements RelatedItemsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRelatedItemsClient.class);
    private final RestClient restClient;
    private final RelatedItemSettings relatedItemSettings;
    private final RelatedCollectorItemRepository relatedCollectorItemRepository;
    private final DashboardRepository dashboardRepository;
    private final ComponentRepository componentRepository;
    private final CollectorItemRepository collectorItemRepository;
    private final CollectorRepository collectorRepository;
    private final AutoDiscoveryRepository autoDiscoveryRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private static final String BEARER = "Bearer ";
    private static final String HTTP_CODE_200 = "200";


    @Autowired
    public DefaultRelatedItemsClient(RelatedItemSettings relatedItemSettings, RestClient restClient,
                                     RelatedCollectorItemRepository relatedCollectorItemRepository,
                                     DashboardRepository dashboardRepository,
                                     ComponentRepository componentRepository,
                                     CollectorItemRepository collectorItemRepository,
                                     CollectorRepository collectorRepository,
                                     AutoDiscoveryRepository autoDiscoveryRepository,
                                     FeatureFlagRepository featureFlagRepository ) {
        this.relatedItemSettings = relatedItemSettings;
        this.restClient = restClient;
        this.relatedCollectorItemRepository = relatedCollectorItemRepository;
        this.dashboardRepository = dashboardRepository;
        this.componentRepository = componentRepository;
        this.collectorItemRepository = collectorItemRepository;
        this.collectorRepository = collectorRepository;
        this.autoDiscoveryRepository = autoDiscoveryRepository;
        this.featureFlagRepository = featureFlagRepository;
    }


    @Override
    public Map<ObjectId, List<AutoDiscoverCollectorItem>> collectAllRelatedCollectorItems(long beginDate, long endDate) {
        Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping = new HashMap<>();
        List<RelatedCollectorItem> relatedCollectorItems = relatedCollectorItemRepository.findAllByCreationTimeIsBetweenOrderByCreationTimeDesc(beginDate - 1, endDate + 1);

        if (CollectionUtils.isEmpty(relatedCollectorItems)) return dashboardGrouping;

        FeatureFlag featureFlag = featureFlagRepository.findByName(FeatureFlagsEnum.auto_discover.toString());
        for (RelatedCollectorItem rci : relatedCollectorItems) {
            CollectorItem leftCollectorItem = collectorItemRepository.findOne(rci.getLeft());
            CollectorItem rightCollectorItem = collectorItemRepository.findOne(rci.getRight());

            if (Objects.isNull(leftCollectorItem) || Objects.isNull(rightCollectorItem)) continue;

            Collector leftCollector = collectorRepository.findOne(leftCollectorItem.getCollectorId());
            if (!Objects.isNull(leftCollector)) leftCollectorItem.setCollector(leftCollector);
            Collector rightCollector = collectorRepository.findOne(rightCollectorItem.getCollectorId());
            if (!Objects.isNull(rightCollector)) rightCollectorItem.setCollector(rightCollector);

            // find components based on the collectorItems and featureflag
            List<com.capitalone.dashboard.model.Component> components = new ArrayList<>();
            components = getComponentsAndProcessExclusions(leftCollectorItem, rightCollectorItem, components, featureFlag);

            Collection<com.capitalone.dashboard.model.Component> c = components.stream().collect(Collectors.toConcurrentMap(com.capitalone.dashboard.model.Component::getId, Function.identity(), (p, q) -> p)).values();
            List<com.capitalone.dashboard.model.Component> distinctComponents = new ArrayList<>(c);

            distinctComponents.forEach(component -> {
                List<Dashboard> dashboards = dashboardRepository.findByApplicationComponentIdsIn(Collections.singletonList(component.getId()));
                dashboards.forEach(dashboard -> {
                    LOGGER.info("processing dashboard ---" + dashboard.getTitle());
                    List<AutoDiscovery> autoDiscoveries = autoDiscoveryRepository.findByMetaDataTitle(dashboard.getTitle());

                    //  if there is no entry for dashboard in auto_discovery collection, add one
                    if (CollectionUtils.isEmpty(autoDiscoveries)) {
                        initialCall(dashboardGrouping, leftCollectorItem, rightCollectorItem, dashboard, featureFlag);
                    } else {
                        autoDiscoveries.forEach(ad -> {
                            AutoDiscoverCollectorItem l = getAutoDiscoveryCollectorItemForCollectorTypes(leftCollectorItem, ad, featureFlag);
                            AutoDiscoverCollectorItem r = getAutoDiscoveryCollectorItemForCollectorTypes(rightCollectorItem, ad, featureFlag);
                            List<AutoDiscoverCollectorItem> refs = new ArrayList<>();
                            if (Objects.isNull(l)) {
                                convertToAutoDiscoverCollectorItem(leftCollectorItem, refs, featureFlag);
                            }
                            if (Objects.isNull(r)) {
                                convertToAutoDiscoverCollectorItem(rightCollectorItem, refs, featureFlag);
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
        }
        LOGGER.info("Dashboards found auto discovered : " + dashboardGrouping.size());
        return dashboardGrouping;
    }

    @Override
    public List<AutoDiscovery> processRelatedCollectorItems(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGroupings) {
        List<AutoDiscovery> autoDiscoveries = new ArrayList<>();
        FeatureFlag featureFlag = featureFlagRepository.findByName(FeatureFlagsEnum.auto_discover.toString());
        dashboardGroupings.keySet().forEach(id -> {
            List<AutoDiscoverCollectorItem> adcis = dashboardGroupings.get(id);
            AutoDiscovery autoDiscovery = new AutoDiscovery();
            Dashboard dashboard = dashboardRepository.findOne(id);
            if (Objects.nonNull(dashboard)) {
                setAutoDiscoverMetaData(autoDiscovery, dashboard);
                adcis.forEach(adci -> {
                    Collector collector = collectorRepository.findOne(adci.getCollectorId());
                    CollectorType collectorType = collector.getCollectorType();
                    setEntries(autoDiscovery, adci, collectorType, featureFlag);
                });
                List<AutoDiscovery> ads = autoDiscoveryRepository.findByMetaDataTitle(autoDiscovery.getMetaData().getTitle());
                if (CollectionUtils.isNotEmpty(ads)) {
                    //noinspection OptionalGetWithoutIsPresent
                    AutoDiscovery ad = ads.stream().findFirst().get();
                    AutoDiscovery adConsolidate = new AutoDiscovery();
                    updateExistingEntriesInDashboard(autoDiscovery, ad, adConsolidate, featureFlag);
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
        LOGGER.info("Total count of dashboards found : " + CollectionUtils.size(autoDiscoveries));
        return autoDiscoveries;
    }


    @Override
    public List<AutoDiscovery> processAutoDiscoveryBatch(List<AutoDiscovery> autoDiscoveries) {
        List<AutoDiscovery> updatedRecords = new ArrayList<>();
        if (CollectionUtils.isEmpty(autoDiscoveries)) return updatedRecords;

        OAuth2AccessToken accessToken = restTemplate().getAccessToken();

        String token = BEARER + accessToken.getValue();

        autoDiscoveries.forEach(autoDiscovery -> {

            AutoDiscoverySubscriberRemoteRequest autoDiscoverySubscriberRemoteRequest = new AutoDiscoverySubscriberRemoteRequest();
            autoDiscoverySubscriberRemoteRequest.setSystemsToRequest(Collections.singletonList(relatedItemSettings.getSubscriberName()));
            autoDiscoverySubscriberRemoteRequest.setAutoDiscoveryRequest(createRequest(autoDiscovery));
            String json = new Gson().toJson(autoDiscoverySubscriberRemoteRequest);
            ResponseEntity<String> response = makeRestCall(json, token);
            if (Objects.nonNull(response)) {
                String responseCode = response.getStatusCode().toString();
                if (StringUtils.equalsIgnoreCase(responseCode, HTTP_CODE_200) && CollectionUtils.isNotEmpty(autoDiscovery.getAllEntries())) {
                    List<AutoDiscoveredEntry> entries = autoDiscovery.getAllEntries();
                    Iterables.removeIf(entries, Objects::isNull);
                    entries.forEach(entry -> {
                        if (entry.getStatus().equals(AutoDiscoveryStatusType.NEW)) {
                            entry.setStatus(AutoDiscoveryStatusType.AWAITING_USER_RESPONSE);
                        }
                    });
                    autoDiscoveryRepository.save(autoDiscovery);
                    updatedRecords.add(autoDiscovery);
                }
            }
        });

        return updatedRecords;
    }

    private ResponseEntity<String> makeRestCall(String payload, String token) {
        ResponseEntity<String> response = null;
        try {
            response = restClient.makeRestCallPost(getSubscriberUrl(), createHeaders(token, relatedItemSettings.getAccept()), payload);
        } catch (RestClientException re) {
            LOGGER.error("Error with REST url: " + getSubscriberUrl(), re);
        }
        return response;
    }

    private AutoDiscoverCollectorItem getAutoDiscoveryCollectorItemForCollectorTypes(CollectorItem collectorItem, AutoDiscovery ad, FeatureFlag featureFlag) {
        Collector collector = (Objects.isNull(collectorItem.getCollector())) ? collectorRepository.findOne(collectorItem.getCollectorId()) : collectorItem.getCollector();

        AutoDiscoveredEntry adEntry;
        switch (collector.getCollectorType()) {
            case Build:
                adEntry = ad.getBuildEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case SCM:
                adEntry = ad.getCodeRepoEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case CodeQuality:
                adEntry = ad.getStaticCodeEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case LibraryPolicy:
                adEntry = ad.getLibraryScanEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case StaticSecurityScan:
                adEntry = ad.getSecurityScanEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case Deployment:
                adEntry = ad.getDeploymentEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case Artifact:
                adEntry = ad.getArtifactEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case AgileTool:
                adEntry = ad.getFeatureEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            case Test:
                adEntry = ad.getFunctionalTestEntries().stream().filter(entry -> matchOptions(entry.getOptions(), collectorItem.getOptions(), collector)).findAny().orElse(null);
                return HygieiaUtils.allowAutoDiscover(featureFlag, collector.getCollectorType()) ? getAutoDiscoverCollectorItem(collectorItem, collector, adEntry) : null;

            default:
                return null;
        }
    }

    private AutoDiscoverCollectorItem getAutoDiscoverCollectorItem(CollectorItem collectorItem, Collector collector, AutoDiscoveredEntry adEntry) {
        AutoDiscoverCollectorItem leftAdci = null;
        if (Objects.nonNull(adEntry)) {
            try {
                leftAdci = adEntry.toAutoDiscoverCollectorItem(collector);
                leftAdci.setId(collectorItem.getId());
            } catch (HygieiaException e) {
                final String ERROR_MSG = "Exception occured in DefaultRelatedItemsClient.getAutoDiscoverCollectorItem() - invalid fields for collector type found. : "
                        + ExceptionUtils.getMessage(e);
                LOGGER.error(ERROR_MSG);
            }
        }
        return leftAdci;
    }

    private AutoDiscoverCollectorItem toAutoDiscoverCollectorItem(CollectorItem collectorItem, FeatureFlag featureFlag) {
        CollectorType collectorType = (Objects.isNull(collectorItem.getCollector())) ? findCollectorType(collectorItem.getCollectorId()) : collectorItem.getCollector().getCollectorType();
        if (!HygieiaUtils.allowAutoDiscover(featureFlag, collectorType)) return null;

        AutoDiscoverCollectorItem autoDiscoverCollectorItem = new AutoDiscoverCollectorItem();
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

        if(CollectionUtils.isNotEmpty(a)) {
            c.addAll(a);
        }
        if(CollectionUtils.isNotEmpty(b)) {
            c.addAll(b);
        }

        if(CollectionUtils.isNotEmpty(c)) {
            Iterables.removeIf(c, Objects::isNull);
            return c.stream().distinct().collect(Collectors.toList());
        }

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

    private CollectorType findCollectorType(ObjectId collectorId) {
        Collector collector = collectorRepository.findOne(collectorId);
        return collector.getCollectorType();
    }

    private List<com.capitalone.dashboard.model.Component> getComponentsForCollectorItemId(CollectorItem collectorItem, FeatureFlag featureFlag) {

        List<com.capitalone.dashboard.model.Component> components = new ArrayList<>();

        if (Objects.isNull(collectorItem)) return components;

        CollectorType collectorType = (Objects.isNull(collectorItem.getCollector())) ? findCollectorType(collectorItem.getCollectorId()) : collectorItem.getCollector().getCollectorType();

        // we should not be looking at components if the featureflag is turned off for a collectorType
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, collectorType)) return components;

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
            default:
                return components;
        }
    }

    private void addAllDistinctCollectorItemsToMap(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping, Dashboard dashboard, List<AutoDiscoverCollectorItem> refs) {
        Iterables.removeIf(refs, Objects::isNull);
        Collection<AutoDiscoverCollectorItem> rs = refs.stream().collect(Collectors.toConcurrentMap(AutoDiscoverCollectorItem::getId, Function.identity(), (p, q) -> p)).values();
        List<AutoDiscoverCollectorItem> distinctItems = new ArrayList<>(rs);
        dashboardGrouping.put(dashboard.getId(), distinctItems);
    }

    private void convertToAutoDiscoverCollectorItem(CollectorItem collectorItem, List<AutoDiscoverCollectorItem> refs, FeatureFlag featureFlag) {
        CollectorType collectorType = (Objects.isNull(collectorItem.getCollector())) ? findCollectorType(collectorItem.getCollectorId()) : collectorItem.getCollector().getCollectorType();
        if(!HygieiaUtils.allowAutoDiscover(featureFlag, collectorType)) return;
        AutoDiscoverCollectorItem lf = toAutoDiscoverCollectorItem(collectorItem, featureFlag);
        if(Objects.nonNull(lf)) {
            refs.add(lf);
        }
    }

    private void initialCall(Map<ObjectId, List<AutoDiscoverCollectorItem>> dashboardGrouping, CollectorItem leftCollectorItem, CollectorItem rightCollectorItem, Dashboard dashboard, FeatureFlag featureFlag) {

        AutoDiscoverCollectorItem left = toAutoDiscoverCollectorItem(leftCollectorItem, featureFlag);
        AutoDiscoverCollectorItem right  = toAutoDiscoverCollectorItem(rightCollectorItem, featureFlag);

        if (dashboardGrouping.containsKey(dashboard.getId())) {
            List<AutoDiscoverCollectorItem> items = dashboardGrouping.get(dashboard.getId());
            List<AutoDiscoverCollectorItem> refs = new ArrayList<>();


            refs.addAll(Arrays.asList(left, right));
            refs.addAll(items);
            Iterables.removeIf(refs, Objects::isNull);
            if (CollectionUtils.isNotEmpty(refs)) {
                Collection<AutoDiscoverCollectorItem> rs = refs.stream().collect(Collectors.toConcurrentMap(AutoDiscoverCollectorItem::getId, Function.identity(), (p, q) -> p)).values();
                List<AutoDiscoverCollectorItem> distinctItems = new ArrayList<>(rs);
                dashboardGrouping.put(dashboard.getId(), distinctItems);
            }
        } else {
            dashboardGrouping.put(dashboard.getId(), Arrays.asList(left, right));
        }
    }

    private List<com.capitalone.dashboard.model.Component> getComponentsAndProcessExclusions(CollectorItem leftCollectorItem, CollectorItem rightCollectorItem, List<com.capitalone.dashboard.model.Component> components, FeatureFlag featureFlag) {
        List<String> exclusions = relatedItemSettings.getExclusions();
        if (CollectionUtils.isNotEmpty(exclusions)) {
            for (String exclusion : exclusions) {
                String[] ex = StringUtils.split(exclusion, "|");
                if (Objects.isNull(leftCollectorItem.getOptions().get(ex[0])) || !leftCollectorItem.getOptions().get(ex[0]).equals(ex[1])) {
                    components = getComponentsForCollectorItemId(leftCollectorItem, featureFlag);
                }
                if (Objects.isNull(rightCollectorItem.getOptions().get(ex[0])) || !rightCollectorItem.getOptions().get(ex[0]).equals(ex[1])) {
                    components.addAll(getComponentsForCollectorItemId(rightCollectorItem, featureFlag));
                }
            }
        } else {
            components = getComponentsForCollectorItemId(leftCollectorItem, featureFlag);
            components.addAll(getComponentsForCollectorItemId(rightCollectorItem, featureFlag));
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

    private void updateExistingEntriesInDashboard(AutoDiscovery autoDiscovery, AutoDiscovery ad, AutoDiscovery adConsolidate, FeatureFlag featureFlag) {
        adConsolidate.setId(ad.getId());
        adConsolidate.setBuildEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Build) ? consolidate(ad.getBuildEntries(), autoDiscovery.getBuildEntries()) : new ArrayList<>());
        adConsolidate.setCodeRepoEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.SCM) ? consolidate(ad.getCodeRepoEntries(), autoDiscovery.getCodeRepoEntries()) : new ArrayList<>());
        adConsolidate.setArtifactEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Artifact) ? consolidate(ad.getArtifactEntries(), autoDiscovery.getArtifactEntries()) : new ArrayList<>());
        adConsolidate.setStaticCodeEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.CodeQuality) ? consolidate(ad.getStaticCodeEntries(), autoDiscovery.getStaticCodeEntries()) : new ArrayList<>());
        adConsolidate.setLibraryScanEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.LibraryPolicy) ? consolidate(ad.getLibraryScanEntries(), autoDiscovery.getLibraryScanEntries()) : new ArrayList<>());
        adConsolidate.setSecurityScanEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.StaticSecurityScan) ? consolidate(ad.getSecurityScanEntries(), autoDiscovery.getSecurityScanEntries()) : new ArrayList<>());
        adConsolidate.setDeploymentEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Deployment) ? consolidate(ad.getDeploymentEntries(), autoDiscovery.getDeploymentEntries()) : new ArrayList<>());
        adConsolidate.setFunctionalTestEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.Test) ? consolidate(ad.getFunctionalTestEntries(), autoDiscovery.getFunctionalTestEntries()) : new ArrayList<>());
        adConsolidate.setFeatureEntries(HygieiaUtils.allowAutoDiscover(featureFlag, CollectorType.AgileTool) ? consolidate(ad.getFeatureEntries(), autoDiscovery.getFeatureEntries()) : new ArrayList<>());
        adConsolidate.setMetaData(ad.getMetaData());
    }

    private void setEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        RelatedItemsUtils.setBuildEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setCodeRepoEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setArtifactEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setLibraryPolicyEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setStaticCodeEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setStaticSecurityEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setDeployEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setFunctionalTestEntries(autoDiscovery, adci, collectorType, featureFlag);
        RelatedItemsUtils.setFeatureEntries(autoDiscovery, adci, collectorType, featureFlag);
    }

}
