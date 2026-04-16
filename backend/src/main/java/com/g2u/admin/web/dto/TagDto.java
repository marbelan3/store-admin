package com.g2u.admin.web.dto;

import java.util.UUID;

public record TagDto(UUID id, String name, String slug) {
}
