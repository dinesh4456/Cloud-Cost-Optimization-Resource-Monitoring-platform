package com.cloudops.optimizer.aws.scanner;

import com.cloudops.optimizer.aws.client.AwsClientFactory;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
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
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

@Component
@Order(2)
public class EbsScanner implements ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(EbsScanner.class);

    private final AwsClientFactory clientFactory;
    private final OptimizerProperties properties;

    public EbsScanner(AwsClientFactory clientFactory, OptimizerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public List<ResourceSnapshot> scan(ScanJob job) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        String region = job.getRegion() != null ? job.getRegion() : properties.getAws().getRegion();

        try (Ec2Client ec2Client = clientFactory.ec2(region)) {
            DescribeVolumesResponse response = ec2Client.describeVolumes();
            for (Volume volume : response.volumes()) {
                String volumeId = volume.volumeId();
                String state = volume.stateAsString() != null ? volume.stateAsString() : "unknown";
                String name = volume.tags().stream()
                        .filter(t -> "Name".equalsIgnoreCase(t.key()))
                        .map(Tag::value)
                        .findFirst()
                        .orElse(volumeId);

                ResourceSnapshot snapshot = new ResourceSnapshot(
                        job,
                        ResourceType.EBS,
                        volumeId,
                        name,
                        region,
                        state
                );

                Map<String, String> attrs = new LinkedHashMap<>();
                attrs.put("sizeGb", String.valueOf(volume.size()));
                attrs.put("volumeType", volume.volumeTypeAsString());
                attrs.put("iops", String.valueOf(volume.iops()));
                attrs.put("encrypted", String.valueOf(volume.encrypted()));
                attrs.put("availabilityZone", volume.availabilityZone());
                attrs.put("createTime", volume.createTime() != null ? volume.createTime().toString() : "");

                boolean isAttached = volume.attachments() != null && !volume.attachments().isEmpty();
                attrs.put("attached", String.valueOf(isAttached));
                if (isAttached) {
                    attrs.put("attachedInstanceId", volume.attachments().get(0).instanceId());
                    attrs.put("device", volume.attachments().get(0).device());
                } else {
                    attrs.put("attachedInstanceId", "");
                }

                snapshot.setAttributes(attrs);
                snapshots.add(snapshot);
            }
            log.info("EBS scanner discovered {} volumes via AWS SDK", snapshots.size());
        } catch (Exception ex) {
            log.warn("AWS EBS live scan unavailable: {}. Using simulated lab cloud inventory.", ex.getMessage());
            snapshots = generateSimulatedEbsSnapshots(job, region);
        }

        return snapshots;
    }

    private List<ResourceSnapshot> generateSimulatedEbsSnapshots(ScanJob job, String region) {
        List<ResourceSnapshot> simulated = new ArrayList<>();

        // 1. Unattached orphan volume (triggers UNATTACHED_EBS rule)
        ResourceSnapshot orphan1 = new ResourceSnapshot(
                job, ResourceType.EBS, "vol-0abc1234def56789a", "backup-snapshot-old-db", region, "available");
        Map<String, String> attrs1 = new LinkedHashMap<>();
        attrs1.put("sizeGb", "250");
        attrs1.put("volumeType", "gp3");
        attrs1.put("iops", "3000");
        attrs1.put("encrypted", "true");
        attrs1.put("availabilityZone", region + "a");
        attrs1.put("attached", "false");
        attrs1.put("attachedInstanceId", "");
        attrs1.put("createTime", Instant.now().minus(60, ChronoUnit.DAYS).toString());
        orphan1.setAttributes(attrs1);
        simulated.add(orphan1);

        // 2. Another unattached test volume (triggers UNATTACHED_EBS rule)
        ResourceSnapshot orphan2 = new ResourceSnapshot(
                job, ResourceType.EBS, "vol-088f192aa00b11223", "ml-model-temp-storage", region, "available");
        Map<String, String> attrs2 = new LinkedHashMap<>();
        attrs2.put("sizeGb", "500");
        attrs2.put("volumeType", "gp2");
        attrs2.put("iops", "1500");
        attrs2.put("encrypted", "false");
        attrs2.put("availabilityZone", region + "b");
        attrs2.put("attached", "false");
        attrs2.put("attachedInstanceId", "");
        attrs2.put("createTime", Instant.now().minus(40, ChronoUnit.DAYS).toString());
        orphan2.setAttributes(attrs2);
        simulated.add(orphan2);

        // 3. Attached root volume (in-use)
        ResourceSnapshot inUse1 = new ResourceSnapshot(
                job, ResourceType.EBS, "vol-01122334455667788", "prod-web-root-vol", region, "in-use");
        Map<String, String> attrs3 = new LinkedHashMap<>();
        attrs3.put("sizeGb", "80");
        attrs3.put("volumeType", "gp3");
        attrs3.put("iops", "3000");
        attrs3.put("encrypted", "true");
        attrs3.put("availabilityZone", region + "a");
        attrs3.put("attached", "true");
        attrs3.put("attachedInstanceId", "i-041b6c77d98e01a22");
        attrs3.put("device", "/dev/xvda");
        attrs3.put("createTime", Instant.now().minus(90, ChronoUnit.DAYS).toString());
        inUse1.setAttributes(attrs3);
        simulated.add(inUse1);

        return simulated;
    }
}
