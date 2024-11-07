package org.example.helper;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class GCSHelper {

  public static void authenticateImplicitWithAdc(String project) {
    Storage storage = StorageOptions.newBuilder().setProjectId(project).build().getService();

    System.out.println("Buckets:");
    Page<Bucket> buckets = storage.list();
    for (Bucket bucket : buckets.iterateAll()) {
      System.out.println(bucket.toString());
    }
    System.out.println("Listed all storage buckets.");
  }

  public static String generateFilePath(String region, String fileName) {
    // Get current date in YYYY-MM-DD format
    String date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now());

    // Construct the file path
    return String.format("%s/%s/%s.csv", date, region, fileName);
  }

  public static void uploadToGCS(String projectId, String region, String bucketName, String data)
      throws IOException {
    String filePath = GCSHelper.generateFilePath(region, "InstanceHealth");
    Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
    BlobId blobId = BlobId.of(bucketName, filePath);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

    Blob blob = storage.get(blobId);

    if (blob != null && blob.exists()) {
      // Truncate the existing file
      blob.delete();
      System.out.println("Existing file truncated: gs://" + bucketName + "/" + filePath);
    }

    // Upload the new data
    try (ByteArrayInputStream contentStream = new ByteArrayInputStream(
        data.getBytes(StandardCharsets.UTF_8))) {
      storage.createFrom(blobInfo, contentStream);
      System.out.println("File uploaded to GCS: gs://" + bucketName + "/" + filePath);
    }
  }
}
