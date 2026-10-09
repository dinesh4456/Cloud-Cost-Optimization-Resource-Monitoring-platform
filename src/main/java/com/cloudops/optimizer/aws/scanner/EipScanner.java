package com.cloudops.optimizer.aws.scanner;

import com.cloudops.optimizer.aws.client.AwsClientFactory;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Address;
import software.amazon.awssdk.services.ec2.model.DescribeAddressesResponse;
import software.amazon.awssdk.services.ec2.model.Tag;

@Component
@Order(3)
public class EipScanner implements ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(EipScanner.class);

    private final AwsClientFactory clientFactory;
    private final OptimizerProperties properties;

    public EipScanner(AwsClientFactory clientFactory, OptimizerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public List<ResourceSnapshot> scan(ScanJob job) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        String region = job.getRegion() != null ? job.getRegion() : properties.getAws().getRegion();

        try (Ec2Client ec2Client = clientFactory.ec2(region)) {
            DescribeAddressesResponse response = ec2Client.describeAddresses();
            for (Address address : response.addresses()) {
                String allocId = address.allocationId() != null ? address.allocationId() : address.publicIp();
                boolean isAssociated = address.associationId() != null || address.instanceId() != null;
                String state = isAssociated ? "associated" : "unassociated";

                String name = address.tags().stream()
                        .filter(t -> "Name".equalsIgnoreCase(t.key()))
                        .map(Tag::value)
                        .findFirst()
                        .orElse(address.publicIp());

                ResourceSnapshot snapshot = new ResourceSnapshot(
                        job,
                        ResourceType.EIP,
                        allocId,
                        name,
                        region,
                        state
                );

                Map<String, String> attrs = new LinkedHashMap<>();
                attrs.put("publicIp", address.publicIp());
                attrs.put("allocationId", address.allocationId());
                attrs.put("associationId", address.associationId());
                attrs.put("instanceId", address.instanceId());
                attrs.put("networkInterfaceId", address.networkInterfaceId());
                attrs.put("domain", address.domainAsString());
                attrs.put("associated", String.valueOf(isAssociated));

                snapshot.setAttributes(attrs);
                snapshots.add(snapshot);
            }
            log.info("EIP scanner discovered {} addresses via AWS SDK", snapshots.size());
        } catch (Exception ex) {
            log.warn("AWS EIP live scan unavailable: {}. Using simulated lab cloud inventory.", ex.getMessage());
            snapshots = generateSimulatedEipSnapshots(job, region);
        }

        return snapshots;
    }

    private List<ResourceSnapshot> generateSimulatedEipSnapshots(ScanJob job, String region) {
        List<ResourceSnapshot> simulated = new ArrayList<>();

        // 1. Unused EIP #1 (triggers UNUSED_EIP rule)
        ResourceSnapshot unused1 = new ResourceSnapshot(
                job, ResourceType.EIP, "eipalloc-019928374a5b6c7d8", "legacy-loadbalancer-ip", region, "unassociated");
        Map<String, String> attrs1 = new LinkedHashMap<>();
        attrs1.put("publicIp", "15.207.44.19");
        attrs1.put("allocationId", "eipalloc-019928374a5b6c7d8");
        attrs1.put("associationId", "");
        attrs1.put("instanceId", "");
        attrs1.put("networkInterfaceId", "");
        attrs1.put("domain", "vpc");
        attrs1.put("associated", "false");
        unused1.setAttributes(attrs1);
        simulated.add(unused1);

        // 2. Unused EIP #2 (triggers UNUSED_EIP rule)
        ResourceSnapshot unused2 = new ResourceSnapshot(
                job, ResourceType.EIP, "eipalloc-0f7e6d5c4b3a29180", "qa-ingress-static-ip", region, "unassociated");
        Map<String, String> attrs2 = new LinkedHashMap<>();
        attrs2.put("publicIp", "13.127.88.92");
        attrs2.put("allocationId", "eipalloc-0f7e6d5c4b3a29180");
        attrs2.put("associationId", "");
        attrs2.put("instanceId", "");
        attrs2.put("networkInterfaceId", "");
        attrs2.put("domain", "vpc");
        attrs2.put("associated", "false");
        unused2.setAttributes(attrs2);
        simulated.add(unused2);

        // 3. Associated active EIP
        ResourceSnapshot associated = new ResourceSnapshot(
                job, ResourceType.EIP, "eipalloc-044332211aabbccdd", "prod-nat-gateway-ip", region, "associated");
        Map<String, String> attrs3 = new LinkedHashMap<>();
        attrs3.put("publicIp", "3.108.92.14");
        attrs3.put("allocationId", "eipalloc-044332211aabbccdd");
        attrs3.put("associationId", "eipassoc-0988776655");
        attrs3.put("instanceId", "i-041b6c77d98e01a22");
        attrs3.put("networkInterfaceId", "eni-0123456789abcdef0");
        attrs3.put("domain", "vpc");
        attrs3.put("associated", "true");
        associated.setAttributes(attrs3);
        simulated.add(associated);

        return simulated;
    }
}
