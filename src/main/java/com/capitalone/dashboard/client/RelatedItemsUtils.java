package com.capitalone.dashboard.client;

import com.capitalone.dashboard.model.AutoDiscovery;
import com.capitalone.dashboard.model.AutoDiscoverCollectorItem;
import com.capitalone.dashboard.model.AutoDiscoveredEntry;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.model.FeatureFlag;
import com.capitalone.dashboard.util.HygieiaUtils;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RelatedItemsUtils {

    public static AutoDiscoveredEntry getEntry(AutoDiscoverCollectorItem adci) {
        AutoDiscoveredEntry autoDiscoveredEntry = new AutoDiscoveredEntry();
        autoDiscoveredEntry.setDescription(adci.getDescription());
        autoDiscoveredEntry.setNiceName(adci.getNiceName());
        autoDiscoveredEntry.setOptions(adci.getOptions());
        autoDiscoveredEntry.setStatus(adci.getAutoDiscoverStatus());
        return autoDiscoveredEntry;
    }

    public static void setBuildEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.Build.equals(collectorType)) {
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

    public static void setCodeRepoEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.SCM.equals(collectorType)) {
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

    public static void setStaticCodeEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.CodeQuality.equals(collectorType)) {
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

    public static void setLibraryPolicyEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.LibraryPolicy.equals(collectorType)) {
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

    public static void setStaticSecurityEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.StaticSecurityScan.equals(collectorType) ) {
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

    public static void setArtifactEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.Artifact.equals(collectorType)) {
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

    public static void setDeployEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.Deployment.equals(collectorType)) {
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

    public static void setFunctionalTestEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.Test.equals(collectorType)) {
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

    public static void setFeatureEntries(AutoDiscovery autoDiscovery, AutoDiscoverCollectorItem adci, CollectorType collectorType, FeatureFlag featureFlag) {
        if (HygieiaUtils.allowAutoDiscover(featureFlag,collectorType) && CollectorType.AgileTool.equals(collectorType)) {
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
