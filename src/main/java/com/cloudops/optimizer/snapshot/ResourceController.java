package com.cloudops.optimizer.snapshot;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public List<ResourceSnapshot> listResources(@RequestParam(required = false) ResourceType type) {
        return resourceService.findLatestResources(type);
    }

    @GetMapping("/{id}")
    public ResourceSnapshot getResource(@PathVariable Long id) {
        return resourceService.findById(id);
    }

    @GetMapping("/{id}/metrics")
    public List<ResourceMetric> getMetrics(@PathVariable Long id) {
        return resourceService.findMetricsBySnapshotId(id);
    }
}
