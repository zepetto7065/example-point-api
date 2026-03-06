package com.musinsa.point.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PointErrorCode implements ErrorCode {

    // 적립 관련
    EXCEED_MAX_EARN_AMOUNT(HttpStatus.BAD_REQUEST, "P001", "1회 최대 적립 금액을 초과했습니다."),
    EXCEED_MAX_BALANCE(HttpStatus.BAD_REQUEST, "P002", "최대 보유 한도를 초과합니다."),
    INVALID_EXPIRE_DAYS(HttpStatus.BAD_REQUEST, "P003", "만료일수가 허용 범위를 벗어났습니다."),
    INVALID_EARN_AMOUNT(HttpStatus.BAD_REQUEST, "P004", "적립 금액은 1 이상이어야 합니다."),
    INVALID_EXPIRE_DATE(HttpStatus.BAD_REQUEST, "P005", "만료일은 시작일 이후여야 합니다."),

    // 적립취소 관련
    LEDGER_NOT_FOUND(HttpStatus.NOT_FOUND, "P010", "적립건을 찾을 수 없습니다."),
    LEDGER_ALREADY_CANCELED(HttpStatus.CONFLICT, "P011", "이미 취소된 적립건입니다."),
    LEDGER_PARTIALLY_USED(HttpStatus.CONFLICT, "P012", "일부 사용된 적립건은 취소할 수 없습니다."),

    // 사용 관련
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "P020", "잔액이 부족합니다."),
    INVALID_USE_AMOUNT(HttpStatus.BAD_REQUEST, "P021", "사용 금액은 1 이상이어야 합니다."),

    // 사용취소 관련
    TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "P030", "거래를 찾을 수 없습니다."),
    INVALID_CANCEL_AMOUNT(HttpStatus.BAD_REQUEST, "P031", "취소 금액이 올바르지 않습니다."),
    EXCEED_CANCELLABLE_AMOUNT(HttpStatus.BAD_REQUEST, "P032", "취소 가능 금액을 초과했습니다."),
    NOT_USE_TRANSACTION(HttpStatus.BAD_REQUEST, "P033", "사용 거래만 취소할 수 있습니다."),

    // 지갑 관련
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "P040", "포인트 지갑을 찾을 수 없습니다."),

    // 설정 관련
    CONFIG_NOT_FOUND(HttpStatus.NOT_FOUND, "P050", "설정을 찾을 수 없습니다."),

    // 공통
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "P092", "이미 처리된 요청입니다."),
    MISSING_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "P091", "X-Idempotency-Key 헤더가 필요합니다."),
    OPTIMISTIC_LOCK_CONFLICT(HttpStatus.CONFLICT, "P090", "동시 요청 충돌이 발생했습니다. 다시 시도해주세요."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "P000", "입력값 검증 실패"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "P099", "내부 서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
