package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleActiveRequest(
        @NotNull Boolean active
) {}
