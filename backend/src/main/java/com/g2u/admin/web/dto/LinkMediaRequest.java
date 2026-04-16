package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LinkMediaRequest(
        @NotNull UUID mediaId
) {}
