import { api } from './client';
import type { Page } from '$lib/types/product';

export interface AuditLogEntry {
	id: string;
	userId: string | null;
	userName: string | null;
	action: string;
	entityType: string;
	entityId: string;
	changes: Record<string, unknown> | null;
	createdAt: string;
}

export interface AuditLogFilters {
	entityType?: string;
	entityId?: string;
	action?: string;
	from?: string;
	to?: string;
	page?: number;
	size?: number;
}

export function getAuditLogs(filters: AuditLogFilters = {}) {
	const params: Record<string, string> = {};
	if (filters.entityType) params.entityType = filters.entityType;
	if (filters.entityId) params.entityId = filters.entityId;
	if (filters.action) params.action = filters.action;
	if (filters.from) params.from = filters.from;
	if (filters.to) params.to = filters.to;
	if (filters.page !== undefined) params.page = String(filters.page);
	if (filters.size !== undefined) params.size = String(filters.size);
	return api.get<Page<AuditLogEntry>>('/api/audit-logs', params);
}
