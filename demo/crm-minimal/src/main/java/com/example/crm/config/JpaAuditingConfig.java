package com.example.crm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA's standard {@code @CreatedBy}/{@code @LastModifiedBy}/
 * {@code @CreatedDate}/{@code @LastModifiedDate} entity auditing. This is complementary to
 * (not a replacement for) {@code tenant-audit}'s {@code @Audited} AOP aspect.
 *
 * <p>{@code auditorAwareRef} points at {@code tenant-audit}'s default
 * {@code SecurityContextActorResolver} bean, which already implements {@code AuditorAware<String>}
 * (and is also the default {@code ActorResolver} for {@code @Audited}) — no custom
 * {@code AuditorAware} bean needed here anymore.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "securityContextActorResolver")
public class JpaAuditingConfig {
}