package com.musinsa.point.service;

import com.musinsa.point.domain.Config;
import com.musinsa.point.domain.enums.ConfigKey;
import com.musinsa.point.exception.PointErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfigService {

    private final ConfigRepository configRepository;

    @Cacheable(value = "config", key = "#configKey.name()")
    public long getConfigValue(ConfigKey configKey) {
        Config config = configRepository.findByConfigKey(configKey.name())
                .orElseThrow(() -> new PointException(PointErrorCode.CONFIG_NOT_FOUND));
        return config.toLong();
    }

    @Transactional(readOnly = true)
    public List<Config> getAllConfigs() {
        return configRepository.findAll();
    }

    @CacheEvict(value = "config", key = "#configKey")
    @Transactional
    public Config updateConfig(String configKey, String configValue) {
        Config config = configRepository.findByConfigKey(configKey)
                .orElseThrow(() -> new PointException(PointErrorCode.CONFIG_NOT_FOUND));
        config.updateValue(configValue);
        return config;
    }

}
