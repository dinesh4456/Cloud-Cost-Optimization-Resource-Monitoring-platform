package com.cloudops.optimizer.aws.scanner;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

@Component
public class CloudWatchMetricsCollector {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchMetricsCollector.class);

    public Optional<BigDecimal> average(
            CloudWatchClient client,
            String namespace,
            String metricName,
            String dimensionName,
            String dimensionValue,
            int lookbackDays,
            String extraDimensionName,
            String extraDimensionValue) {
        Instant end = Instant.now();
        Instant start = end.minus(Duration.ofDays(lookbackDays));
        try {
            GetMetricStatisticsRequest.Builder builder = GetMetricStatisticsRequest.builder()
                    .namespace(namespace)
                    .metricName(metricName)
                    .startTime(start)
                    .endTime(end)
                    .period(86400)
                    .statistics(Statistic.AVERAGE)
                    .dimensions(Dimension.builder().name(dimensionName).value(dimensionValue).build());
            if (extraDimensionName != null) {
                builder.dimensions(
                        Dimension.builder().name(dimensionName).value(dimensionValue).build(),
                        Dimension.builder().name(extraDimensionName).value(extraDimensionValue).build());
            }
            GetMetricStatisticsResponse response = client.getMetricStatistics(builder.build());
            if (response.datapoints() == null || response.datapoints().isEmpty()) {
                return Optional.empty();
            }
            double avg = response.datapoints().stream()
                    .mapToDouble(point -> point.average() == null ? 0.0 : point.average())
                    .average()
                    .orElse(0);
            return Optional.of(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        } catch (Exception ex) {
            log.warn("CloudWatch {}/{} failed for {}: {}", namespace, metricName, dimensionValue, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> average(
            CloudWatchClient client,
            String namespace,
            String metricName,
            String dimensionName,
            String dimensionValue,
            int lookbackDays) {
        return average(client, namespace, metricName, dimensionName, dimensionValue, lookbackDays, null, null);
    }
}
