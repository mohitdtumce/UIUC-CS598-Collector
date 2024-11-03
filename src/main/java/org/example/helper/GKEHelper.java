package org.example.helper;

import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.container.v1.Cluster;
import com.google.container.v1.ListClustersResponse;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import java.io.IOException;

public class GKEHelper {

  public static String fetchPodMetrics() {
    try {
      // Specify the namespace, e.g., "default"
      String namespace = "default";

      // Initialize Kubernetes API client
      ApiClient client = Config.defaultClient();
      Configuration.setDefaultApiClient(client);

      // Use the CoreV1 API to list pods
      CoreV1Api api = new CoreV1Api();

      // List pods in the specified namespace
      V1PodList podList = api.listNamespacedPod(namespace).execute();

      StringBuilder builder = new StringBuilder();
      for (V1Pod pod : podList.getItems()) {
        String podName = pod.getMetadata().getName();
        builder.append(podName).append(",");
        // Retrieve CPU and memory requests and limits (if specified in pod spec)
        pod.getSpec().getContainers().forEach(container -> {
          builder.append(container.getName()).append(",");
          if (container.getResources() != null) {
            if (container.getResources().getRequests() != null) {
              builder.append(container.getResources().getRequests().get("cpu").getNumber())
                  .append(",");
              builder.append(container.getResources().getRequests().get("memory").getNumber())
                  .append(",");
              builder.append(
                      container.getResources().getRequests().get("ephemeral-storage").getNumber())
                  .append("\n");
            }
          }
        });
      }
      return builder.toString();
    } catch (IOException | ApiException e) {
      System.err.println("Error listing pods or retrieving metrics: " + e.getMessage());
      e.printStackTrace();
    }
    return "";
  }

  public static String getGkeClusterInRegion(String projectId, String region, String clusterId) {
    String location = String.format("projects/%s/locations/%s/clusters/%s", projectId, region,
        clusterId);

    StringBuilder builder = new StringBuilder();
    try (ClusterManagerClient clusterManagerClient = ClusterManagerClient.create()) {
      Cluster cluster = clusterManagerClient.getCluster(location);
      builder.append("ClusterName:").append(cluster.getName()).append(",")
          .append("ClusterStatus:").append(cluster.getStatus()).append(",")
          .append("ClusterLocation:").append(cluster.getLocation()).append(",")
          .append("ClusterEndpoint:").append(cluster.getEndpoint()).append("\n");
    } catch (Exception e) {
      System.err.println("Error listing GKE clusters: " + e.getMessage());
      e.printStackTrace();
    }
    return builder.toString();
  }

  public static void listGkeClustersInRegion(String projectId, String region) {
    String location = String.format("projects/%s/locations/%s", projectId, region);

    try (ClusterManagerClient clusterManagerClient = ClusterManagerClient.create()) {

      ListClustersResponse response = clusterManagerClient.listClusters(location);

      for (Cluster cluster : response.getClustersList()) {
        System.out.printf("Cluster Name: %s%n", cluster.getName());
        System.out.printf("Cluster Status: %s%n", cluster.getStatus());
        System.out.printf("Cluster Location: %s%n", cluster.getLocation());
        System.out.printf("Cluster Endpoint: %s%n", cluster.getEndpoint());
        System.out.println("------");
      }
    } catch (Exception e) {
      System.err.println("Error listing GKE clusters: " + e.getMessage());
      e.printStackTrace();
    }
  }


  public static String fetchNodeMetrics() throws IOException {
    // Initialize Kubernetes API client
    ApiClient client = Config.defaultClient();
    Configuration.setDefaultApiClient(client);

    // Use the CoreV1 API to list pods
    CoreV1Api api = new CoreV1Api();

    StringBuilder builder = new StringBuilder();
    try {
      V1NodeList nodeList = api.listNode().execute();
      for (V1Node node : nodeList.getItems()) {
        String nodeName = node.getMetadata().getName();
        builder.append(nodeName).append(",");

        if (node.getStatus() != null && node.getStatus().getCapacity() != null) {
          builder.append(node.getStatus().getCapacity().get("cpu").getNumber()).append(",")
              .append(node.getStatus().getCapacity().get("memory").getNumber()).append(",")
              .append(node.getStatus().getCapacity().get("ephemeral-storage").getNumber())
              .append(",");
        }

      }
    } catch (ApiException e) {
      System.err.println("Error fetching node metrics: " + e.getMessage());
      e.printStackTrace();
    }
    return builder.toString();
  }

  private static String getPodStorageUsage(CoreV1Api api, String podName, String namespace) {
    try {
      // List PVCs in the specified namespace
      V1PersistentVolumeClaimList pvcList = api.listNamespacedPersistentVolumeClaim(namespace)
          .execute();

      for (V1PersistentVolumeClaim pvc : pvcList.getItems()) {
        // Check if PVC is bound to the pod
        if (pvc.getMetadata().getName().equals(podName)) {
          return pvc.getStatus().getPhase();
        }
      }

    } catch (ApiException e) {
      System.err.println("Error fetching storage usage for pod: " + e.getMessage());
    }
    return ",";
  }
}
