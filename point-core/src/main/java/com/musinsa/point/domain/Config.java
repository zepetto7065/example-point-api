package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Config extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(nullable = false)
    private String configValue;

    private String description;

    @Builder
    public Config(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
    }

    public void updateValue(String configValue) {
        this.configValue = configValue;
    }

    public long toLong() {
        return Long.parseLong(this.configValue);
    }
}
