package com.silverline.erp.common.constants;

public class PosConstants {

    public static final String SHIFT_STATUS_OPEN = "OPEN";
    public static final String SHIFT_STATUS_CLOSED = "CLOSED";

    // Payment Types
    public static final String PAYMENT_CASH = "CASH";
    public static final String PAYMENT_CARD = "CARD";
    public static final String PAYMENT_QR = "QR";
    public static final String PAYMENT_BANK_TRANSFER = "BANK_TRANSFER";

    // Payment Status
    public static final String PAYMENT_STATUS_PAID = "PAID";
    public static final String PAYMENT_STATUS_PARTIAL = "PARTIAL";
    public static final String PAYMENT_STATUS_PENDING = "PENDING";

    // Sale Type
    public static final String SALE_TYPE_RETAIL = "RETAIL";
    public static final String SALE_TYPE_WHOLESALE = "WHOLESALE";

    // Serial Status
    public static final String SERIAL_STATUS_IN_STOCK = "IN_STOCK";
    public static final String SERIAL_STATUS_SOLD = "SOLD";
    public static final String SERIAL_STATUS_DAMAGED = "DAMAGED";
    public static final String SERIAL_STATUS_RETURNED = "RETURNED";

    // Return Status
    public static final String RETURN_STATUS_PENDING = "PENDING";
    public static final String RETURN_STATUS_APPROVED = "APPROVED";
    public static final String RETURN_STATUS_REJECTED = "REJECTED";

    // Refund Methods
    public static final String REFUND_CASH = "CASH";
    public static final String REFUND_CARD = "CARD";
    public static final String REFUND_CREDIT_NOTE = "CREDIT_NOTE";

    // Cash difference tolerance (in currency units)
    public static final double CASH_TOLERANCE = 10.0;

    // Tax rates (can be moved to database later)
    public static final double DEFAULT_TAX_RATE = 0.0;
}

