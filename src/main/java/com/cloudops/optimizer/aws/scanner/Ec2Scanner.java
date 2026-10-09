package com.cloudops.optimizer.aws.scanner;

import com.cloudops.optimizer.aws.client.AwsClientFactory;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.snapshot.ResourceMetric;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

@Component
@Order(1)
public class Ec2Scanner implements ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(Ec2Scanner.class);

    private final AwsClientFactory clientFactory;
    private final CloudWatchMetricsCollector metricsCollector;
    private final OptimizerProperties properties;

    public Ec2Scanner(
            AwsClientFactory clientFactory,
            CloudWatchMetricsCollector metricsCollector,
            OptimizerProperties properties) {
        this.clientFactory = clientFactory;
        this.metricsCollector = metricsCollector;
        this.properties = properties;
    }

    @Override
    public List<ResourceSnapshot> scan(ScanJob job) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        String region = job.getRegion() != null ? job.getRegion() : properties.getAws().getRegion();

        try (Ec2Client ec2Client = clientFactory.ec2(region);
             CloudWatchClient cwClient = clientFactory.cloudWatch(region)) {

            DescribeInstancesResponse response = ec2Client.describeInstances();
            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String instanceId = instance.instanceId();
                    String stateName = instance.state() != null ? instance.state().nameAsString() : "unknown";
                    String name = instance.tags().stream()
                            .filter(t -> "Name".equalsIgnoreCase(t.key()))
                            .map(Tag::value)
                            .findFirst()
                            .orElse(instanceId);

                    ResourceSnapshot snapshot = new ResourceSnapshot(
                            job,
                            ResourceType.EC2,
                            instanceId,
                            name,
                            region,
                            stateName
                    );

                    Map<String, String> attrs = new LinkedHashMap<>();
                    attrs.put("instanceType", instance.instanceTypeAsString());
                    attrs.put("architecture", instance.architectureAsString());
                    attrs.put("availabilityZone", instance.placement() != null ? instance.placement().availabilityZone() : "");
                    attrs.put("privateIp", instance.privateIpAddress());
                    attrs.put("publicIp", instance.publicIpAddress());
                    attrs.put("launchTime", instance.launchTime() != null ? instance.launchTime().toString() : "");

                    if ("running".equalsIgnoreCase(stateName)) {
                        Optional<BigDecimal> cpuAvg = metricsCollector.average(
                                cwClient,
                                "AWS/EC2",
                                "CPUUtilization",
                                "InstanceId",
                                instanceId,
                                properties.getRules().getIdleLookbackDays()
                        );
                        cpuAvg.ifPresent(avg -> {
                            snapshot.setCpuAvg7d(avg);
                            attrs.put("cpuAvg7d", avg.toPlainString() + "%");
                            ResourceMetric metric = new ResourceMetric(
                                    "CPUUtilization_7d_avg",
                                    avg,
                                    "Percent",
                                    Instant.now().minus(properties.getRules().getIdleLookbackDays(), ChronoUnit.DAYS),
                                    Instant.now()
                            );
                            snapshot.addMetric(metric);
                        });
                    }

                    snapshot.setAttributes(attrs);
                    snapshots.add(snapshot);
                }
            }
            log.info("EC2 scanner discovered {} instances via AWS SDK", snapshots.size());
        } catch (Exception ex) {
            log.warn("AWS EC2 live scan unavailable: {}. Using simulated lab cloud inventory.", ex.getMessage());
            snapshots = generateSimulatedEc2Snapshots(job, region);
        }

        return snapshots;
    }

    private List<ResourceSnapshot> generateSimulatedEc2Snapshots(ScanJob job, String region) {
        List<ResourceSnapshot> simulated = new ArrayList<>();

        // 1. Idle EC2 instance (triggers IDLE_EC2 rule)
        ResourceSnapshot idle = new ResourceSnapshot(
                job, ResourceType.EC2, "i-098a72b11cd4e5f01", "dev-api-staging-backend", region, "running");
        idle.setCpuAvg7d(new BigDecimal("3.40"));
        Map<String, String> idleAttrs = new LinkedHashMap<>();
        idleAttrs.put("instanceType", "m5.large");
        idleAttrs.put("architecture", "x86_64");
        idleAttrs.put("availabilityZone", region + "a");
        idleAttrs.put("privateIp", "10.0.12.45");
        idleAttrs.put("publicIp", "13.235.10.88");
        idleAttrs.put("launchTime", Instant.now().minus(45, ChronoUnit.DAYS).toString());
        idleAttrs.put("cpuAvg7d", "3.40%");
        idle.setAttributes(idleAttrs);
        idle.addMetric(new ResourceMetric("CPUUtilization_7d_avg", new BigDecimal("3.40"), "Percent",
                Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()));
        simulated.add(idle);

        // 2. Another low-utilization compute (triggers IDLE_EC2 rule)
        ResourceSnapshot idleWorker = new ResourceSnapshot(
                job, ResourceType.EC2, "i-03f48a92b21c17e33", "analytics-batch-worker-02", region, "running");
        idleWorker.setCpuAvg7d(new BigDecimal("5.80"));
        Map<String, String> idleWorkerAttrs = new LinkedHashMap<>();
        idleWorkerAttrs.put("instanceType", "c5.xlarge");
        idleWorkerAttrs.put("architecture", "x86_64");
        idleWorkerAttrs.put("availabilityZone", region + "b");
        idleWorkerAttrs.put("privateIp", "10.0.24.110");
        idleWorkerAttrs.put("publicIp", "");
        idleWorkerAttrs.put("launchTime", Instant.now().minus(20, ChronoUnit.DAYS).toString());
        idleWorkerAttrs.put("cpuAvg7d", "5.80%");
        idleWorker.setAttributes(idleWorkerAttrs);
        idleWorker.addMetric(new ResourceMetric("CPUUtilization_7d_avg", new BigDecimal("5.80"), "Percent",
                Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()));
        simulated.add(idleWorker);

        // 3. Healthy active EC2 instance
        ResourceSnapshot active = new ResourceSnapshot(
                job, ResourceType.EC2, "i-041b6c77d98e01a22", "prod-web-frontend-01", region, "running");
        active.setCpuAvg7d(new BigDecimal("58.20"));
        Map<String, String> activeAttrs = new LinkedHashMap<>();
        activeAttrs.put("instanceType", "t3.medium");
        activeAttrs.put("architecture", "x86_64");
        activeAttrs.put("availabilityZone", region + "a");
        activeAttrs.put("privateIp", "10.0.1.15");
        activeAttrs.put("publicIp", "3.108.92.14");
        activeAttrs.put("launchTime", Instant.now().minus(90, ChronoUnit.DAYS).toString());
        activeAttrs.put("cpuAvg7d", "58.20%");
        active.setAttributes(activeAttrs);
        active.addMetric(new ResourceMetric("CPUUtilization_7d_avg", new BigDecimal("58.20"), "Percent",
                Instant.now().minus(7, ChronoUnit.DAYS), Instant.now()));
        simulated.add(active);

        // 4. Stopped instance
        ResourceSnapshot stopped = new ResourceSnapshot(
                job, ResourceType.EC2, "i-077c11a99f0e44b33", "legacy-migration-temp", region, "stopped");
        Map<String, String> stoppedAttrs = new LinkedHashMap<>();
        stoppedAttrs.put("instanceType", "t3.small");
        stoppedAttrs.put("architecture", "x86_64");
        stoppedAttrs.put("availabilityZone", region + "b");
        stoppedAttrs.put("privateIp", "10.0.2.88");
        stoppedAttrs.put("publicIp", "");
        stoppedAttrs.put("launchTime", Instant.now().minus(120, ChronoUnit.DAYS).toString());
        stopped.setAttributes(stoppedAttrs);
        simulated.add(stopped);

        return simulated;
    }
}
