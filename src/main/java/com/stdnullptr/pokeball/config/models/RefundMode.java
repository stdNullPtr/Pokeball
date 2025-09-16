package com.stdnullptr.pokeball.config.models;

/**
 * Enumeration of refund modes for pokeball returns
 */
public enum RefundMode {
    /**
     * Give pokeball back to player inventory
     */
    GIVE,

    /**
     * Drop pokeball on the ground at impact location
     */
    DROP
}