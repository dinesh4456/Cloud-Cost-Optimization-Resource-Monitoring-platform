package com.cloudops.optimizer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CloudCostOptimizerApplicationTests {

    @Test
    void contextLoads() {
        // Spring context starts with the test profile (H2).
    }
}
