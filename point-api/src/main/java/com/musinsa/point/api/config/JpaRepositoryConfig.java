package com.musinsa.point.api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan("com.musinsa.point.domain")
@EnableJpaRepositories("com.musinsa.point.repository")
public class JpaRepositoryConfig {
}
