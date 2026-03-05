package com.musinsa.point.config;

import com.musinsa.point.domain.Config;
import com.musinsa.point.repository.ConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigValidator 테스트")
class ConfigValidatorTest {

    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private ConfigValidator configValidator;

    @Mock
    private ContextRefreshedEvent event;

    @Test
    @DisplayName("모든 필수 설정이 존재하면 검증 성공")
    void validateConfigs_success_allConfigsPresent() {
        // given
        when(configRepository.findByConfigKey("MAX_EARN_AMOUNT_PER_ONCE"))
                .thenReturn(Optional.of(createConfig("MAX_EARN_AMOUNT_PER_ONCE", "10000")));
        when(configRepository.findByConfigKey("MAX_BALANCE_PER_MEMBER"))
                .thenReturn(Optional.of(createConfig("MAX_BALANCE_PER_MEMBER", "100000")));
        when(configRepository.findByConfigKey("MIN_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("MIN_EXPIRE_DAYS", "1")));
        when(configRepository.findByConfigKey("MAX_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("MAX_EXPIRE_DAYS", "730")));
        when(configRepository.findByConfigKey("DEFAULT_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("DEFAULT_EXPIRE_DAYS", "365")));

        // when & then
        assertThatCode(() -> configValidator.onApplicationEvent(event))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("필수 설정 하나라도 누락되면 검증 실패")
    void validateConfigs_fail_missingConfig() {
        // given
        when(configRepository.findByConfigKey("MAX_EARN_AMOUNT_PER_ONCE"))
                .thenReturn(Optional.of(createConfig("MAX_EARN_AMOUNT_PER_ONCE", "10000")));
        when(configRepository.findByConfigKey("MAX_BALANCE_PER_MEMBER"))
                .thenReturn(Optional.empty()); // 누락
        when(configRepository.findByConfigKey("MIN_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("MIN_EXPIRE_DAYS", "1")));
        when(configRepository.findByConfigKey("MAX_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("MAX_EXPIRE_DAYS", "730")));
        when(configRepository.findByConfigKey("DEFAULT_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("DEFAULT_EXPIRE_DAYS", "365")));

        // when & then
        assertThatThrownBy(() -> configValidator.onApplicationEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("필수 설정값이 누락되었습니다")
                .hasMessageContaining("MAX_BALANCE_PER_MEMBER");
    }

    @Test
    @DisplayName("여러 설정이 누락되면 모두 에러 메시지에 포함")
    void validateConfigs_fail_multipleConfigsMissing() {
        // given
        when(configRepository.findByConfigKey("MAX_EARN_AMOUNT_PER_ONCE"))
                .thenReturn(Optional.of(createConfig("MAX_EARN_AMOUNT_PER_ONCE", "10000")));
        when(configRepository.findByConfigKey("MAX_BALANCE_PER_MEMBER"))
                .thenReturn(Optional.empty()); // 누락
        when(configRepository.findByConfigKey("MIN_EXPIRE_DAYS"))
                .thenReturn(Optional.of(createConfig("MIN_EXPIRE_DAYS", "1")));
        when(configRepository.findByConfigKey("MAX_EXPIRE_DAYS"))
                .thenReturn(Optional.empty()); // 누락
        when(configRepository.findByConfigKey("DEFAULT_EXPIRE_DAYS"))
                .thenReturn(Optional.empty()); // 누락

        // when & then
        assertThatThrownBy(() -> configValidator.onApplicationEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MAX_BALANCE_PER_MEMBER")
                .hasMessageContaining("MAX_EXPIRE_DAYS")
                .hasMessageContaining("DEFAULT_EXPIRE_DAYS");
    }

    private Config createConfig(String key, String value) {
        return Config.builder()
                .configKey(key)
                .configValue(value)
                .build();
    }
}