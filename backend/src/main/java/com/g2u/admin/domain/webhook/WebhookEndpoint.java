package com.g2u.admin.domain.webhook;

import com.g2u.admin.domain.common.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "webhook_endpoints")
@Getter
@Setter
@NoArgsConstructor
public class WebhookEndpoint extends TenantAwareEntity {

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "events", nullable = false, length = 500)
    private String events;

    @Column(name = "secret", nullable = false, length = 100)
    private String secret;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder
    public WebhookEndpoint(String url, String events, String secret, boolean active) {
        this.url = url;
        this.events = events;
        this.secret = secret;
        this.active = active;
    }
}
