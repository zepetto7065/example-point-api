package com.musinsa.point.api.controller;

import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.enums.TransactionType;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Transaction;
import com.musinsa.point.domain.UsageDetail;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.service.QueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QueryController.class)
@AutoConfigureRestDocs
class QueryControllerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QueryService queryService;

    @Test
    @DisplayName("잔액 조회 API 문서")
    void getBalance() throws Exception {
        Wallet wallet = Wallet.builder()
                .memberId(1L)
                .totalBalance(15000L)
                .build();

        when(queryService.getBalance(eq(1L))).thenReturn(wallet);

        mockMvc.perform(get("/api/v1/points/balance/{memberId}", 1L))
                .andExpect(status().isOk())
                .andDo(document("point-query/balance",
                        pathParameters(
                                parameterWithName("memberId").description("회원 ID")
                        ),
                        responseFields(
                                fieldWithPath("memberId").description("회원 ID"),
                                fieldWithPath("totalBalance").description("총 잔액")
                        )
                ));
    }

    @Test
    @DisplayName("이력 조회 API 문서")
    void getTransactions() throws Exception {
        Transaction tx = Transaction.builder()
                .memberId(1L)
                .type(TransactionType.EARN)
                .amount(5000L)
                .description("구매 적립")
                .build();
        ReflectionTestUtils.setField(tx, "transactionId", 1L);
        ReflectionTestUtils.setField(tx, "createdAt", LocalDateTime.now());

        Page<Transaction> page = new PageImpl<>(
                List.of(tx), PageRequest.of(0, 20), 1);

        when(queryService.getTransactions(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/points/transactions/{memberId}", 1L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("point-query/transactions",
                        pathParameters(
                                parameterWithName("memberId").description("회원 ID")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값 20)").optional()
                        ),
                        relaxedResponseFields(
                                fieldWithPath("content[].transactionId").description("거래 ID"),
                                fieldWithPath("content[].pointKey").description("포인트 키"),
                                fieldWithPath("content[].memberId").description("회원 ID"),
                                fieldWithPath("content[].type").description("거래 유형 (EARN, USE, USE_CANCEL, EARN_CANCEL)"),
                                fieldWithPath("content[].amount").description("거래 금액"),
                                fieldWithPath("content[].orderId").description("주문 ID"),
                                fieldWithPath("content[].relatedPointKey").description("관련 포인트 키"),
                                fieldWithPath("content[].description").description("설명"),
                                fieldWithPath("content[].createdAt").description("생성 일시"),
                                fieldWithPath("totalElements").description("전체 요소 수"),
                                fieldWithPath("totalPages").description("전체 페이지 수"),
                                fieldWithPath("size").description("페이지 크기"),
                                fieldWithPath("number").description("현재 페이지 번호")
                        )
                ));
    }

    @Test
    @DisplayName("적립건 목록 조회 API 문서")
    void getLedgers() throws Exception {
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

        Page<Ledger> page = new PageImpl<>(
                List.of(ledger), PageRequest.of(0, 20), 1);

        when(queryService.getLedgers(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/points/ledgers/{memberId}", 1L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andDo(document("point-query/ledgers",
                        pathParameters(
                                parameterWithName("memberId").description("회원 ID")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (0부터 시작)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값 20)").optional()
                        ),
                        relaxedResponseFields(
                                fieldWithPath("content[].ledgerId").description("적립건 ID"),
                                fieldWithPath("content[].pointKey").description("포인트 키"),
                                fieldWithPath("content[].memberId").description("회원 ID"),
                                fieldWithPath("content[].earnAmount").description("적립 금액"),
                                fieldWithPath("content[].balance").description("적립건 잔액"),
                                fieldWithPath("content[].earnType").description("적립 유형 (NORMAL, ADMIN_MANUAL, USE_CANCEL_RESTORE)"),
                                fieldWithPath("content[].description").description("설명"),
                                fieldWithPath("content[].status").description("상태 (ACTIVE, EXPIRED, CANCELED)"),
                                fieldWithPath("content[].startAt").description("시작 일시"),
                                fieldWithPath("content[].expireAt").description("만료 일시"),
                                fieldWithPath("content[].createdAt").description("생성 일시"),
                                fieldWithPath("totalElements").description("전체 요소 수"),
                                fieldWithPath("totalPages").description("전체 페이지 수"),
                                fieldWithPath("size").description("페이지 크기"),
                                fieldWithPath("number").description("현재 페이지 번호")
                        )
                ));
    }

    @Test
    @DisplayName("주문별 사용 상세 조회 API 문서")
    void getUsageDetails() throws Exception {
        Transaction useTransaction = Transaction.builder()
                .memberId(1L)
                .type(TransactionType.USE)
                .amount(5000L)
                .orderId("ORDER-001")
                .build();
        ReflectionTestUtils.setField(useTransaction, "transactionId", 10L);

        Ledger ledger = Ledger.builder()
                .memberId(1L)
                .earnAmount(10000L)
                .earnType(EarnType.NORMAL)
                .startAt(LocalDate.now().atStartOfDay())
                .expireAt(LocalDate.now().plusDays(365).atTime(23, 59, 59))
                .build();
        ReflectionTestUtils.setField(ledger, "ledgerId", 1L);

        UsageDetail detail = UsageDetail.builder()
                .useTransaction(useTransaction)
                .ledger(ledger)
                .amount(5000L)
                .build();
        ReflectionTestUtils.setField(detail, "usageDetailId", 1L);

        when(queryService.getUsageDetailsByOrderId(eq("ORDER-001")))
                .thenReturn(List.of(detail));

        mockMvc.perform(get("/api/v1/points/usage-details/{orderId}", "ORDER-001"))
                .andExpect(status().isOk())
                .andDo(document("point-query/usage-details",
                        pathParameters(
                                parameterWithName("orderId").description("주문 ID")
                        ),
                        responseFields(
                                fieldWithPath("[].usageDetailId").description("사용 상세 ID"),
                                fieldWithPath("[].useTransactionId").description("사용 거래 ID"),
                                fieldWithPath("[].ledgerId").description("적립건 ID"),
                                fieldWithPath("[].ledgerPointKey").description("적립건 포인트 키"),
                                fieldWithPath("[].amount").description("사용 금액"),
                                fieldWithPath("[].cancelAmount").description("취소된 금액"),
                                fieldWithPath("[].cancellableAmount").description("취소 가능 금액")
                        )
                ));
    }
}
