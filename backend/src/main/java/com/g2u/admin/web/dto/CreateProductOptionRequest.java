package com.g2u.admin.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProductOptionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotEmpty List<@NotBlank @Size(max = 100) String> values,
        Integer displayOrder
) {}
