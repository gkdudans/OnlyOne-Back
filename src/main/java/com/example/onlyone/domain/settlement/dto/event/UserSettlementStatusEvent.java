package com.example.onlyone.domain.settlement.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSettlementStatusEvent {
    public enum ResultType { SUCCESS, FAILED }

    private ResultType type;              // "SUCCESS" | "FAILED"
    private String operationId;                  // "stl:4:usr:100234:v1"
    private Instant occurredAt;

    private long settlementId;
    private long userSettlementId;
    private long participantId;

    private long memberWalletId;
    private long leaderId;
    private long leaderWalletId;
    private long amount;
    private long memberBalance; // 처리 시점 참가자 지갑 잔액 스냅샷
}
