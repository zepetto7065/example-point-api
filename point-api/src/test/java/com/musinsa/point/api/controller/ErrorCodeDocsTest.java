package com.musinsa.point.api.controller;

import com.musinsa.point.exception.PointErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

class ErrorCodeDocsTest {

    @Test
    @DisplayName("오류 코드 문서 생성")
    void generateErrorCodeDocs() throws IOException {
        Map<String, List<PointErrorCode>> grouped = new HashMap<>();
        grouped.put("적립", new ArrayList<>());
        grouped.put("적립취소", new ArrayList<>());
        grouped.put("사용", new ArrayList<>());
        grouped.put("사용취소", new ArrayList<>());
        grouped.put("지갑", new ArrayList<>());
        grouped.put("설정", new ArrayList<>());
        grouped.put("공통", new ArrayList<>());

        for (PointErrorCode code : PointErrorCode.values()) {
            grouped.get(resolveCategory(code.getCode())).add(code);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<PointErrorCode>> entry : grouped.entrySet()) {
            if (entry.getValue().isEmpty()) continue;

            sb.append("=== ").append(entry.getKey()).append("\n\n");
            sb.append("|===\n");
            sb.append("|코드 |HTTP 상태 |설명\n\n");
            for (PointErrorCode code : entry.getValue()) {
                sb.append(String.format("|`%s` |%d %s |%s\n",
                        code.getCode(),
                        code.getHttpStatus().value(),
                        code.getHttpStatus().getReasonPhrase(),
                        code.getMessage()));
            }
            sb.append("|===\n\n");
        }

        Path dir = Paths.get("build/generated-snippets/error-codes");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("error-codes.adoc"), sb.toString());
    }

    private String resolveCategory(String code) {
        return switch (code.substring(0, 3)) {
            case "P00" -> code.equals("P000") ? "공통" : "적립";
            case "P01" -> "적립취소";
            case "P02" -> "사용";
            case "P03" -> "사용취소";
            case "P04" -> "지갑";
            case "P05" -> "설정";
            default -> "공통";
        };
    }
}
