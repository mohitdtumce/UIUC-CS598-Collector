package org.example;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.cloud.container.v1.ClusterManagerSettings;
import com.google.container.v1.Cluster;
import com.google.container.v1.ListClustersResponse;

import java.io.IOException;

import com.google.api.gax.paging.Page;
import com.google.cloud.container.v1.ClusterManagerClient;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.container.v1.Cluster;

public class Main {
  public static void clusterHealth() {
    String namespace = "default"; // Specify the namespace, e.g., "default"

    try {
      // Initialize Kubernetes API client
      ApiClient client = Config.defaultClient();
      Configuration.setDefaultApiClient(client);

      // Use the CoreV1 API to list pods
      CoreV1Api api = new CoreV1Api();

      // List pods in the specified namespace
      V1PodList podList = api.listNamespacedPod(namespace).execute();

      for (V1Pod pod : podList.getItems()) {
        String podName = pod.getMetadata().getName();
        System.out.printf("Pod Name: %s%n", podName);

        // Retrieve CPU and memory requests and limits (if specified in pod spec)
        pod.getSpec().getContainers().forEach(container -> {
          System.out.printf("Container Name: %s%n", container.getName());

          if (container.getResources() != null && container.getResources().getRequests() != null) {
            System.out.printf("  CPU Request: %s%n", container.getResources().getRequests().get("cpu"));
            System.out.printf("  Memory Request: %s%n", container.getResources().getRequests().get("memory"));
          }

          if (container.getResources() != null && container.getResources().getLimits() != null) {
            System.out.printf("  CPU Limit: %s%n", container.getResources().getLimits().get("cpu"));
            System.out.printf("  Memory Limit: %s%n", container.getResources().getLimits().get("memory"));
          }
        });
        System.out.println("------");
      }

    } catch (IOException | ApiException e) {
      System.err.println("Error listing pods or retrieving metrics: " + e.getMessage());
      e.printStackTrace();
    }
  }
  // public static void authenticateImplicitWithAdc(String project) {
  //   Storage storage = StorageOptions.newBuilder().setProjectId(project).build().getService();
  //
  //   System.out.println("Buckets:");
  //   Page<Bucket> buckets = storage.list();
  //   for (Bucket bucket : buckets.iterateAll()) {
  //     System.out.println(bucket.toString());
  //   }
  //   System.out.println("Listed all storage buckets.");
  // }

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

  public static void main(String[] args) throws Exception {
    System.out.println("Hello world!");
    String projectId = "mohitshr-learning";
    String region = "us-east4";
    listGkeClustersInRegion(projectId, region);

    /*
       https://cloud.google.com/kubernetes-engine/docs/how-to/cluster-access-for-kubectl#install_plugin
       sudo apt-get install google-cloud-cli-gke-gcloud-auth-plugin
       gcloud container clusters get-credentials mohitshr-auto-cluster1 --region us-east4
    */
    clusterHealth();
  }
}