package com.silverline.erp.module.inventory.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class InventoryUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate a unique code with prefix, branch, date, and sequence
     */
    public static String generateCode(String prefix, Long branchId, Long sequence) {
        String dateStr = LocalDate.now().format(DATE_FORMAT);
        return String.format("%s-%d-%s-%03d", prefix, branchId, dateStr, sequence);
    }

    /**
     * Validate Dispatch status for updates
     */
    public static boolean canModifyDispatch(String status) {
        return "PENDING".equals(status);
    }

    /**
     * Validate Dispatch status for approval
     */
    public static boolean canApproveDispatch(String status) {
        return "PENDING".equals(status);
    }

    /**
     * Validate payment status update
     */
    public static boolean canUpdatePaymentStatus(String dispatchStatus) {
        return "APPROVED".equals(dispatchStatus);
    }

    /**
     * Check if payment status is valid
     */
    public static boolean isValidPaymentStatus(String paymentStatus) {
        return paymentStatus != null &&
                (paymentStatus.equals("PAID") ||
                        paymentStatus.equals("UNPAID") ||
                        paymentStatus.equals("PARTIAL"));
    }
}
