package com.musinsa.point.service;

import com.musinsa.point.domain.Config;
import com.musinsa.point.domain.enums.ConfigKey;
import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.ConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService 단위 테스트")
class ConfigServiceTest {

    @Mock
    private ConfigRepository configRepository;

    @InjectMocks
    private ConfigService configService;

    @Test
    @DisplayName("설정값 조회 성공")
    void getConfigValue_success() {
        // given
        ConfigKey configKey = ConfigKey.DEFAULT_EXPIRE_DAYS;
        Config config = Config.builder()
                .configKey(configKey.name())
                .configValue("365")
                .build();
        given(configRepository.findByConfigKey(configKey.name()))
                .willReturn(Optional.of(config));

        // when
        long result = configService.getConfigValue(configKey);

        // then
        assertThat(result).isEqualTo(365L);
    }

    @Test
    @DisplayName("설정값 조회 실패 - 설정 없음")
    void getConfigValue_notFound() {
        // given
        ConfigKey configKey = ConfigKey.DEFAULT_EXPIRE_DAYS;
        given(configRepository.findByConfigKey(configKey.name()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> configService.getConfigValue(configKey))
                .isInstanceOf(PointException.class);
    }

    @Test
    @DisplayName("전체 설정 조회 성공")
    void getAllConfigs_success() {
        // given
        List<Config> configs = List.of(
                Config.builder().configKey("KEY1").configValue("100").build(),
                Config.builder().configKey("KEY2").configValue("200").build()
        );
        given(configRepository.findAll()).willReturn(configs);

        // when
        List<Config> result = configService.getAllConfigs();

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("설정값 수정 성공")
    void updateConfig_success() {
        // given
        String configKey = "DEFAULT_EXPIRE_DAYS";
        String newValue = "500";
        Config config = Config.builder()
                .configKey(configKey)
                .configValue("365")
                .build();
        given(configRepository.findByConfigKey(configKey))
                .willReturn(Optional.of(config));

        // when
        Config result = configService.updateConfig(configKey, newValue);

        // then
        assertThat(result.getConfigValue()).isEqualTo(newValue);
    }

    @Test
    @DisplayName("설정값 수정 실패 - 설정 없음")
    void updateConfig_notFound() {
        // given
        String configKey = "NOT_EXISTS";
        given(configRepository.findByConfigKey(configKey))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> configService.updateConfig(configKey, "100"))
                .isInstanceOf(PointException.class);
    }
}
