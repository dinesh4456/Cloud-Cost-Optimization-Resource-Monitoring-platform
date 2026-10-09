package com.cloudops.optimizer.aws.scanner;

import com.cloudops.optimizer.aws.client.AwsClientFactory;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Component
@Order(4)
public class S3Scanner implements ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(S3Scanner.class);

    private final AwsClientFactory clientFactory;
    private final OptimizerProperties properties;

    public S3Scanner(AwsClientFactory clientFactory, OptimizerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public List<ResourceSnapshot> scan(ScanJob job) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        String region = job.getRegion() != null ? job.getRegion() : properties.getAws().getRegion();

        try (S3Client s3Client = clientFactory.s3(region)) {
            ListBucketsResponse response = s3Client.listBuckets();
            int sampleLimit = properties.getScan().getS3ObjectSampleLimit();

            for (Bucket bucket : response.buckets()) {
                String bucketName = bucket.name();
                ResourceSnapshot snapshot = new ResourceSnapshot(
                        job,
                        ResourceType.S3,
                        bucketName,
                        bucketName,
                        region,
                        "ACTIVE"
                );

                long totalBytes = 0;
                long objectCount = 0;
                Instant oldestObjectDate = Instant.now();
                Instant newestObjectDate = Instant.EPOCH;
                String dominantStorageClass = "STANDARD";

                try {
                    ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .maxKeys(sampleLimit)
                            .build();
                    ListObjectsV2Response listResp = s3Client.listObjectsV2(listReq);
                    if (listResp.contents() != null) {
                        for (S3Object obj : listResp.contents()) {
                            totalBytes += obj.size();
                            objectCount++;
                            if (obj.lastModified() != null) {
                                if (obj.lastModified().isBefore(oldestObjectDate)) {
                                    oldestObjectDate = obj.lastModified();
                                }
                                if (obj.lastModified().isAfter(newestObjectDate)) {
                                    newestObjectDate = obj.lastModified();
                                }
                            }
                            if (obj.storageClassAsString() != null) {
                                dominantStorageClass = obj.storageClassAsString();
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Bucket {} objects listing restricted: {}", bucketName, ex.getMessage());
                }

                double sizeGb = (double) totalBytes / (1024.0 * 1024.0 * 1024.0);
                long oldestAgeDays = oldestObjectDate.isBefore(Instant.now())
                        ? ChronoUnit.DAYS.between(oldestObjectDate, Instant.now())
                        : 0;

                Map<String, String> attrs = new LinkedHashMap<>();
                attrs.put("creationDate", bucket.creationDate() != null ? bucket.creationDate().toString() : "");
                attrs.put("estimatedSizeGb", String.format("%.2f", sizeGb));
                attrs.put("sampledObjectCount", String.valueOf(objectCount));
                attrs.put("oldestObjectAgeDays", String.valueOf(oldestAgeDays));
                attrs.put("storageClass", dominantStorageClass);

                snapshot.setAttributes(attrs);
                snapshots.add(snapshot);
            }
            log.info("S3 scanner discovered {} buckets via AWS SDK", snapshots.size());
        } catch (Exception ex) {
            log.warn("AWS S3 live scan unavailable: {}. Using simulated lab cloud inventory.", ex.getMessage());
            snapshots = generateSimulatedS3Snapshots(job, region);
        }

        return snapshots;
    }

    private List<ResourceSnapshot> generateSimulatedS3Snapshots(ScanJob job, String region) {
        List<ResourceSnapshot> simulated = new ArrayList<>();

        // 1. Large stale standard storage bucket (triggers STALE_S3_DATA rule)
        ResourceSnapshot staleBucket = new ResourceSnapshot(
                job, ResourceType.S3, "cloudops-raw-access-logs-archive", "cloudops-raw-access-logs-archive", region, "ACTIVE");
        Map<String, String> attrs1 = new LinkedHashMap<>();
        attrs1.put("creationDate", Instant.now().minus(365, ChronoUnit.DAYS).toString());
        attrs1.put("estimatedSizeGb", "650.00");
        attrs1.put("sampledObjectCount", "12500");
        attrs1.put("oldestObjectAgeDays", "180");
        attrs1.put("storageClass", "STANDARD");
        staleBucket.setAttributes(attrs1);
        simulated.add(staleBucket);

        // 2. Active application assets bucket (healthy)
        ResourceSnapshot activeBucket = new ResourceSnapshot(
                job, ResourceType.S3, "cloudops-frontend-static-assets", "cloudops-frontend-static-assets", region, "ACTIVE");
        Map<String, String> attrs2 = new LinkedHashMap<>();
        attrs2.put("creationDate", Instant.now().minus(90, ChronoUnit.DAYS).toString());
        attrs2.put("estimatedSizeGb", "8.50");
        attrs2.put("sampledObjectCount", "450");
        attrs2.put("oldestObjectAgeDays", "15");
        attrs2.put("storageClass", "STANDARD");
        activeBucket.setAttributes(attrs2);
        simulated.add(activeBucket);

        return simulated;
    }
}
