package org.example;

import org.example.helper.GKEHelper;
import org.example.helper.StorageHelper;

public class Main {
  public static void main(String[] args) throws Exception {
    String projectId = "mohitshr-learning";
    String region = "us-east4";
    String clusterId = "mohitshr-auto-cluster1";
    String bucketName = "mohitshr-project-gslb-us-east4";

    /*
     * gcloud auth application-default login --no-launch-browser
     */
    System.out.println(GKEHelper.getGkeClusterInRegion(projectId, region, clusterId));

    /*
     * https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl#install_plugin
     * sudo apt-get install google-cloud-cli-gke-gcloud-auth-plugin
     * gcloud container clusters get-credentials {clusterId} --region {region}
     */

    // Generate the file path
    String podData = GKEHelper.fetchPodMetrics();
    String podFilePath = StorageHelper.generateFilePath(region, clusterId, "PODHealth");
    StorageHelper.uploadToGCS(projectId, bucketName, podFilePath, podData);

    String nodeData = GKEHelper.fetchNodeMetrics();
    String nodeFileName = StorageHelper.generateFilePath(region, clusterId, "NodeHealth");
    StorageHelper.uploadToGCS(projectId, bucketName, nodeFileName, nodeData);

    // MonitoringHelper.fetchIngressBytesCount(projectId, clusterId);
    // MonitoringHelper.fetchEgressBytesCount(projectId, clusterId);
    // MonitoringHelper.fetchGkeMetrics(projectId, clusterId);
  }
}