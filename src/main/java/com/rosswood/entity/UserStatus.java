package com.rosswood.entity;

public enum UserStatus {
    /** Fully registered and active */
    REGISTERED,
    /** Account created but not yet confirmed (if email confirmation is needed) */
    UNCONFIRMED,
    /** Disabled by an administrator */
    DISABLED
}
