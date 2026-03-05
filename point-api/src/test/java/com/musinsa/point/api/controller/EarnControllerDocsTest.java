package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.EarnRequest;
import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.api.service.EarnFacadeService;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.service.EarnService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EarnController.class)
@AutoConfigureRestDocs
class EarnControllerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EarnFacadeService earnFacadeService;

    @MockitoBean
    private EarnService earnService;

    @Test
    @DisplayName("포인트 적립 API 문서")
    void earn() throws Exception {
        Ledger ledger = Ledger.builder()
                .memberId(1L)
                .earnAmount(5000L)
                .earnType(EarnType.NORMAL)
                .description("구매 적립")
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ReflectionTestUtils.setField(ledger, "ledgerId", 1L);
        ReflectionTestUtils.setField(ledger, "createdAt", LocalDateTime.now());

        when(earnFacadeService.earn(anyString(), anyLong(), anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(EarnResponse.from(ledger));

        EarnRequest request = new EarnRequest(1L, 5000L, EarnType.NORMAL, "구매 적립", null,
                LocalDate.now(), LocalDate.now().plusDays(365));

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", "550e8400-e29b-41d4-a716-446655440000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("point-earn/earn",
                        requestHeaders(
                                headerWithName("X-Idempotency-Key").description("멱등성 키 (UUID)")
                        ),
                        requestFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("amount").description("적립 금액"),
                                fieldWithPath("earnType").description("적립 유형 (NORMAL, ADMIN_MANUAL)"),
                                fieldWithPath("description").description("적립 설명").optional(),
                                fieldWithPath("userId").description("사용자 ID (ADMIN_MANUAL인 경우)").optional(),
                                fieldWithPath("startDate").description("시작일 (미입력 시 오늘)").optional(),
                                fieldWithPath("expireDate").description("만료일 (미입력 시 시작일 + 기본 만료일수)").optional()
                        ),
                        responseFields(
                                fieldWithPath("ledgerId").description("적립건 ID"),
                                fieldWithPath("pointKey").description("포인트 키 (UUID)"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("earnAmount").description("적립 금액"),
                                fieldWithPath("balance").description("적립건 잔액"),
                                fieldWithPath("earnType").description("적립 유형"),
                                fieldWithPath("description").description("적립 설명"),
                                fieldWithPath("status").description("상태 (ACTIVE, EXPIRED, CANCELED)"),
                                fieldWithPath("startAt").description("시작 일시"),
                                fieldWithPath("expireAt").description("만료 일시"),
                                fieldWithPath("createdAt").description("생성 일시")
                        )
                ));
    }

    @Test
    @DisplayName("적립 취소 API 문서")
    void cancelEarn() throws Exception {
        doNothing().when(earnService).cancelEarn(anyString());

        mockMvc.perform(delete("/api/v1/points/earn/{pointKey}", "pk-abcd-1234-efgh-5678"))
                .andExpect(status().isNoContent())
                .andDo(document("point-earn/cancel-earn",
                        pathParameters(
                                parameterWithName("pointKey").description("취소할 적립 포인트 키")
                        )
                ));
    }
}
