package com.saga.audit.constants;

/**
 * Constants for unit tests.
 */
public final class TestConstants {

    private TestConstants() {}

    public static final String TRANSFER_ID_1 = "T-001";

    /** Payload/event keys for tests */
    public static final String PAYLOAD_KEY_TRANSFER_ID = "transferId";
    public static final String PAYLOAD_KEY_AMOUNT = "amount";
    public static final String PAYLOAD_KEY_RECORDED = "recorded";

    public static final String AMOUNT_100 = "100";
    public static final String ACTION_TRANSFER = "TRANSFER";

    /** Expected values for topic constants (AuditConstants) */
    public static final String EXPECTED_TOPIC_AUDIT_RECORD = "audit.record";
    public static final String EXPECTED_TOPIC_AUDIT_RECORDED = "audit.recorded";
}
