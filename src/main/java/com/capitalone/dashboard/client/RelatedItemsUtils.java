package com.capitalone.dashboard.client;

import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.CollectorType;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RelatedItemsUtils {

    public static AutoDiscoveredEntry getEntry(AutoDiscoverCollectorItem adci) {
        return toEntry(adci);
    }

    public static AutoDiscoveredEntry toEntry(AutoDiscoverCollectorItem adci) {
        AutoDiscoveredEntry autoDiscoveredEntry = new AutoDiscoveredEntry();
        autoDiscoveredEntry.setDescription(adci.getDescription());
        autoDiscoveredEntry.setNiceName(adci.getNiceName());
        autoDiscoveredEntry.setOptions(adci.getOptions());
        autoDiscoveredEntry.setStatus(adci.getAutoDiscoverStatus());
        return autoDiscoveredEntry;
    }


    public static void setBuildEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.Build)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getBuildEntries())) {
                List<AutoDiscoveredEntry> builds = autoDiscovery.getBuildEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(builds);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setBuildEntries(ls);
            } else {
                autoDiscovery.setBuildEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }

    }

    public static void setCodeRepoEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.SCM)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getCodeRepoEntries())) {
                List<AutoDiscoveredEntry> repos = autoDiscovery.getCodeRepoEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(repos);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setCodeRepoEntries(ls);
            } else {
                autoDiscovery.setCodeRepoEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setStaticCodeEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.CodeQuality)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getStaticCodeEntries())) {
                List<AutoDiscoveredEntry> staticCodeEntries = autoDiscovery.getStaticCodeEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(staticCodeEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setStaticCodeEntries(ls);
            } else {
                autoDiscovery.setStaticCodeEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setLibraryPolicyEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.LibraryPolicy)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getLibraryScanEntries())) {
                List<AutoDiscoveredEntry> libraryScanEntries = autoDiscovery.getLibraryScanEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(libraryScanEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setLibraryScanEntries(ls);

            } else {
                autoDiscovery.setLibraryScanEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setStaticSecurityEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.StaticSecurityScan)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getStaticCodeEntries())) {
                List<AutoDiscoveredEntry> securityScanEntries = autoDiscovery.getSecurityScanEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(securityScanEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setSecurityScanEntries(ls);
            } else {
                autoDiscovery.setSecurityScanEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setArtifactEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.Artifact)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getArtifactEntries())) {
                List<AutoDiscoveredEntry> artifactEntries = autoDiscovery.getArtifactEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(artifactEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setArtifactEntries(ls);
            } else {
                autoDiscovery.setArtifactEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setDeployEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.Deployment)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getDeploymentEntries())) {
                List<AutoDiscoveredEntry> deploymentEntries = autoDiscovery.getDeploymentEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(deploymentEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setDeploymentEntries(ls);
            } else {
                autoDiscovery.setDeploymentEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setFunctionalTestEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.Test)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getFunctionalTestEntries())) {
                List<AutoDiscoveredEntry> functionalTestEntries = autoDiscovery.getFunctionalTestEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(functionalTestEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setFunctionalTestEntries(ls);
            } else {
                autoDiscovery.setFunctionalTestEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }

    public static void setFeatureEntries(com.capitalone.dashboard.model.AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType) {
        if (collectorType.equals(CollectorType.AgileTool)) {
            if (CollectionUtils.isNotEmpty(autoDiscovery.getFeatureEntries())) {
                List<AutoDiscoveredEntry> featureEntries = autoDiscovery.getFeatureEntries();
                List<AutoDiscoveredEntry> ls = new ArrayList<>();
                ls.addAll(featureEntries);
                ls.add(RelatedItemsUtils.getEntry(adci));
                autoDiscovery.setFeatureEntries(ls);
            } else {
                autoDiscovery.setFeatureEntries(Arrays.asList(RelatedItemsUtils.getEntry(adci)));
            }
        }
    }


}
