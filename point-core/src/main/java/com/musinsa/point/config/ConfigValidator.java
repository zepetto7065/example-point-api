package com.musinsa.point.config;

import com.musinsa.point.domain.Config;
import com.musinsa.point.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigValidator implements ApplicationListener<ContextRefreshedEvent> {

    private final ConfigRepository configRepository;

    private static final List<String> REQUIRED_CONFIG_KEYS = List.of(
            "MAX_EARN_AMOUNT_PER_ONCE",    // 1회 최대 적립 금액
            "MAX_BALANCE_PER_MEMBER",       // 회원당 최대 잔액
            "MIN_EXPIRE_DAYS",              // 최소 만료 일수
            "MAX_EXPIRE_DAYS",              // 최대 만료 일수
            "DEFAULT_EXPIRE_DAYS"           // 기본 만료 일수
    );

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("=== 필수 설정값 검증 시작 ===");

        List<String> missingKeys = new ArrayList<>();

        for (String key : REQUIRED_CONFIG_KEYS) {
            Optional<Config> config = configRepository.findByConfigKey(key);

            if (config.isEmpty()) {
                missingKeys.add(key);
                log.error("필수 설정 누락: {}", key);
            } else {
                log.debug("설정 확인: {} = {}", key, config.get().getConfigValue());
            }
        }

        if (!missingKeys.isEmpty()) {
            String errorMessage = String.format("애플리케이션 시작 실패: 필수 설정값이 누락되었습니다. 누락된 설정: %s",missingKeys);
            log.error("=== 필수 설정값 검증 실패 ===");
            throw new IllegalStateException(errorMessage);
        }

        log.info("=== 필수 설정값 검증 완료, (총 {}개) ===", REQUIRED_CONFIG_KEYS.size());
    }
}