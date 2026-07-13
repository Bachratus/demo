package com.bachratus.demo.infra.db.outbox;

public enum OutboxStatus {
    PENDING,
    FAILED,
    DLT_PENDING,
    PUBLISHED,
    DEAD_LETTERED
}
