package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.ConfigUpdateRequest;
import com.musinsa.point.domain.Config;
import com.musinsa.point.service.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConfigController.class)
@AutoConfigureRestDocs
class ConfigControllerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfigService configService;

    @Test
    @DisplayName("설정 전체 조회 API 문서")
    void getAllConfigs() throws Exception {
        Config config1 = Config.builder()
                .configKey("DEFAULT_EXPIRE_DAYS")
                .configValue("365")
                .description("기본 포인트 만료 일수")
                .build();
        ReflectionTestUtils.setField(config1, "configId", 1L);

        Config config2 = Config.builder()
                .configKey("MAX_EARN_AMOUNT_PER_ONCE")
                .configValue("1000000")
                .description("1회 최대 적립 금액")
                .build();
        ReflectionTestUtils.setField(config2, "configId", 2L);

        when(configService.getAllConfigs()).thenReturn(List.of(config1, config2));

        mockMvc.perform(get("/api/v1/points/configs"))
                .andExpect(status().isOk())
                .andDo(document("point-config/get-all",
                        responseFields(
                                fieldWithPath("[].configId").description("설정 ID"),
                                fieldWithPath("[].configKey").description("설정 키"),
                                fieldWithPath("[].configValue").description("설정 값"),
                                fieldWithPath("[].description").description("설정 설명")
                        )
                ));
    }

    @Test
    @DisplayName("설정 변경 API 문서")
    void updateConfig() throws Exception {
        Config updatedConfig = Config.builder()
                .configKey("DEFAULT_EXPIRE_DAYS")
                .configValue("500")
                .description("기본 포인트 만료 일수")
                .build();
        ReflectionTestUtils.setField(updatedConfig, "configId", 1L);

        when(configService.updateConfig(eq("DEFAULT_EXPIRE_DAYS"), anyString()))
                .thenReturn(updatedConfig);

        ConfigUpdateRequest request = new ConfigUpdateRequest("500");

        mockMvc.perform(put("/api/v1/points/configs/{configKey}", "DEFAULT_EXPIRE_DAYS")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("point-config/update",
                        pathParameters(
                                parameterWithName("configKey").description("변경할 설정 키")
                        ),
                        requestFields(
                                fieldWithPath("configValue").description("변경할 설정 값")
                        ),
                        responseFields(
                                fieldWithPath("configId").description("설정 ID"),
                                fieldWithPath("configKey").description("설정 키"),
                                fieldWithPath("configValue").description("설정 값"),
                                fieldWithPath("description").description("설정 설명")
                        )
                ));
    }
}
