package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.UseCancelRequest;
import com.musinsa.point.api.controller.payload.request.UseRequest;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.api.service.UseFacadeService;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.service.UseService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UseController.class)
@AutoConfigureRestDocs
class UseControllerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UseFacadeService useFacadeService;

    @Test
    @DisplayName("포인트 사용 API 문서")
    void use() throws Exception {
        Transaction transaction = Transaction.builder()
                .memberId(1L)
                .type(TransactionType.USE)
                .amount(3000L)
                .orderId("ORDER-001")
                .description("상품 결제")
                .build();
        ReflectionTestUtils.setField(transaction, "transactionId", 1L);
        ReflectionTestUtils.setField(transaction, "createdAt", LocalDateTime.now());

        when(useFacadeService.use(anyString(), anyLong(), anyLong(), anyString(), any()))
                .thenReturn(TransactionResponse.from(transaction));

        UseRequest request = new UseRequest(1L, 3000L, "ORDER-001", "상품 결제");

        mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", "550e8400-e29b-41d4-a716-446655440001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andDo(document("point-use/use",
                        requestHeaders(
                                headerWithName("X-Idempotency-Key").description("멱등성 키 (UUID)")
                        ),
                        requestFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("amount").description("사용 금액"),
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("description").description("사용 설명").optional()
                        ),
                        responseFields(
                                fieldWithPath("transactionId").description("거래 ID"),
                                fieldWithPath("pointKey").description("포인트 키 (UUID)"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("type").description("거래 유형 (USE)"),
                                fieldWithPath("amount").description("사용 금액"),
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("relatedPointKey").description("관련 포인트 키").optional(),
                                fieldWithPath("description").description("설명"),
                                fieldWithPath("createdAt").description("생성 일시")
                        )
                ));
    }

    @Test
    @DisplayName("사용 취소 API 문서")
    void cancelUse() throws Exception {
        Transaction cancelTransaction = Transaction.builder()
                .memberId(1L)
                .type(TransactionType.USE_CANCEL)
                .amount(2000L)
                .orderId("ORDER-001")
                .relatedPointKey("use-point-key-1234")
                .description("사용취소(부분)")
                .build();
        ReflectionTestUtils.setField(cancelTransaction, "transactionId", 2L);
        ReflectionTestUtils.setField(cancelTransaction, "createdAt", LocalDateTime.now());

        when(useFacadeService.cancelUse(anyString(), anyString(), anyLong()))
                .thenReturn(TransactionResponse.from(cancelTransaction));

        UseCancelRequest request = new UseCancelRequest("use-point-key-1234", 2000L);

        mockMvc.perform(post("/api/v1/points/use-cancel")
                        .header("X-Idempotency-Key", "550e8400-e29b-41d4-a716-446655440002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(document("point-use/cancel-use",
                        requestHeaders(
                                headerWithName("X-Idempotency-Key").description("멱등성 키 (UUID)")
                        ),
                        requestFields(
                                fieldWithPath("usePointKey").description("사용 거래 포인트 키"),
                                fieldWithPath("cancelAmount").description("취소 금액")
                        ),
                        responseFields(
                                fieldWithPath("transactionId").description("거래 ID"),
                                fieldWithPath("pointKey").description("포인트 키 (UUID)"),
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("type").description("거래 유형 (USE_CANCEL)"),
                                fieldWithPath("amount").description("취소 금액"),
                                fieldWithPath("orderId").description("주문 ID"),
                                fieldWithPath("relatedPointKey").description("원본 사용 거래 포인트 키"),
                                fieldWithPath("description").description("설명"),
                                fieldWithPath("createdAt").description("생성 일시")
                        )
                ));
    }
}
