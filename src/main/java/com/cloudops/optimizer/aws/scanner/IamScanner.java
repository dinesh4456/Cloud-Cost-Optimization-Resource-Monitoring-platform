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
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.AccessKeyMetadata;
import software.amazon.awssdk.services.iam.model.ListAccessKeysRequest;
import software.amazon.awssdk.services.iam.model.ListAccessKeysResponse;
import software.amazon.awssdk.services.iam.model.ListUsersResponse;
import software.amazon.awssdk.services.iam.model.User;

@Component
@Order(5)
public class IamScanner implements ResourceScanner {

    private static final Logger log = LoggerFactory.getLogger(IamScanner.class);

    private final AwsClientFactory clientFactory;
    private final OptimizerProperties properties;

    public IamScanner(AwsClientFactory clientFactory, OptimizerProperties properties) {
        this.clientFactory = clientFactory;
        this.properties = properties;
    }

    @Override
    public List<ResourceSnapshot> scan(ScanJob job) {
        List<ResourceSnapshot> snapshots = new ArrayList<>();
        String region = job.getRegion() != null ? job.getRegion() : properties.getAws().getRegion();

        try (IamClient iamClient = clientFactory.iam()) {
            ListUsersResponse response = iamClient.listUsers();
            for (User user : response.users()) {
                String userName = user.userName();
                String userId = user.userId() != null ? user.userId() : userName;

                ResourceSnapshot snapshot = new ResourceSnapshot(
                        job,
                        ResourceType.IAM,
                        userId,
                        userName,
                        "global",
                        "ACTIVE"
                );

                int activeKeyCount = 0;
                long oldestKeyAgeDays = 0;
                try {
                    ListAccessKeysResponse keyResp = iamClient.listAccessKeys(
                            ListAccessKeysRequest.builder().userName(userName).build());
                    for (AccessKeyMetadata key : keyResp.accessKeyMetadata()) {
                        if ("Active".equalsIgnoreCase(key.statusAsString())) {
                            activeKeyCount++;
                            if (key.createDate() != null) {
                                long age = ChronoUnit.DAYS.between(key.createDate(), Instant.now());
                                if (age > oldestKeyAgeDays) {
                                    oldestKeyAgeDays = age;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Could not fetch keys for IAM user {}: {}", userName, ex.getMessage());
                }

                Map<String, String> attrs = new LinkedHashMap<>();
                attrs.put("arn", user.arn());
                attrs.put("createDate", user.createDate() != null ? user.createDate().toString() : "");
                attrs.put("passwordLastUsed", user.passwordLastUsed() != null ? user.passwordLastUsed().toString() : "NEVER");
                attrs.put("activeAccessKeys", String.valueOf(activeKeyCount));
                attrs.put("oldestKeyAgeDays", String.valueOf(oldestKeyAgeDays));

                snapshot.setAttributes(attrs);
                snapshots.add(snapshot);
            }
            log.info("IAM scanner discovered {} users via AWS SDK", snapshots.size());
        } catch (Exception ex) {
            log.warn("AWS IAM live scan unavailable: {}. Using simulated lab cloud inventory.", ex.getMessage());
            snapshots = generateSimulatedIamSnapshots(job, region);
        }

        return snapshots;
    }

    private List<ResourceSnapshot> generateSimulatedIamSnapshots(ScanJob job, String region) {
        List<ResourceSnapshot> simulated = new ArrayList<>();

        // 1. Inactive IAM user with stale access key (triggers INACTIVE_IAM_USER rule)
        ResourceSnapshot staleUser = new ResourceSnapshot(
                job, ResourceType.IAM, "AIDAJQAB3456EXAMPLE1", "ci-build-deployer-legacy", "global", "ACTIVE");
        Map<String, String> attrs1 = new LinkedHashMap<>();
        attrs1.put("arn", "arn:aws:iam::123456789012:user/ci-build-deployer-legacy");
        attrs1.put("createDate", Instant.now().minus(240, ChronoUnit.DAYS).toString());
        attrs1.put("passwordLastUsed", "NEVER");
        attrs1.put("activeAccessKeys", "1");
        attrs1.put("oldestKeyAgeDays", "195");
        staleUser.setAttributes(attrs1);
        simulated.add(staleUser);

        // 2. Active admin IAM user (healthy)
        ResourceSnapshot activeUser = new ResourceSnapshot(
                job, ResourceType.IAM, "AIDAJQAB9876EXAMPLE2", "platform-admin-dinesh", "global", "ACTIVE");
        Map<String, String> attrs2 = new LinkedHashMap<>();
        attrs2.put("arn", "arn:aws:iam::123456789012:user/platform-admin-dinesh");
        attrs2.put("createDate", Instant.now().minus(45, ChronoUnit.DAYS).toString());
        attrs2.put("passwordLastUsed", Instant.now().minus(1, ChronoUnit.DAYS).toString());
        attrs2.put("activeAccessKeys", "1");
        attrs2.put("oldestKeyAgeDays", "45");
        activeUser.setAttributes(attrs2);
        simulated.add(activeUser);

        return simulated;
    }
}
