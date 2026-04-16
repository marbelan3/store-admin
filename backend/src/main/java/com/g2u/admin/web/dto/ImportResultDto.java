package com.g2u.admin.web.dto;

import java.util.List;

public record ImportResultDto(
        int created,
        int updated,
        List<ImportError> errors
) {
    public record ImportError(int row, String message) {
    }
}
