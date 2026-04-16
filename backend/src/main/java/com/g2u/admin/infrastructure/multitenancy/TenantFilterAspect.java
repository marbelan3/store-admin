package com.g2u.admin.infrastructure.multitenancy;

/**
 * Previously this aspect enabled a Hibernate @Filter on every repository find/count/exists call.
 * This caused duplicate tenant_id conditions on queries that already include tenantId as a parameter,
 * leading to "Parameter index out of range" errors.
 *
 * All repository methods already explicitly filter by tenantId, making this aspect unnecessary.
 * The @FilterDef/@Filter annotations on TenantAwareEntity are kept for documentation but are no
 * longer activated at runtime.
 */
public class TenantFilterAspect {
    // Intentionally left empty — tenant filtering is handled by explicit query parameters
}
