package com.g2u.admin.web.dto;

import java.util.List;

public record UpdateWebhookRequest(
        String url,
        List<String> events,
        Boolean active
) {
}
