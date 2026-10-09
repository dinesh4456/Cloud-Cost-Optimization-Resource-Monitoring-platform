package com.cloudops.optimizer.recommendation;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<Recommendation> listRecommendations() {
        return recommendationService.findAll();
    }

    @GetMapping("/{id}")
    public Recommendation getRecommendation(@PathVariable Long id) {
        return recommendationService.findById(id);
    }

    @PatchMapping("/{id}")
    public Recommendation updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.getOrDefault("status", "ACKNOWLEDGED");
        RecommendationStatus status = RecommendationStatus.valueOf(statusStr.toUpperCase());
        return recommendationService.updateStatus(id, status);
    }
}
