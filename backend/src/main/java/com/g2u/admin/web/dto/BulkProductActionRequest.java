package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record BulkProductActionRequest(
        @NotNull Action action,
        @NotEmpty Set<UUID> productIds,
        String status // for CHANGE_STATUS action
) {
    public enum Action {
        DELETE,
        CHANGE_STATUS
    }
}
