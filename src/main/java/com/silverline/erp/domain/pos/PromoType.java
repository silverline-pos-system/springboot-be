package com.silverline.erp.domain.pos;

/** Promotion strategies supported by the offers engine. */
public enum PromoType {
    /** Buy N of an item, get M (of the same or another product) free. */
    BUY_X_GET_Y_FREE,
    /** Percentage off the qualifying lines. */
    PERCENT_OFF,
    /** Fixed amount off the qualifying lines. */
    AMOUNT_OFF,
    /** N units for a fixed total price (e.g. 3 for 500). */
    N_FOR_FIXED,
    /** Bundle of listed products for a fixed total price. */
    BUNDLE,
    /** Auto discount for batches within N days of expiry (stock clearance). */
    EXPIRY_CLEARANCE
}
