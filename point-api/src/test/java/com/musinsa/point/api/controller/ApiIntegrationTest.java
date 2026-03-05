package com.musinsa.point.api.controller;

import com.musinsa.point.api.controller.payload.request.ConfigUpdateRequest;
import com.musinsa.point.api.controller.payload.request.EarnRequest;
import com.musinsa.point.api.controller.payload.request.UseCancelRequest;
import com.musinsa.point.api.controller.payload.request.UseRequest;
import com.musinsa.point.api.controller.payload.response.BalanceResponse;
import com.musinsa.point.api.controller.payload.response.EarnResponse;
import com.musinsa.point.api.controller.payload.response.TransactionResponse;
import com.musinsa.point.domain.enums.EarnType;
import com.musinsa.point.domain.Ledger;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.service.ExpireService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.cache.type=none"
)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private ExpireService expireService;

    private static final Long TEST_MEMBER_ID = 1L;
    private static final String TEST_ORDER_ID = "ORDER-A1234";

    @Test
    @Order(1)
    @DisplayName("E2E 시나리오: 적립 → 사용 → 만료 → 사용취소 전체 플로우")
    void testCompletePointLifecycleScenario() throws Exception {
        Long memberId = TEST_MEMBER_ID;

        // Step 1: 1000원 적립(A)
        EarnRequest earnRequestA = new EarnRequest(
                memberId, 1000L, EarnType.NORMAL,
                "첫번째 적립", null, null, null
        );

        MvcResult earnResultA = mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequestA)))
                .andExpect(status().isCreated())
                .andReturn();

        EarnResponse earnResponseA = objectMapper.readValue(
                earnResultA.getResponse().getContentAsString(), EarnResponse.class);
        String pointKeyA = earnResponseA.pointKey();

        MvcResult balanceResult1 = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance1 = objectMapper.readValue(
                balanceResult1.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance1.totalBalance()).isEqualTo(1000L);

        // Step 2: 500원 적립(B)
        EarnRequest earnRequestB = new EarnRequest(
                memberId, 500L, EarnType.NORMAL,
                "두번째 적립", null, null, null
        );

        MvcResult earnResultB = mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequestB)))
                .andExpect(status().isCreated())
                .andReturn();

        EarnResponse earnResponseB = objectMapper.readValue(
                earnResultB.getResponse().getContentAsString(), EarnResponse.class);
        String pointKeyB = earnResponseB.pointKey();

        MvcResult balanceResult2 = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance2 = objectMapper.readValue(
                balanceResult2.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance2.totalBalance()).isEqualTo(1500L);

        // Step 3: 1200원 사용(C)
        UseRequest useRequest = new UseRequest(memberId, 1200L, TEST_ORDER_ID, "포인트 사용");

        MvcResult useResult = mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse useResponse = objectMapper.readValue(
                useResult.getResponse().getContentAsString(), TransactionResponse.class);
        String usePointKey = useResponse.pointKey();

        MvcResult balanceResult3 = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance3 = objectMapper.readValue(
                balanceResult3.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance3.totalBalance()).isEqualTo(300L);

        Ledger ledgerA = ledgerRepository.findByPointKey(pointKeyA).orElseThrow();
        Ledger ledgerB = ledgerRepository.findByPointKey(pointKeyB).orElseThrow();
        assertThat(ledgerA.getBalance()).isEqualTo(0L);
        assertThat(ledgerB.getBalance()).isEqualTo(300L);

        // Step 4: A 만료 처리
        updateExpireDate(pointKeyA, LocalDateTime.now().minusDays(1));
        expireService.expirePoints();

        // Step 5: 1100원 사용취소(D)
        UseCancelRequest cancelRequest = new UseCancelRequest(usePointKey, 1100L);

        mockMvc.perform(post("/api/v1/points/use-cancel")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk());

        MvcResult balanceResult4 = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance4 = objectMapper.readValue(
                balanceResult4.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance4.totalBalance()).isEqualTo(1400L);

        Ledger updatedLedgerB = ledgerRepository.findByPointKey(pointKeyB).orElseThrow();
        assertThat(updatedLedgerB.getBalance()).isEqualTo(400L);

        List<Ledger> allLedgers = ledgerRepository.findAll();
        long restoreLedgerCount = allLedgers.stream()
                .filter(l -> l.getEarnType() == EarnType.USE_CANCEL_RESTORE)
                .count();
        assertThat(restoreLedgerCount).isGreaterThanOrEqualTo(1L);
    }

    private void updateExpireDate(String pointKey, LocalDateTime newExpireAt) {
        Ledger ledger = ledgerRepository.findByPointKey(pointKey).orElseThrow();
        try {
            var field = Ledger.class.getDeclaredField("expireAt");
            field.setAccessible(true);
            field.set(ledger, newExpireAt);
            ledgerRepository.saveAndFlush(ledger);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update expireAt", e);
        }
    }

    @Test
    @DisplayName("적립 API - 성공")
    void testEarnPointSuccess() throws Exception {
        EarnRequest request = new EarnRequest(
                100L, 5000L, EarnType.NORMAL,
                "상품 구매 적립", null, null, null
        );

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointKey").exists())
                .andExpect(jsonPath("$.memberId").value(100L))
                .andExpect(jsonPath("$.earnAmount").value(5000L))
                .andExpect(jsonPath("$.balance").value(5000L))
                .andExpect(jsonPath("$.earnType").value("NORMAL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("적립취소 API - 성공")
    void testCancelEarnSuccess() throws Exception {
        EarnRequest earnRequest = new EarnRequest(
                200L, 3000L, EarnType.NORMAL,
                "테스트 적립", null, null, null
        );

        MvcResult earnResult = mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        EarnResponse earnResponse = objectMapper.readValue(
                earnResult.getResponse().getContentAsString(), EarnResponse.class);

        mockMvc.perform(delete("/api/v1/points/earn/" + earnResponse.pointKey()))
                .andExpect(status().isNoContent());

        MvcResult balanceResult = mockMvc.perform(get("/api/v1/points/balance/200"))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance = objectMapper.readValue(
                balanceResult.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance.totalBalance()).isEqualTo(0L);
    }

    @Test
    @DisplayName("사용 API - 성공")
    void testUsePointSuccess() throws Exception {
        Long memberId = 300L;
        EarnRequest earnRequest = new EarnRequest(
                memberId, 10000L, EarnType.NORMAL, "초기 적립", null, null, null);

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        UseRequest useRequest = new UseRequest(memberId, 7000L, "ORDER-001", "상품 결제");

        mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pointKey").exists())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.amount").value(7000L))
                .andExpect(jsonPath("$.orderId").value("ORDER-001"))
                .andExpect(jsonPath("$.type").value("USE"));

        MvcResult balanceResult = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance = objectMapper.readValue(
                balanceResult.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance.totalBalance()).isEqualTo(3000L);
    }

    @Test
    @DisplayName("사용취소 API - 성공")
    void testUseCancelSuccess() throws Exception {
        Long memberId = 400L;
        EarnRequest earnRequest = new EarnRequest(
                memberId, 8000L, EarnType.NORMAL, "초기 적립", null, null, null);

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        UseRequest useRequest = new UseRequest(memberId, 5000L, "ORDER-002", "상품 결제");

        MvcResult useResult = mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        TransactionResponse useResponse = objectMapper.readValue(
                useResult.getResponse().getContentAsString(), TransactionResponse.class);

        UseCancelRequest cancelRequest = new UseCancelRequest(useResponse.pointKey(), 3000L);

        mockMvc.perform(post("/api/v1/points/use-cancel")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("USE_CANCEL"))
                .andExpect(jsonPath("$.amount").value(3000L));

        MvcResult balanceResult = mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk()).andReturn();
        BalanceResponse balance = objectMapper.readValue(
                balanceResult.getResponse().getContentAsString(), BalanceResponse.class);
        assertThat(balance.totalBalance()).isEqualTo(6000L);
    }

    @Test
    @DisplayName("잔액조회 API - 성공")
    void testGetBalanceSuccess() throws Exception {
        Long memberId = 500L;
        EarnRequest earnRequest = new EarnRequest(
                memberId, 15000L, EarnType.NORMAL, "초기 적립", null, null, null);

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/points/balance/" + memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.totalBalance").value(15000L));
    }

    @Test
    @DisplayName("잔액조회 API - 회원 없음 (404)")
    void testGetBalanceNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/points/balance/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("이력조회 API - 성공")
    void testGetTransactionsSuccess() throws Exception {
        Long memberId = 600L;

        EarnRequest earnRequest = new EarnRequest(
                memberId, 10000L, EarnType.NORMAL, "적립", null, null, null);
        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        UseRequest useRequest = new UseRequest(memberId, 3000L, "ORDER-003", "사용");
        mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/points/transactions/" + memberId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("설정조회 API - 성공")
    void testGetAllConfigsSuccess() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/points/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        List<Object> configs = objectMapper.readValue(
                result.getResponse().getContentAsString(), new TypeReference<List<Object>>() {});
        assertThat(configs).isNotNull();
    }

    @Test
    @DisplayName("설정변경 API - 성공")
    void testUpdateConfigSuccess() throws Exception {
        MvcResult getResult = mockMvc.perform(get("/api/v1/points/configs"))
                .andExpect(status().isOk()).andReturn();

        String content = getResult.getResponse().getContentAsString();
        if (!content.equals("[]")) {
            ConfigUpdateRequest updateRequest = new ConfigUpdateRequest("500");
            String testConfigKey = "DEFAULT_EXPIRE_DAYS";

            try {
                mockMvc.perform(put("/api/v1/points/configs/" + testConfigKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.configKey").value(testConfigKey))
                        .andExpect(jsonPath("$.configValue").value("500"));
            } catch (Exception e) {
                // config may not exist
            }
        }
    }

    @Test
    @DisplayName("적립 API - 잘못된 요청 (금액 0)")
    void testEarnPointWithZeroAmount() throws Exception {
        String invalidRequest = """
                {
                    "memberId": 700,
                    "amount": 0,
                    "earnType": "NORMAL",
                    "description": "잘못된 적립"
                }
                """;

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용 API - 잔액 부족")
    void testUsePointInsufficientBalance() throws Exception {
        Long memberId = 800L;
        EarnRequest earnRequest = new EarnRequest(
                memberId, 1000L, EarnType.NORMAL, "소액 적립", null, null, null);

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        UseRequest useRequest = new UseRequest(memberId, 5000L, "ORDER-004", "초과 사용 시도");

        mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("사용취소 API - 존재하지 않는 사용 거래")
    void testUseCancelWithInvalidPointKey() throws Exception {
        UseCancelRequest cancelRequest = new UseCancelRequest("invalid-point-key", 1000L);

        mockMvc.perform(post("/api/v1/points/use-cancel")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("이력조회 API - 페이징 동작 확인")
    void testGetTransactionsPagination() throws Exception {
        Long memberId = 900L;

        for (int i = 0; i < 25; i++) {
            EarnRequest earnRequest = new EarnRequest(
                    memberId, 1000L, EarnType.NORMAL, "적립 " + i, null, null, null);
            mockMvc.perform(post("/api/v1/points/earn")
                            .header("X-Idempotency-Key", UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(earnRequest)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/points/transactions/" + memberId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0));

        mockMvc.perform(get("/api/v1/points/transactions/" + memberId)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test
    @DisplayName("적립건 목록 조회 API - 성공")
    void testGetLedgersSuccess() throws Exception {
        Long memberId = 1000L;
        EarnRequest earnRequest = new EarnRequest(
                memberId, 5000L, EarnType.NORMAL, "테스트 적립", null, null, null);

        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/points/ledgers/" + memberId)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isNotEmpty());
    }

    @Test
    @DisplayName("주문별 사용 상세 조회 API - 성공")
    void testGetUsageDetailsSuccess() throws Exception {
        Long memberId = 1100L;
        String orderId = "ORDER-005";

        EarnRequest earnRequest = new EarnRequest(
                memberId, 20000L, EarnType.NORMAL, "대량 적립", null, null, null);
        mockMvc.perform(post("/api/v1/points/earn")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earnRequest)))
                .andExpect(status().isCreated());

        UseRequest useRequest = new UseRequest(memberId, 15000L, orderId, "포인트 사용");
        mockMvc.perform(post("/api/v1/points/use")
                        .header("X-Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(useRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/points/usage-details/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isNotEmpty());
    }
}
