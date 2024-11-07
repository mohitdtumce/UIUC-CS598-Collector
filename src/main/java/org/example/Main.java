package org.example;

import java.util.List;
import java.util.StringJoiner;
import org.example.helper.GCEHelper;
import org.example.helper.GCSHelper;

public class Main {

  public static void main(String[] args) throws Exception {
    String projectId = "mohitshr-learning";
    String region = "us-central1";
    String bucketName = "mohitshr-project-gslb-us-east4";

    StringJoiner joiner = new StringJoiner("\n");
    List<String> content = GCEHelper.fetchMetrics(projectId, region);
    for (String c : content) {
      joiner.add(c);
    }

    GCSHelper.uploadToGCS(projectId, region, bucketName, joiner.toString());
  }
}