package com.g2u.admin.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateMediaRequest(
        @Size(max = 500) String altText
) {}
