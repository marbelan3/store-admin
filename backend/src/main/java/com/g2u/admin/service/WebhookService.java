package com.g2u.admin.service;

import com.g2u.admin.domain.webhook.WebhookDelivery;
import com.g2u.admin.domain.webhook.WebhookDeliveryRepository;
import com.g2u.admin.domain.webhook.WebhookEndpoint;
import com.g2u.admin.domain.webhook.WebhookEndpointRepository;
import com.g2u.admin.web.dto.CreateWebhookRequest;
import com.g2u.admin.web.dto.UpdateWebhookRequest;
import com.g2u.admin.web.dto.WebhookDeliveryDto;
import com.g2u.admin.web.dto.WebhookEndpointDto;
import com.g2u.admin.web.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WebhookService {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookService.class);
    private static final int SECRET_LENGTH = 32;
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final WebhookDeliveryRepository webhookDeliveryRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService retryExecutor;

    public WebhookService(WebhookEndpointRepository webhookEndpointRepository,
                          WebhookDeliveryRepository webhookDeliveryRepository,
                          ObjectMapper objectMapper) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.webhookDeliveryRepository = webhookDeliveryRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "webhook-retry");
            t.setDaemon(true);
            return t;
        });
    }

    // --- CRUD operations ---

    @Transactional(readOnly = true)
    public Page<WebhookEndpointDto> getWebhooks(UUID tenantId, Pageable pageable) {
        return webhookEndpointRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebhookEndpointDto getWebhook(UUID tenantId, UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint", id));
        return toDto(endpoint);
    }

    @Transactional
    public WebhookEndpointDto createWebhook(UUID tenantId, CreateWebhookRequest request) {
        String secret = generateSecret();
        String events = String.join(",", request.events());

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .url(request.url())
                .events(events)
                .secret(secret)
                .active(true)
                .build();
        endpoint.setTenantId(tenantId);

        endpoint = webhookEndpointRepository.save(endpoint);
        LOG.debug("Created webhook endpoint: {} for tenant {}", endpoint.getId(), tenantId);
        return toDto(endpoint);
    }

    @Transactional
    public WebhookEndpointDto updateWebhook(UUID tenantId, UUID id, UpdateWebhookRequest request) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint", id));

        if (request.url() != null) endpoint.setUrl(request.url());
        if (request.events() != null) endpoint.setEvents(String.join(",", request.events()));
        if (request.active() != null) endpoint.setActive(request.active());

        endpoint = webhookEndpointRepository.save(endpoint);
        LOG.debug("Updated webhook endpoint: {}", endpoint.getId());
        return toDto(endpoint);
    }

    @Transactional
    public void deleteWebhook(UUID tenantId, UUID id) {
        WebhookEndpoint endpoint = webhookEndpointRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint", id));
        webhookEndpointRepository.delete(endpoint);
        LOG.debug("Deleted webhook endpoint: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<WebhookDeliveryDto> getDeliveries(UUID tenantId, UUID webhookId, Pageable pageable) {
        // Verify webhook belongs to tenant
        webhookEndpointRepository.findByIdAndTenantId(webhookId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook endpoint", webhookId));

        return webhookDeliveryRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId, pageable)
                .map(this::toDeliveryDto);
    }

    // --- Dispatch ---

    @Async
    public void dispatch(UUID tenantId, String event, Map<String, Object> payload) {
        LOG.debug("Dispatching webhook event: {} for tenant {}", event, tenantId);

        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findByTenantIdAndActiveTrue(tenantId);

        for (WebhookEndpoint endpoint : endpoints) {
            List<String> subscribedEvents = Arrays.asList(endpoint.getEvents().split(","));
            if (subscribedEvents.contains(event)) {
                deliverWebhook(endpoint, event, payload, 1);
            }
        }
    }

    private void deliverWebhook(WebhookEndpoint endpoint, String event,
                                Map<String, Object> payload, int attempt) {
        try {
            String body = objectMapper.writeValueAsString(payload);
            String signature = computeHmacSignature(body, endpoint.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Webhook-Signature", "sha256=" + signature);
            headers.set("X-Webhook-Event", event);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint.getUrl(), HttpMethod.POST, request, String.class);

            recordDelivery(endpoint.getId(), event, payload,
                    response.getStatusCode().value(), true, attempt);
            LOG.debug("Webhook delivered successfully to {} (attempt {})", endpoint.getUrl(), attempt);

        } catch (Exception e) {
            LOG.warn("Webhook delivery failed to {} (attempt {}): {}", endpoint.getUrl(), attempt, e.getMessage());

            recordDelivery(endpoint.getId(), event, payload, null, false, attempt);

            // Retry once after 5 seconds if this was the first attempt
            if (attempt == 1) {
                retryExecutor.schedule(
                        () -> deliverWebhook(endpoint, event, payload, 2),
                        5, TimeUnit.SECONDS);
            }
        }
    }

    private void recordDelivery(UUID webhookId, String event, Map<String, Object> payload,
                                Integer responseStatus, boolean success, int attempt) {
        try {
            WebhookDelivery delivery = WebhookDelivery.builder()
                    .webhookId(webhookId)
                    .event(event)
                    .payload(payload)
                    .responseStatus(responseStatus)
                    .success(success)
                    .attempt(attempt)
                    .build();
            webhookDeliveryRepository.save(delivery);
        } catch (Exception e) {
            LOG.error("Failed to record webhook delivery: {}", e.getMessage());
        }
    }

    private String computeHmacSignature(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC signature", e);
        }
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_LENGTH];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private WebhookEndpointDto toDto(WebhookEndpoint e) {
        return new WebhookEndpointDto(
                e.getId(),
                e.getTenantId(),
                e.getUrl(),
                Arrays.asList(e.getEvents().split(",")),
                e.getSecret(),
                e.isActive(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private WebhookDeliveryDto toDeliveryDto(WebhookDelivery d) {
        return new WebhookDeliveryDto(
                d.getId(),
                d.getWebhookId(),
                d.getEvent(),
                d.getPayload(),
                d.getResponseStatus(),
                d.isSuccess(),
                d.getAttempt(),
                d.getCreatedAt()
        );
    }
}
