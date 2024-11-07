package org.example.helper;

import com.google.api.MetricDescriptor;
import com.google.cloud.compute.v1.Instance;
import com.google.cloud.compute.v1.InstancesClient;
import com.google.cloud.compute.v1.InstancesClient.ListPagedResponse;
import com.google.cloud.compute.v1.ListInstancesRequest;
import com.google.cloud.compute.v1.Region;
import com.google.cloud.compute.v1.RegionsClient;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import com.google.monitoring.v3.Aggregation;
import com.google.monitoring.v3.ListTimeSeriesRequest;
import com.google.monitoring.v3.ProjectName;
import com.google.monitoring.v3.TimeInterval;
import com.google.monitoring.v3.TimeSeries;
import com.google.protobuf.Duration;
import com.google.protobuf.util.Timestamps;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GCEHelper {

  private static final List<String> metrics = List.of(
      // CPU
      "compute.googleapis.com/instance/cpu/utilization",

      // RAM
      "compute.googleapis.com/instance/memory/balloon/ram_used",
      "compute.googleapis.com/instance/memory/balloon/ram_size",

      // Network
      "compute.googleapis.com/instance/network/received_bytes_count",
      "compute.googleapis.com/instance/network/sent_bytes_count",

      // Disk
      "compute.googleapis.com/instance/disk/average_io_latency",
      "compute.googleapis.com/instance/disk/read_bytes_count",
      "compute.googleapis.com/instance/disk/write_bytes_count"
  );

  static List<MetricPair> initializeMetricPairs(String projectId, List<String> metricNames,
      MetricServiceClient metricServiceClient) throws IOException {
    List<MetricPair> metricPairs = new ArrayList<>();
    for (String metricName : metricNames) {
      // Ensure metricName has the full path with projectId
      String fullMetricName = String.format("projects/%s/metricDescriptors/%s", projectId,
          metricName);
      MetricDescriptor descriptor = metricServiceClient.getMetricDescriptor(fullMetricName);
      Aggregation aggregation = getAggregationForMetric(descriptor);
      metricPairs.add(new MetricPair(metricName, aggregation));
    }
    return metricPairs;
  }

  public static List<String> listZonesInRegion(String projectId, String region) {
    List<String> zones = new ArrayList<>();
    try (RegionsClient regionsClient = RegionsClient.create()) {
      // Get the region details
      Region regionDetails = regionsClient.get(projectId, region);

      // List and print zones available in the region
      // System.out.println("Zones in region " + region + ":");
      for (String zone : regionDetails.getZonesList()) {
        String zoneName = zone.substring(zone.lastIndexOf('/') + 1);
        // System.out.println(zoneName);
        zones.add(zoneName);
      }
    } catch (IOException e) {
      System.out.println("Error occurred while listing zones: " + e.getMessage());
    }
    return zones;
  }

  public static String removePrefix(String str, String prefix) {
    if (str.startsWith(prefix)) {
      return str.substring(prefix.length());
    }
    return str;
  }

  public static List<String> fetchMetrics(String projectId, String region) throws IOException {
    List<String> zones = listZonesInRegion(projectId, region);
    List<String> joiner = new ArrayList<>();
    for (String zone : zones) {
      try (MetricServiceClient metricServiceClient = MetricServiceClient.create();
          InstancesClient instancesClient = InstancesClient.create()) {

        List<MetricPair> metricPairs = initializeMetricPairs(projectId, metrics,
            metricServiceClient);

        ListInstancesRequest request = ListInstancesRequest.newBuilder()
            .setProject(projectId).setZone(zone).setFilter("scheduling.preemptible = true").build();

        ListPagedResponse response = instancesClient.list(request);
        StringBuilder output = new StringBuilder();

        for (Instance instance : response.iterateAll()) {
          StringBuilder instanceData = new StringBuilder("instance:").append(instance.getName())
              .append(",zone:").append(zone)
              .append(",status:").append(instance.getStatus());
          for (MetricPair metricPair : metricPairs) {
            instanceData.append(",")
                .append(removePrefix(metricPair.metricName, "compute.googleapis.com/instance/"))
                .append(":").append(
                    fetchMetrics(metricPair, projectId, zone, instance.getId(), metricServiceClient));
          }
          output.append(instanceData);
        }
        if (!output.isEmpty()) {
          joiner.add(output.toString());
        }
      }
    }
    return joiner;
  }

  private static Aggregation getAggregationForMetric(MetricDescriptor descriptor) {
    switch (descriptor.getMetricKind()) {
      case GAUGE:
        return Aggregation.newBuilder()
            .setAlignmentPeriod(Duration.newBuilder().setSeconds(60))
            .setPerSeriesAligner(Aggregation.Aligner.ALIGN_MEAN).build();
      case CUMULATIVE:
        return Aggregation.newBuilder()
            .setAlignmentPeriod(Duration.newBuilder().setSeconds(60))
            .setPerSeriesAligner(Aggregation.Aligner.ALIGN_SUM).build();
      default:
        return Aggregation.newBuilder().build();
    }
  }

  private static String fetchMetrics(MetricPair metricPair, String projectId, String zone,
      long instanceId, MetricServiceClient client) throws IOException {
    ProjectName projectName = ProjectName.of(projectId);
    TimeInterval interval = TimeInterval.newBuilder()
        .setEndTime(Timestamps.fromMillis(System.currentTimeMillis()))
        .setStartTime(Timestamps.fromMillis(System.currentTimeMillis() - 300_000))
        .build();

    ListTimeSeriesRequest request = ListTimeSeriesRequest.newBuilder()
        .setName(projectName.toString())
        .setFilter(String.format(
            "metric.type=\"%s\" AND resource.labels.instance_id=\"%s\" AND resource.labels.zone=\"%s\"",
            metricPair.metricName, instanceId, zone))
        .setInterval(interval)
        .setAggregation(metricPair.aggregation)
        .build();

    double maxVal = 0.0;
    for (TimeSeries timeSeries : client.listTimeSeries(request).iterateAll()) {
      for (var point : timeSeries.getPointsList()) {
        maxVal = Math.max(maxVal, point.getValue().getDoubleValue());
      }
    }
    return String.valueOf(maxVal);
  }

  record MetricPair(String metricName, Aggregation aggregation) {

  }
}