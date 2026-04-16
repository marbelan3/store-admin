package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToWatchlistRequest(
        @NotNull UUID connectionId,
        @NotBlank String externalProductId
) {}
