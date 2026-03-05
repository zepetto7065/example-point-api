package com.musinsa.point.exception;

import org.springframework.http.HttpStatus;

/**
 * 커스텀 에러코드 인터페이스.
 * 도메인별로 디테일한 ErrorCode가 필요하면 이 인터페이스를 구현한다.
 */
public interface ErrorCode {

    HttpStatus getHttpStatus();

    String getCode();

    String getMessage();
}
