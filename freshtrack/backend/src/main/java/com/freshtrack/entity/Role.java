package com.freshtrack.entity;

/**
 * System roles for Role-Based Access Control (RBAC).
 *
 * <ul>
 *     <li>{@code CENTRAL_ADMIN} – Global / cross-warehouse scope. Manages users,
 *         warehouse mappings, master invoice uploads and global reporting.</li>
 *     <li>{@code HUB_USER} – Restricted to assigned warehouse(s). Executes the
 *         scan-to-receive workflow.</li>
 * </ul>
 */
public enum Role {
    CENTRAL_ADMIN,
    HUB_USER
}
