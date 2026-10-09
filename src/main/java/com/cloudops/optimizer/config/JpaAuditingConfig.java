package com.cloudops.optimizer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on automatic createdAt / updatedAt filling for entities that use
 * {@code @CreatedDate} and {@code @LastModifiedDate}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
