package com.example.vaadin.security;

/**
 * Application role hierarchy.
 * Ordered from least to most privileged.
 */
public enum Role {
    GUEST("Guest"),
    USER("User"),
    MANAGER("Manager"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Checks if this role has the privilege level of the required role.
     * Uses enum ordinal for hierarchical comparison.
     *
     * @param requiredRole the role to check against
     * @return true if this role has equal or higher privilege
     */
    public boolean hasPrivilege(Role requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }
}
